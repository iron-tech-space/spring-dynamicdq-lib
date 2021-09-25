package com.irontechspace.dynamicdq.executor.query;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.irontechspace.dynamicdq.configurator.query.QueryConfigService;
import com.irontechspace.dynamicdq.configurator.query.model.QueryField;
import com.irontechspace.dynamicdq.configurator.query.model.QueryConfig;
import com.irontechspace.dynamicdq.exceptions.NotFoundException;
import com.irontechspace.dynamicdq.executor.query.model.JoinTable;
import com.irontechspace.dynamicdq.executor.query.model.TypeQuery;
import com.irontechspace.dynamicdq.executor.query.QueryRepository;
import com.irontechspace.dynamicdq.executor.query.QueryRowMapper;
import com.irontechspace.dynamicdq.utils.SqlUtils;
import com.irontechspace.dynamicdq.utils.TypeConverter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;

@Log4j2
@Service
public class QueryService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String SQL_SELECT = "\tselect %s ";
    private static final String SQL_FROM_TABLE = "\n\tfrom %s a \n";
    private static final String SQL_FROM_SQL = "\n\tfrom (%s) a \n";
    private static final String SQL_JOIN = "\t\tleft join %s %s on %s.%s = %s.%s \n";
    private static final String SQL_WHERE_1_1 = "\twhere 1=1 \n";
    private static final String SQL_GROUP_BY = "\tgroup by %s \n";
    private static final String SQL_ORDER_BY = "\torder by %s \n";
    private static final String SQL_PAGEABLE = "\tlimit :pageSize\n\toffset :offset \n";
    private static final String SQL_ROW_NUMBER = " row_number() over() + :offset as row_number, ";

    @Autowired
    QueryConfigService queryConfigService;
    @Autowired
    QueryRepository queryRepository;
    @Autowired
    TypeConverter typeConverter;


    /** Получение SQL */
    public ObjectNode getSql(String configName, UUID userId, List<String> userRoles, JsonNode filter, Pageable pageable){
        return getSql(null, configName, userId, userRoles, filter, pageable);
    }
    /** Получение SQL */
    public ObjectNode getSql(DataSource dateSource, String configName, UUID userId, List<String> userRoles, JsonNode filter, Pageable pageable){
        return getQueryData(dateSource, getConfigByName(dateSource, configName, userId, userRoles), userId, userRoles, filter, pageable, TypeQuery.SQL);
    }

    /** Получить количественный SQL */
    public ObjectNode getSqlCount(String configName, UUID userId, List<String> userRoles, JsonNode filter, Pageable pageable){
        return getSqlCount(null, configName, userId, userRoles, filter, pageable);
    }
    /** Получить количественный SQL */
    public ObjectNode getSqlCount(DataSource dateSource, String configName, UUID userId, List<String> userRoles, JsonNode filter, Pageable pageable){
        return getQueryData(dateSource, getConfigByName(dateSource, configName, userId, userRoles), userId, userRoles, filter, pageable, TypeQuery.SQL_COUNT);
    }

    /** Получение объект данных */
    public ObjectNode getObject(String configName, UUID userId, List<String> userRoles, JsonNode filter, Pageable pageable){
        return getObject(null, configName, userId, userRoles, filter, pageable);
    }
    /** Получение объект данных */
    public ObjectNode getObject(DataSource dateSource, String configName, UUID userId, List<String> userRoles, JsonNode filter, Pageable pageable){
        return getQueryData(dateSource, getConfigByName(dateSource, configName, userId, userRoles), userId, userRoles, filter, pageable, TypeQuery.OBJECT);
    }

    /** Получение плоской таблицы данных */
    public List<ObjectNode> getFlatData(String configName, UUID userId, List<String> userRoles, JsonNode filter, Pageable pageable){
        return getFlatData(null, configName, userId, userRoles, filter, pageable);
    }
    /** Получение плоской таблицы данных */
    public List<ObjectNode> getFlatData(DataSource dateSource, String configName, UUID userId, List<String> userRoles, JsonNode filter, Pageable pageable){
        return getQueryData(dateSource, getConfigByName(dateSource, configName, userId, userRoles), userId, userRoles, filter, pageable);
    }

    /** Получение кол-ва данных в плоской таблице */
    public Long getFlatDataCount(String configName, UUID userId, List<String> userRoles, JsonNode filter, Pageable pageable){
        return getFlatDataCount(null, configName, userId, userRoles, filter, pageable);
    }
    /** Получение кол-ва данных в плоской таблице */
    public Long getFlatDataCount(DataSource dateSource, String configName, UUID userId, List<String> userRoles, JsonNode filter, Pageable pageable){
        QueryConfig queryConfig = getConfigByName(dateSource, configName, userId, userRoles);
        return getQueryData(dateSource, queryConfig, userId, userRoles, filter, pageable, TypeQuery.COUNT);
    }

    /** Получение иерархической таблицы данных */
    public List<ObjectNode> getHierarchicalData(String configName, UUID userId, List<String> userRoles, JsonNode filter, Pageable pageable){
        return getHierarchicalData(null, configName, userId, userRoles, filter, pageable);
    }
    /** Получение иерархической таблицы данных */
    public List<ObjectNode> getHierarchicalData(DataSource dateSource, String configName, UUID userId, List<String> userRoles, JsonNode filter, Pageable pageable){
        QueryConfig queryConfig = getConfigByName(dateSource, configName, userId, userRoles);
        List<ObjectNode> result = getHierarchicalData(getQueryData(dateSource, queryConfig, userId, userRoles, filter, pageable), queryConfig.getHierarchyField());

        // Формирование мапы параметров поиска
        Map<String, Pattern> searchFields = new HashMap<>();
        for(QueryField queryField : queryConfig.getFields()){
            if(queryField.getFilterFields() != null && queryField.getFilterSigns() != null){
                String[] filterFields = queryField.getFilterFields().split("/");
                String[] filterSigns = queryField.getFilterSigns().split("/");
                if(filterFields.length == filterSigns.length){
                    for (int i = 0; i < filterFields.length; i++) {
                        if (filter.has(filterFields[i])
                                && (filterSigns[i].toLowerCase().equals("ilike") || filterSigns[i].toLowerCase().equals("like"))) {
                            if(queryConfig.getLoggingQueries())
                                log.info("[{}] Add search field [{}]: [{}]", configName, queryField.getAliasOrName(), filter.get(filterFields[i]).asText().replaceAll("\\s", ".+?"));
                            searchFields.put(
                                    queryField.getAliasOrName(),
                                    Pattern.compile(filter.get(filterFields[i]).asText().replaceAll("\\s", ".+?")));
                        }
                    }
                }
            }
        }
        // Если мапа параметров поиска не пустая, то ищем
        if(searchFields.size() > 0) {
            if(queryConfig.getLoggingQueries())
                log.info("[{}] Search by tree", configName);
            for (int i = result.size() - 1; i >= 0; i--) {
                if (removeRecursively(result.get(i), searchFields)) {
                    result.remove(i);
                }
            }
        } else if(queryConfig.getLoggingQueries())
            log.info("[{}] Skip search by tree", configName);
        return result;
    }

    /** Перегрузка метода получения данных */
    public List<ObjectNode> getQueryData(DataSource dateSource, QueryConfig queryConfig, UUID userId, List<String> userRoles, JsonNode filter, Pageable pageable){
        return getQueryData(dateSource, queryConfig, userId, userRoles, filter, pageable, TypeQuery.TABLE);
    }

    /** ОСНОВНОЙ метода получения данных */
    private <T> T getQueryData(DataSource dateSource, QueryConfig queryConfig, UUID userId, List<String> userRoles, JsonNode filter, Pageable pageable, TypeQuery typeQuery) {
        List<QueryField> queryFields = queryConfig.getFields();
        List<String> sourceFields = new ArrayList<>();
        List<JoinTable> joinTables = new ArrayList<>();
        List<String> filterOutside = new ArrayList<>();
        List<String> groupBy = new ArrayList<>();
        List<String> having = new ArrayList<>();
        SortedMap<Integer, String> orderByInside = new TreeMap<>();

        // Инициализация параметров запроса
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("pageSize", pageable.getPageSize());
        params.addValue("offset", pageable.getOffset());
        params.addValue("userId", userId);
        params.addValue("userRoles", userRoles);
        // ARRAY(SELECT json_array_elements_text(({0})::json)) && array[ :userRoles ]::text[]
        // ({0})::jsonb @> '[ :userId ]'

        char prevJoinCode;
        char nextJoinCode = 'a';
        char fieldJoinCode = 'b';
        List<String> aggregateTypes = Arrays.asList("linkBack", "customAggregate");
//        Boolean linkBackExist = fields.stream().anyMatch(o -> o.getType().equals("linkBack") || o.getType().equals("aggregate"));
        Boolean linkBackExist = queryFields.stream().anyMatch(o -> aggregateTypes.contains(o.getTypeField()));

        for(QueryField queryField : queryFields){

            // Если поле не row_number
            if(!queryField.getName().equals("row_number")) {

                // Если поле содержит ссылку - создаем каскад join-ов
                if (queryField.getLinkPath() != null) {

                    // public.highway.id.highway_id/public.generation_object.id.generation_object_id
                    // public.generation_object.id.generation_object_id.full_name
                    // public.highway.id.highway_id.full_name
                    // public.generation_object.id.parent_generation_object_id.short_name

                    /**
                     * Примеры link_path для таблицы assd.metering_reports
                     *
                     * assd.metering_bundles.id.id_bundle/assd.metering_points.id.id_point
                     * assd.report_statuses.code.code_status
                     * assd.metering_bundles.id.id_bundle/assd.metering_points.id.id_point/assd.metering_point_types.code.code_type
                     */

                    String[] joinObjs = queryField.getLinkPath().split("/"); // joinLine.split("/");

                    String parentJoinParams = "";

                    for(int i = 0; i < joinObjs.length; i++){

                        String[] joinObjsParams = joinObjs[i].split("\\.");

                        if(i == 0)
                            parentJoinParams = String.format("%s", queryConfig.getTableName());
                        else
                            parentJoinParams = joinObjs[i-1];

                        // Строка сравнения JOIN-ов
                        // assd.metering_reports-assd.metering_bundles.id.id_bundle
                        String compare = String.format("%s-%s", parentJoinParams, joinObjs[i]);

                        // Поиск JOIN-а среди уже существующих
                        Optional<JoinTable> joinTable = joinTables.stream().filter(o -> o.getCompare().equals(compare)).findFirst();

                        // JOIN найден - берем его код и используем для указания поля источника
                        if(joinTable.isPresent()) {
                            // Заполнение кода для поле этого JOIN
                            fieldJoinCode = joinTable.get().getCode();
                        }
                        // JOIN не найден - создаем
                        else {
                            // Инкрементируем код JOIN-а
                            nextJoinCode++;

                            // Заполняем код предыдущей таблицы
                            if(i == 0) prevJoinCode = 'a';
                            else prevJoinCode = fieldJoinCode; // (char) (nextJoinCode - 1);

                            // Заполняем код для поля этого JOIN
                            fieldJoinCode = nextJoinCode;

                            // Сам JOIN
                            String localJoin = String.format(SQL_JOIN,
                                    String.format("%s.%s", joinObjsParams[0], joinObjsParams[1]), // Таблица для JOIN
                                    nextJoinCode,               // Синоном таблицы JOIN
                                    nextJoinCode,               // Префикс к полю новой таблицы
                                    joinObjsParams[2],          // Поле новой таблицы
                                    prevJoinCode,               // Превикс к полю предыдущей таблицы
                                    joinObjsParams[3]);         // Поле предыдущей таблицы

                            // Добавить JOIN в хранилище
                            joinTables.add(new JoinTable(nextJoinCode, compare, localJoin));
                        }

                        // Поле для вывода (поле источника данных)
                        if(i == joinObjs.length - 1) {
                            // Тут был блок заполнения sourceFields после создания join
                        }
                    }
                }

                // Добавление поля в список полей в соответствии с типом поля
                // column, link, linkBack, custom, customAggregate
                switch (queryField.getTypeField()){
                    case "column":
                        sourceFields.add(String.format("\n a.%s as %s", queryField.getName(), queryField.getAliasOrName() ));
                        break;
                    case "link":
                        sourceFields.add(String.format("\n %s.%s as %s", fieldJoinCode, queryField.getLinkView(), queryField.getAliasOrName() ));
                        break;
                    case "linkBack":
                        String[] linkViewFields = queryField.getLinkView().split("/");
                        List<String> jsonBuildParams = new ArrayList<>();
                        for (String linkViewField : linkViewFields) {
                            String[] viewFields = linkViewField.split(":");
                            if(viewFields.length == 2)
                                jsonBuildParams.add(String.format("'%s', %s.%s", viewFields[0], fieldJoinCode, viewFields[1].replaceAll("([^_A-Z])([A-Z])", "$1_$2").toLowerCase()));
                            else
                                jsonBuildParams.add(String.format("'%s', %s.%s", linkViewField, fieldJoinCode, linkViewField.replaceAll("([^_A-Z])([A-Z])", "$1_$2").toLowerCase()));
                        }
                        sourceFields.add(String.format("\n json_agg(json_build_object(%s)) as %s", String.join(", ", jsonBuildParams), queryField.getAliasOrName()));
                        break;

                    case "custom":
                    case "customAggregate":
                        sourceFields.add(String.format("\n %s as %s", getCustomFieldName(queryField, fieldJoinCode), queryField.getAliasOrName()));
                        break;
                }

                if(!aggregateTypes.contains(queryField.getTypeField()))
                    if(linkBackExist)
                        if(queryField.getLinkPath() != null)
                            groupBy.add(String.format("\n %s.%s", fieldJoinCode, queryField.getLinkView()));
                        else
                            groupBy.add(String.format("\n a.%s", queryField.getName()));

                // and {0} = 13
                // and {0} = 5
                // and {1} ilike '%54%'

                // Фильтрация по полю (системная = дефолтная)
                // {0} = 5 transform to a.[name] = 5
                // {1} = 5 transform to [JoinCode].[LinkView] = 5
                if(queryField.getFilterInside() != null){
                    String whereInside = queryField.getFilterInside();
                    whereInside = whereInside.replaceAll("\\{0\\}", String.format("a.%s", queryField.getName()));
                    whereInside = whereInside.replaceAll("\\{1\\}", String.format("%s.%s", fieldJoinCode, queryField.getLinkView()));
                    if(whereInside.startsWith("and"))
                        filterOutside.add(String.format(" %s \n", whereInside));
                    else
                        filterOutside.add(String.format(" and %s \n", whereInside));
                }


                // Фильтрация по полю (пользовательская)
                if(queryField.getFilterFields() != null && queryField.getFilterSigns() != null){
                    String[] filterFields = queryField.getFilterFields().split("/");
                    String[] filterSigns = queryField.getFilterSigns().split("/");

                    // Если кол-ва параметров фильтрации равны, то фильтруем
                    if(filterFields.length == filterSigns.length){

                        for (int i = 0; i < filterFields.length; i++){

                            if(queryConfig.getHierarchical() != null && queryConfig.getHierarchical()
                                && (filterSigns[i].toLowerCase().equals("ilike") || filterSigns[i].toLowerCase().equals("like"))){
                                if(queryConfig.getLoggingQueries())
                                    log.info("[{}] Skip like filter", queryConfig.getConfigName());
                                continue;
                            }

                            if(filter.has(filterFields[i])) {
                                boolean paramOnly = filterSigns[i].equals("paramOnly");
                                String whereFieldName;
                                String whereParamName;

                                // Опеределяем имя поля и имя параметра
                                if (queryField.getLinkPath() != null) {
                                    whereFieldName = String.format("%s.%s", fieldJoinCode, queryField.getLinkView());
                                    whereParamName = String.format("%s.%s.%s", fieldJoinCode, queryField.getLinkView(), filterFields[i]);
                                } else {
                                    whereFieldName = String.format("a.%s", queryField.getName());
                                    whereParamName = String.format("a.%s.%s", queryField.getAliasOrName(), filterFields[i]);
                                }

                                // Меняем имя поля если тип "aggregate" или "math"
                                if(queryField.getTypeField().equals("custom") || queryField.getTypeField().equals("customAggregate")) {
                                    whereFieldName = getCustomFieldName(queryField, fieldJoinCode);
                                }

                                // Если только параметр, то задать имя параметра от пользователя
                                if(paramOnly)
                                    whereParamName = String.format("%s", filterFields[i]);

                                /** Если массив */
                                if(filter.get(filterFields[i]).isArray()) {
                                    ArrayNode jsonNodes = (ArrayNode) filter.get(filterFields[i]);

                                    // Оперделяем тип и собираем массив
                                    List<Object> values = typeConverter.getListObjectsByType(queryField.getTypeData(), jsonNodes);

                                    params.addValue(whereParamName, values);

                                    // Делаем строку запроса, если массив не пустой
                                    if(values.size() > 0 && !paramOnly)
                                        setWhereOrHaving(queryField, filterOutside, having, String.format(" and %s %s (:%s) \n", whereFieldName, filterSigns[i].toLowerCase(), whereParamName));
//                                        filterOutside.add(String.format(" and %s %s (:%s) \n", whereFieldName, filterSigns[i].toLowerCase(), whereParamName));

                                } else {
                                    // Если значение null
                                    if(filter.get(filterFields[i]).isNull()){
                                        if(!paramOnly)
                                            if(filterSigns[i].toLowerCase().equals("="))
                                                setWhereOrHaving(queryField, filterOutside, having, String.format(" and %s is null \n", whereFieldName));
//                                                filterOutside.add(String.format(" and %s is null \n", whereFieldName));
                                            else
                                                setWhereOrHaving(queryField, filterOutside, having, String.format(" and %s is not null \n", whereFieldName));
//                                                filterOutside.add(String.format(" and %s is not null \n", whereFieldName));
                                    } else {
                                        // Делаем строку запроса
                                        if(!paramOnly)
                                            setWhereOrHaving(queryField, filterOutside, having, String.format(" and %s %s :%s \n", whereFieldName, filterSigns[i].toLowerCase(), whereParamName));
//                                            filterOutside.add(String.format(" and %s %s :%s \n", whereFieldName, filterSigns[i].toLowerCase(), whereParamName));

                                        // Оперделяем тип и получаем значение
                                        Object value = typeConverter.getObjectByType(queryField.getTypeData(), filterFields[i], filter);

                                        // Заносим значение в параметры
                                        if (filterSigns[i].toLowerCase().equals("ilike") || filterSigns[i].toLowerCase().equals("like"))
                                            params.addValue(whereParamName, SqlUtils.toSearchString((String)value));
                                        else
                                            params.addValue(whereParamName, value);
                                    }
                                }
                            }
                        }
                    }
                }

                // Сортировка из конфига по полю
                if(queryField.getOrderByInside() != null) {
                    String[] orderByIn = queryField.getOrderByInside().split("\\.");
                    if(queryField.getTypeField().equals("link") || queryField.getTypeField().equals("linkBack"))
                        orderByInside.put(Integer.valueOf(orderByIn[0]), String.format("%s.%s %s", fieldJoinCode, queryField.getLinkView(), orderByIn[1]));
                    else if(queryField.getTypeField().equals("custom") || queryField.getTypeField().equals("customAggregate"))
                        orderByInside.put(Integer.valueOf(orderByIn[0]), String.format("%s %s", getCustomFieldName(queryField, fieldJoinCode), orderByIn[1]));
                    else // column
                        orderByInside.put(Integer.valueOf(orderByIn[0]), String.format("a.%s %s", queryField.getName(), orderByIn[1]));
                }
            }
        }

        // Сортировка от клиента
        if(pageable.getSort().isSorted())
            orderByInside.put(100, pageable.getSort().stream().map(order -> String.format("%s %s", order.getProperty(), order.getDirection().name())).collect(joining(", ")));


        String FINAL_SQL;
        if(queryConfig.getCustomSql() != null){
            String sql = queryConfig.getCustomSql();

            // Добавили фильтры в SQL
            if (filterOutside.size() > 0)
                sql = sql.replaceAll(":WHERE_FIELDS", String.join("", filterOutside));
            else
                sql = sql.replaceAll(":WHERE_FIELDS", "");

            // Добавили сортировку в SQL
            if (orderByInside.size() > 0)
                sql = sql.replaceAll(":SORT_FIELDS", String.format(SQL_ORDER_BY, String.join(", ", orderByInside.values())));
            else
                sql = sql.replaceAll(":SORT_FIELDS", "");

            // Добавили пагинацию в SQL (если pageSize == 1, то ограничение на кол-во не накладывается)
            if (typeQuery != TypeQuery.COUNT && typeQuery != TypeQuery.SQL_COUNT && pageable.getPageSize() != 1)
                sql = sql.replaceAll( ":PAGEABLE", SQL_PAGEABLE);
            else
                sql = sql.replaceAll( ":PAGEABLE", "");

            if(queryConfig.getLoggingQueries())
                log.info("[{}]\n DATA: [{}]\n SQL: [{}]\n", queryConfig.getConfigName(), params.toString(), sql);

            FINAL_SQL = sql;

        } else {

            // Формирование SQL
            StringBuilder sql = new StringBuilder();
            sql.append("with t as ( \n");

            // Добавили поля для выборки
            sql.append(String.format(SQL_SELECT, String.join(", ", sourceFields)));

            // Добавили таблицу для выборки
            try {
                QueryConfig subConfig = getConfigByName(dateSource, queryConfig.getTableName(), userId, userRoles);
                ObjectNode subSql = getQueryData(dateSource, subConfig, userId, userRoles, filter, pageable, TypeQuery.SQL);
                sql.append(String.format(SQL_FROM_SQL, subSql.get("sql").asText()));
            } catch (NotFoundException e){
                sql.append(String.format(SQL_FROM_TABLE, queryConfig.getTableName()));
            }

            // Добавили JOIN-ы для выборки
            sql.append(joinTables.stream().map(JoinTable::getJoin).collect(Collectors.joining("")));

            // Добавили заглушку WHERE
            sql.append(SQL_WHERE_1_1);

            // Добавили фильтры в SQL
            if (filterOutside.size() > 0)
                sql.append(String.join("", filterOutside));

            // Добавили группировку в SQL
            if (groupBy.size() > 0)
                sql.append(String.format(SQL_GROUP_BY, String.join(", ", groupBy)));

            if(having.size() > 0)
                sql.append(String.format("having 1=1 %s", String.join(", ", having)));

            // Добавили сортировку в SQL
            if (orderByInside.size() > 0)
                sql.append(String.format(SQL_ORDER_BY, String.join(", ", orderByInside.values())));
            // sql.append(String.format(SQL_ORDER_BY, orderByInside.entrySet().stream().map(e -> e.getValue()).collect(Collectors.joining(", "))));

            // Добавили пагинацию в SQL (если pageSize == 1, то ограничение на кол-во не накладывается)
            if (typeQuery != TypeQuery.COUNT && typeQuery != TypeQuery.SQL_COUNT && pageable.getPageSize() != 1)
                sql.append(SQL_PAGEABLE);

            // Добавить основной селект
            if(typeQuery == TypeQuery.COUNT || typeQuery == TypeQuery.SQL_COUNT)
                sql.append(") \nselect count(t.*) from t \n");
            else
                sql.append(String.format(") \nselect %s t.* from t \n", getRowNumber(queryFields)));

            if(queryConfig.getLoggingQueries())
                log.info("[{}]\n DATA: [{}]\n PAGE SIZE: [{}], OFFSET: [{}]\n SQL: [{}]\n", queryConfig.getConfigName(), params.toString(), pageable.getPageSize(), pageable.getOffset(), sql.toString());

            FINAL_SQL = sql.toString();
        }

        switch (typeQuery){
            case SQL:
            case SQL_COUNT:
                ObjectNode objectNode = OBJECT_MAPPER.createObjectNode();
                objectNode.put("sql", FINAL_SQL);
                try {
                    objectNode.put("params", OBJECT_MAPPER.writeValueAsString(params.getValues()));
                } catch (JsonProcessingException ignored) {}
                return (T) objectNode;
            case COUNT:
                return  dateSource == null
                        ? (T) queryRepository.selectCount(FINAL_SQL, params, Long.class)
                        : (T) queryRepository.selectCount(new NamedParameterJdbcTemplate(dateSource), FINAL_SQL, params, Long.class);
            case OBJECT:
                return dateSource == null
                        ? (T) queryRepository.selectObject(FINAL_SQL, params.getValues())
                        : (T) queryRepository.selectObject(new NamedParameterJdbcTemplate(dateSource), FINAL_SQL, params.getValues());
            case TABLE:
            default:
                return dateSource == null
                    ? (T) queryRepository.selectTable(FINAL_SQL, params, new QueryRowMapper(queryConfig))
                    : (T) queryRepository.selectTable(new NamedParameterJdbcTemplate(dateSource), FINAL_SQL, params, new QueryRowMapper(queryConfig));
        }
    }

    /** Поулчить конфигурацию по параметрам */
    private QueryConfig getConfigByName (DataSource dateSource, String configName, UUID userId, List<String> userRoles) {
        return dateSource == null
                ? queryConfigService.getByName(configName, userId, userRoles)
                : queryConfigService.getByName(dateSource, configName, userId, userRoles);
    }

    /** Получение RowNumber SQL */
    private String getRowNumber(List<QueryField> queryFields){
        Optional<QueryField> rowNumber = queryFields.stream().filter(o -> o.getName().equals("row_number")).findFirst();
        return rowNumber.filter(QueryField::getVisible).map(queryField -> SQL_ROW_NUMBER).orElse("");
//        if(rowNumber.isPresent()) {
//            return rowNumber.get().getVisible() ? SQL_ROW_NUMBER : "";
//        } else {
//            return "";
//        }
    }

    /** Задать Where или Having в зависимости от типа поля */
    private void setWhereOrHaving(QueryField queryField, List<String> where, List<String> having, String param) {
        if(queryField.getTypeField().equals("customAggregate")){
            having.add(param);
        } else {
            where.add(param);
        }
    }

    /** Получить name для кастомного поля */
    private String getCustomFieldName(QueryField queryField, char fieldJoinCode){
        String expression = queryField.getName();
        expression = expression.replaceAll("\\{0\\}", "a");
        expression = expression.replaceAll("\\{1\\}", String.valueOf(fieldJoinCode));
        return expression;
    }

    private List<ObjectNode> getHierarchicalData(List<ObjectNode> flatData, String hierarchyField) {

        String ID_KEY = "id";
        String PARENT_KEY = "parent_id";
        String CHILDREN_KEY = "children";

        if(hierarchyField != null){
            String[] hField = hierarchyField.split("/");
            if(hField.length >= 2) {
                ID_KEY = hField[0];
                PARENT_KEY = hField[1];
            }
        }

        List<ObjectNode> treeData = new ArrayList<>();
        Map<String, ArrayNode> childrenOf = new HashMap<>();
        ObjectNode item;
        JsonNode id, parentId;

        for (ObjectNode datum : flatData) {

            item = datum;
            id = item.get(ID_KEY);
            parentId = item.get(PARENT_KEY);

            // Каждый элемент может иметь детей
            // Если есть, то обновить, если нет, то создать пустой массив
            if (childrenOf.get(id.asText()) != null) {
                childrenOf.put(id.asText(), childrenOf.get(id.asText()));
            } else {
                childrenOf.put(id.asText(), OBJECT_MAPPER.createArrayNode());
            }

            // Инициализируем детей у элемента
//            item.putArray(CHILDREN_KEY, childrenOf.get(id.asText()));
            item.putArray(CHILDREN_KEY);
            item.set(CHILDREN_KEY, childrenOf.get(id.asText()));

            // Если есть родитель и он не пустой
            if (parentId != null && !parentId.isNull()) {

                // Если у родителя нет детей, то добавим пустой массив
                if (childrenOf.get(parentId.asText()) != null) {
                    childrenOf.put(parentId.asText(), childrenOf.get(parentId.asText()));
                } else {
                    childrenOf.put(parentId.asText(), OBJECT_MAPPER.createArrayNode());
                }

                // Добавить в родителя текущий элемент
                childrenOf.get(parentId.asText()).add(item);

            } else {
                // Добавить элемент в дерево
                treeData.add(item);
            }
        }
        return treeData;
    }

    private Boolean removeRecursively(ObjectNode treeItem, Map<String, Pattern> searchFields) {
        // Если treeItem имеет детей
        if (treeItem.has("children") && treeItem.get("children").isArray() && treeItem.get("children").size() > 0) {
            boolean needRemoveNode = true;
            ArrayNode children = (ArrayNode) treeItem.get("children");
            // Перебор всех детей с конца и рекурсивное удаление
            for(int i = children.size()-1; i >= 0 ; i--)
                if(removeRecursively((ObjectNode) children.get(i), searchFields))
                    children.remove(i);
                else
                    needRemoveNode = false;
            // log.info("[{}] => {}", treeItem.get("name").asText(), needRemoveNode);
            return needRemoveNode;
        } else {
            boolean needRemoveSheet = true;
            for (String field : searchFields.keySet()) {
                // log.info("Sheet key: {}", field);
                String name = treeItem.get(field).asText();
                if(searchFields.get(field).matcher(name).find()){
                    // log.info("Sheet key: {}, value: [{}], result [true]", field, name);
                    needRemoveSheet = false;
                }
                // else
                //     log.info("Sheet key: {}, value: [{}], result [false]", field, name);
            }
            return needRemoveSheet;
        }
    }
}
