package com.irontechspace.dynamicdq.service;

import com.irontechspace.dynamicdq.model.ConfigField;
import com.irontechspace.dynamicdq.model.ConfigTable;
import com.irontechspace.dynamicdq.model.JoinTable;
import com.irontechspace.dynamicdq.repository.DataRepository;
import com.irontechspace.dynamicdq.repository.RowMapper.DataRowMapper;
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
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;

@Log4j2
@Service
public class DataService {

    @Autowired
    ConfigService configService;

    @Autowired
    DataRepository dataRepository;

    @Autowired
    TypeConverter typeConverter;


    private static final String SQL_SELECT = " select %s ";

    private static final String SQL_FROM = "\n from %s a \n";

    private static final String SQL_JOIN = "\t left join %s %s on %s.%s = %s.%s \n";

    private static final String SQL_WHERE_1_1 = " where 1=1 \n";

    private static final String SQL_GROUP_BY = " group by %s \n";

    private static final String SQL_ORDER_BY = " order by %s \n";

    private static final String SQL_PAGEABLE = "" +
            " limit :pageSize \n" +
            " offset :offset \n";

    private static final String SQL_ROW_NUMBER = "" +
            "row_number() over() + :offset as row_number, ";


    /** Получение плоской таблицы данных */
    public List<ObjectNode> getFlatData(String configName, UUID userId, List<String> userRoles, JsonNode filter, Pageable pageable){

        ConfigTable configTable = configService.getConfigByConfigName(configName, userId, userRoles);

        return getFlatData(configTable, userId, userRoles, filter, pageable);
    }

    /** Получение кол-ва данных в плоской таблице */
    public Long getFlatDataCount(String configName, UUID userId, List<String> userRoles, JsonNode filter, Pageable pageable){

        ConfigTable configTable = configService.getConfigByConfigName(configName, userId, userRoles);

        return getFlatData(configTable, userId, userRoles, filter, pageable, true);
    }

    /** Получение иерархической таблицы данных */
    public List<ObjectNode> getHierarchicalData(String configName, UUID userId, List<String> userRoles, JsonNode filter, Pageable pageable){

        ConfigTable configTable = configService.getConfigByConfigName(configName, userId, userRoles);

        List<ObjectNode> result = getHierarchicalData(getFlatData(configTable, userId, userRoles, filter, pageable), configTable.getHierarchyField());

        // Формирование мапы параметров поиска
        Map<String, Pattern> searchFields = new HashMap<>();
        for(ConfigField field : configTable.getFields()){
            if(field.getFilterFields() != null && field.getFilterSigns() != null){
                String[] filterFields = field.getFilterFields().split("/");
                String[] filterSigns = field.getFilterSigns().split("/");
                if(filterFields.length == filterSigns.length){
                    for (int i = 0; i < filterFields.length; i++) {
                        if (filter.has(filterFields[i])
                                && (filterSigns[i].toLowerCase().equals("ilike") || filterSigns[i].toLowerCase().equals("like"))) {
                            log.info("Add searchFields [{}] => {}", field.getAliasOrName(), filter.get(filterFields[i]).asText().replaceAll("\\s", ".+?"));
                            searchFields.put(
                                    field.getAliasOrName(),
                                    Pattern.compile(filter.get(filterFields[i]).asText().replaceAll("\\s", ".+?")));
                        }
                    }
                }
            }
        }
        // Если мапа параметров поиска не пустая, то ищем
        if(searchFields.size() > 0) {
            log.info("Search by tree");
            for (int i = result.size() - 1; i >= 0; i--) {
                if (removeRecursively(result.get(i), searchFields)) {
                    result.remove(i);
                }
            }
        } else
            log.info("Skip search by tree");
        return result;
    }


    /** Получение RowNumber SQL */
    private String getRowNumber(List<ConfigField> fields){
        Optional<ConfigField> rowNumber = fields.stream().filter(o -> o.getName().equals("row_number")).findFirst();
        if(rowNumber.isPresent()) {
            return rowNumber.get().getVisible() ? SQL_ROW_NUMBER : "";
        } else {
            return "";
        }
    }

    private List<ObjectNode> getFlatData(ConfigTable configTable, UUID userId, List<String> userRoles, JsonNode filter, Pageable pageable){
        return getFlatData(configTable, userId, userRoles, filter, pageable, false);
    }

    private <T> T getFlatData(ConfigTable configTable, UUID userId, List<String> userRoles, JsonNode filter, Pageable pageable, Boolean countQuery) {

        List<ConfigField> fields = configTable.getFields();
        List<String> sourceFields = new ArrayList<>();
        List<JoinTable> joinTables = new ArrayList<>();
        List<String> filterOutside = new ArrayList<>();
        List<String> groupBy = new ArrayList<>();
        List<String> having = new ArrayList<>();
        SortedMap<Integer, String> orderByInside = new TreeMap<>();

        log.info("countQuery => [{}]", countQuery);

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
        Boolean linkBackExist = fields.stream().anyMatch(o -> aggregateTypes.contains(o.getTypeField()));

        for(ConfigField field : fields){

            // Если поле не row_number
            if(!field.getName().equals("row_number")) {

                // Если поле содержит ссылку - создаем каскад join-ов
                if (field.getLinkPath() != null) {

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

                    String[] joinObjs = field.getLinkPath().split("/"); // joinLine.split("/");

                    String parentJoinParams = "";

                    for(int i = 0; i < joinObjs.length; i++){

                        String[] joinObjsParams = joinObjs[i].split("\\.");

                        if(i == 0)
                            parentJoinParams = String.format("%s", configTable.getTableName());
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
                switch (field.getTypeField()){
                    case "column":
                        sourceFields.add(String.format("\n a.%s as %s", field.getName(), field.getAliasOrName() ));
                        break;
                    case "link":
                        sourceFields.add(String.format("\n %s.%s as %s", fieldJoinCode, field.getLinkView(), field.getAliasOrName() ));
                        break;
                    case "linkBack":
                        String[] linkViewFields = field.getLinkView().split("/");
                        List<String> jsonBuildParams = new ArrayList<>();
                        for (String linkViewField : linkViewFields) {
                            String[] viewFields = linkViewField.split(":");
                            if(viewFields.length == 2)
                                jsonBuildParams.add(String.format("'%s', %s.%s", viewFields[0], fieldJoinCode, viewFields[1].replaceAll("([^_A-Z])([A-Z])", "$1_$2").toLowerCase()));
                            else
                                jsonBuildParams.add(String.format("'%s', %s.%s", linkViewField, fieldJoinCode, linkViewField.replaceAll("([^_A-Z])([A-Z])", "$1_$2").toLowerCase()));
                        }
                        sourceFields.add(String.format("\n json_agg(json_build_object(%s)) as %s", String.join(", ", jsonBuildParams), field.getAliasOrName()));
                        break;

                    case "custom":
                    case "customAggregate":
                        sourceFields.add(String.format("\n %s as %s", getCustomFieldName(field, fieldJoinCode), field.getAliasOrName()));
                        break;
                }

                if(!aggregateTypes.contains(field.getTypeField()))
                    if(linkBackExist)
                        if(field.getLinkPath() != null)
                            groupBy.add(String.format("\n %s.%s", fieldJoinCode, field.getLinkView()));
                        else
                            groupBy.add(String.format("\n a.%s", field.getName()));

                // and {0} = 13
                // and {0} = 5
                // and {1} ilike '%54%'

                // Фильтрация по полю (системная = дефолтная)
                // {0} = 5 transform to a.[name] = 5
                // {1} = 5 transform to [JoinCode].[LinkView] = 5
                if(field.getFilterInside() != null){
                    String whereInside = field.getFilterInside();
                    whereInside = whereInside.replaceAll("\\{0\\}", String.format("a.%s", field.getName()));
                    whereInside = whereInside.replaceAll("\\{1\\}", String.format("%s.%s", fieldJoinCode, field.getLinkView()));
                    if(whereInside.startsWith("and"))
                        filterOutside.add(String.format(" %s \n", whereInside));
                    else
                        filterOutside.add(String.format(" and %s \n", whereInside));
//                    log.debug(whereInside);
                }


                // Фильтрация по полю (пользовательская)
                if(field.getFilterFields() != null && field.getFilterSigns() != null){
                    String[] filterFields = field.getFilterFields().split("/");
                    String[] filterSigns = field.getFilterSigns().split("/");

                    // Если кол-ва параметров фильтрации равны, то фильтруем
                    if(filterFields.length == filterSigns.length){

                        for (int i = 0; i < filterFields.length; i++){

                            if(configTable.getHierarchical() != null && configTable.getHierarchical()
                                && (filterSigns[i].toLowerCase().equals("ilike") || filterSigns[i].toLowerCase().equals("like"))){
                                log.info("Skip like filter");
                                continue;
                            }

                            if(filter.has(filterFields[i])) {
                                boolean paramOnly = filterSigns[i].equals("paramOnly");
                                String whereFieldName;
                                String whereParamName;

                                // Опеределяем имя поля и имя параметра
                                if (field.getLinkPath() != null) {
                                    whereFieldName = String.format("%s.%s", fieldJoinCode, field.getLinkView());
                                    whereParamName = String.format("%s.%s.%s", fieldJoinCode, field.getLinkView(), filterFields[i]);
                                } else {
                                    whereFieldName = String.format("a.%s", field.getName());
                                    whereParamName = String.format("a.%s.%s", field.getAliasOrName(), filterFields[i]);
                                }

                                // Меняем имя поля если тип "aggregate" или "math"
                                if(field.getTypeField().equals("custom") || field.getTypeField().equals("customAggregate")) {
                                    whereFieldName = getCustomFieldName(field, fieldJoinCode);
                                }

                                // Если только параметр, то задать имя параметра от пользователя
                                if(paramOnly)
                                    whereParamName = String.format("%s", filterFields[i]);

                                /** Если массив */
                                if(filter.get(filterFields[i]).isArray()) {
                                    ArrayNode jsonNodes = (ArrayNode) filter.get(filterFields[i]);

                                    // Оперделяем тип и собираем массив
                                    List<Object> values = typeConverter.getListObjectsByType(field.getTypeData(), jsonNodes);

                                    params.addValue(whereParamName, values);

                                    // Делаем строку запроса, если массив не пустой
                                    if(values.size() > 0 && !paramOnly)
                                        setWhereOrHaving(field, filterOutside, having, String.format(" and %s %s (:%s) \n", whereFieldName, filterSigns[i].toLowerCase(), whereParamName));
//                                        filterOutside.add(String.format(" and %s %s (:%s) \n", whereFieldName, filterSigns[i].toLowerCase(), whereParamName));

                                } else {
                                    // Если значение null
                                    if(filter.get(filterFields[i]).isNull()){
                                        if(!paramOnly)
                                            if(filterSigns[i].toLowerCase().equals("="))
                                                setWhereOrHaving(field, filterOutside, having, String.format(" and %s is null \n", whereFieldName));
//                                                filterOutside.add(String.format(" and %s is null \n", whereFieldName));
                                            else
                                                setWhereOrHaving(field, filterOutside, having, String.format(" and %s is not null \n", whereFieldName));
//                                                filterOutside.add(String.format(" and %s is not null \n", whereFieldName));
                                    } else {
                                        // Делаем строку запроса
                                        if(!paramOnly)
                                            setWhereOrHaving(field, filterOutside, having, String.format(" and %s %s :%s \n", whereFieldName, filterSigns[i].toLowerCase(), whereParamName));
//                                            filterOutside.add(String.format(" and %s %s :%s \n", whereFieldName, filterSigns[i].toLowerCase(), whereParamName));

                                        // Оперделяем тип и получаем значение
                                        Object value = typeConverter.getObjectByType(field.getTypeData(), filterFields[i], filter);

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
                if(field.getOrderByInside() != null) {
                    String[] orderByIn = field.getOrderByInside().split("\\.");
                    if(field.getTypeField().equals("link") || field.getTypeField().equals("linkBack"))
                        orderByInside.put(Integer.valueOf(orderByIn[0]), String.format("%s.%s %s", fieldJoinCode, field.getName(), orderByIn[1]));
                    if(field.getTypeField().equals("custom") || field.getTypeField().equals("customAggregate"))
                        orderByInside.put(Integer.valueOf(orderByIn[0]), String.format("%s %s", getCustomFieldName(field, fieldJoinCode), orderByIn[1]));
                    else // column
                        orderByInside.put(Integer.valueOf(orderByIn[0]), String.format("a.%s %s", field.getName(), orderByIn[1]));
                }
            }
        }

        // Сортировка от клиента
        if(pageable.getSort().isSorted())
            orderByInside.put(100, pageable.getSort().stream().map(order -> String.format("%s %s", order.getProperty(), order.getDirection().name())).collect(joining(", ")));


        String FINAL_SQL;
        if(configTable.getCustomSql() != null){
            String sql = configTable.getCustomSql();

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
            if (pageable.getPageSize() != 1)
                sql = sql.replaceAll( ":PAGEABLE", SQL_PAGEABLE);
            else
                sql = sql.replaceAll( ":PAGEABLE", "");

            log.debug("Before repository -> \n{}", sql);
            FINAL_SQL = sql;
//            return dataRepository.getTable(sql, params, new DataRowMapper(configTable));

        } else {

            /** Формирование SQL */
            StringBuilder sql = new StringBuilder();
            sql.append("with t as ( \n");

            // Добавили поля для выборки
            sql.append(String.format(SQL_SELECT, String.join(", ", sourceFields)));

            // Добавили таблицу для выборки
            sql.append(String.format(SQL_FROM, configTable.getTableName()));

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
            if (!countQuery && pageable.getPageSize() != 1) sql.append(SQL_PAGEABLE);

            // Добавить основной селект
            if(countQuery)
                sql.append(") \nselect count(t.*) from t \n");
            else
                sql.append(String.format(") \nselect %s t.* from t \n", getRowNumber(fields)));

            // log.debug(ob);
            log.debug("Before repository pageSize: [{}], offset: [{}] -> \n{}", pageable.getPageSize(), pageable.getOffset(), sql.toString());
            FINAL_SQL = sql.toString();
        }

        if(countQuery)
            return (T) dataRepository.getCount(FINAL_SQL, params, Long.class);
        else
            return (T) dataRepository.getTable(FINAL_SQL, params, new DataRowMapper(configTable));
    }

    private void setWhereOrHaving(ConfigField field, List<String> where, List<String> having, String param) {
        if(field.getTypeField().equals("customAggregate")){
            having.add(param);
        } else {
            where.add(param);
        }
    }

    private String getCustomFieldName(ConfigField field, char fieldJoinCode){
        String expression = field.getName();
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

        ObjectMapper mapper = new ObjectMapper();

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
                childrenOf.put(id.asText(), mapper.createArrayNode());
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
                    childrenOf.put(parentId.asText(), mapper.createArrayNode());
                }

                // Добавить в родителя текущий элемент
                childrenOf.get(parentId.asText()).add(item);

            } else {
                // Добавить элемент в дерево
                treeData.add(item);
            }
        }
        ;

        return treeData;
    }

    private Boolean removeRecursively(ObjectNode treeItem, Map<String, Pattern> searchFields) {
        if (treeItem.has("children") && treeItem.get("children").isArray() && treeItem.get("children").size() > 0) {
            Boolean needRemoveNode = true;

            ArrayNode children = (ArrayNode) treeItem.get("children");
            for(int i = children.size()-1; i >= 0 ; i--){
                if(removeRecursively((ObjectNode) children.get(i), searchFields)){
                    children.remove(i);
                } else {
                    needRemoveNode = false;
                }
            }
//            log.info("[{}] => {}", treeItem.get("name").asText(), needRemoveNode);
            return needRemoveNode;
        } else {
            Boolean needRemoveSheet = true;

            for (String field : searchFields.keySet()) {
//                log.info("Sheet key: {}", field);

                String name = treeItem.get(field).asText();
                if(searchFields.get(field).matcher(name).find()){
//                    log.info("Sheet key: {}, value: [{}], result [true]", field, name);
                    needRemoveSheet = false;
                } else {
//                    log.info("Sheet key: {}, value: [{}], result [false]", field, name);
                }
            }
            return needRemoveSheet;
        }
    }

}
