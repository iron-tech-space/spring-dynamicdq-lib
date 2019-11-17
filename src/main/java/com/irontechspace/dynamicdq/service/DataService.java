package com.irontechspace.dynamicdq.service;

import com.irontechspace.dynamicdq.model.ConfigField;
import com.irontechspace.dynamicdq.model.ConfigTable;
import com.irontechspace.dynamicdq.model.JoinTable;
import com.irontechspace.dynamicdq.repository.DataRepository;
import com.irontechspace.dynamicdq.repository.RowMapper.DataRowMapper;
import com.irontechspace.dynamicdq.repository.utils.SqlUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.format.DateTimeFormatter;
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
    public List<ObjectNode> getFlatData(String configName, JsonNode filter, Pageable pageable){

        ConfigTable configTable = configService.getConfigByConfigName(configName);

        if(configTable == null)
            return null;
        else
            return getFlatData(configTable, filter, pageable);
    }

    /** Получение кол-ва данных в плоской таблице */
    public Long getFlatDataCount(String configName, JsonNode filter, Pageable pageable){

        ConfigTable configTable = configService.getConfigByConfigName(configName);

        if(configTable == null)
            return null;
        else
            return getFlatData(configTable, filter, pageable, true);
    }

    /** Получение иерархической таблицы данных */
    public List<ObjectNode> getHierarchicalData(String configName, JsonNode filter, Pageable pageable){

        ConfigTable configTable = configService.getConfigByConfigName(configName);

        if(configTable == null)
            return null;
        else {
            List<ObjectNode> result = getHierarchicalData(getFlatData(configTable, filter, pageable), configTable.getHierarchyField());

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

    private List<ObjectNode> getFlatData(ConfigTable configTable, JsonNode filter, Pageable pageable){
        return getFlatData(configTable, filter, pageable, false);
    }

    private <T> T getFlatData(ConfigTable configTable, JsonNode filter, Pageable pageable, Boolean countQuery) {

        List<ConfigField> fields = configTable.getFields();
        List<String> sourceFields = new ArrayList<>();
        List<JoinTable> joinTables = new ArrayList<>();
        List<String> filterOutside = new ArrayList<>();
        List<String> groupBy = new ArrayList<>();
        SortedMap<Integer, String> orderByInside = new TreeMap<>();

        log.info("countQuery => [{}]", countQuery);

        // Инициализация параметров запроса
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("pageSize", pageable.getPageSize());
        params.addValue("offset", pageable.getOffset());

        char prevJoinCode;
        char nextJoinCode = 'a';
        char fieldJoinCode = 'b';
        Boolean linkBackExist = fields.stream().anyMatch(o -> o.getType().equals("linkBack"));

        for(ConfigField field : fields){

            // Если поле не row_number
            if(!field.getName().equals("row_number")) {

                // Если поле содержит ссылку или тип - обратная ссылка
                if (field.getLinkPath() != null || field.getType().equals("linkBack")) {

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

                            // Получение поля вывода
                            field.setLinkView(field.getLinkView());

                            // Добавить поле
                            if(field.getType().equals("linkBack"))
                                sourceFields.add(String.format("\n string_agg(%s.%s, ', ') as %s", fieldJoinCode, field.getLinkView(), field.getAliasOrName() ));
                            else {
                                sourceFields.add(String.format("\n %s.%s as %s", fieldJoinCode, field.getLinkView(), field.getAliasOrName() ));
                                if(linkBackExist)
                                    groupBy.add(String.format("\n %s.%s", fieldJoinCode, field.getLinkView()));
                            }
                        }
                    }
                }

                // Если линейное поле
                else {
                    sourceFields.add(String.format("\n a.%s as %s", field.getName(), field.getAliasOrName() ));
                    if(linkBackExist)
                        groupBy.add(String.format("\n a.%s", field.getName()));
                }

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
                                if (field.getLinkPath() != null) {
                                    whereFieldName = String.format("%s.%s", fieldJoinCode, field.getLinkView());
                                    whereParamName = String.format("%s.%s.%s", fieldJoinCode, field.getLinkView(), filterFields[i]);
                                } else {
                                    whereFieldName = String.format("a.%s", field.getName());
                                    whereParamName = String.format("a.%s.%s", field.getAliasOrName(), filterFields[i]);
                                }


                                /**
                                 * Если массив и знак массива
                                 *      Делаем строку запроса
                                 *      Оперделяем тип и собираем массив
                                 * Иначе
                                 *      Если значение null
                                 *          Формируем только строку запросы
                                 *      Иначе
                                 *          Делаем строку запроса
                                 *          Определение типа занчения и приведение типа
                                 */

                                if(filter.get(filterFields[i]).isArray()) {
                                    ArrayNode jsonNodes = (ArrayNode) filter.get(filterFields[i]);
                                    List<Object> values = new ArrayList<>();

                                    switch (field.getType()) {
                                        case "uuid":
                                            for (JsonNode jsonNode : jsonNodes)
                                                if(!jsonNode.isNull())
                                                    values.add(UUID.fromString(jsonNode.asText()));
                                            params.addValue(whereParamName, values);
                                            break;
                                        case "text":
                                            for (JsonNode jsonNode : jsonNodes)
                                                if(!jsonNode.isNull())
                                                    values.add(jsonNode.asText());
                                            params.addValue(whereParamName, values);
                                            break;
                                        case "int":
                                            for (JsonNode jsonNode : jsonNodes)
                                                if(!jsonNode.isNull())
                                                    values.add(jsonNode.asLong());
                                            params.addValue(whereParamName, values);
                                            break;
                                        case "double":
                                            for (JsonNode jsonNode : jsonNodes)
                                                if(!jsonNode.isNull())
                                                    values.add(jsonNode.asDouble());
                                            params.addValue(whereParamName, values);
                                            break;
                                    }
                                    if(values.size() > 0 && !paramOnly)
                                        filterOutside.add(String.format(" and %s %s (:%s) \n", whereFieldName, filterSigns[i].toLowerCase(), whereParamName));

                                } else {
                                    /**
                                     *  +++ uuid
                                     *  +++ varchar
                                     *  +++ int8 / long
                                     *  +++ int4 / integer
                                     *   ++ timestamptz
                                     *   ++ time
                                     *   ++ bool
                                     *  +++ numeric / decimal / double
                                     */

                                    if(filter.get(filterFields[i]).isNull()){
                                        if(!paramOnly)
                                            filterOutside.add(String.format(" and %s %s null \n", whereFieldName, filterSigns[i].toLowerCase()));
                                    }else {
                                        if(!paramOnly)
                                            filterOutside.add(String.format(" and %s %s :%s \n", whereFieldName, filterSigns[i].toLowerCase(), whereParamName));
                                        switch (field.getType()) {
                                            case "uuid":
                                                params.addValue(whereParamName, UUID.fromString(filter.get(filterFields[i]).asText()));
                                                break;
                                            case "text":
                                                if (filterSigns[i].toLowerCase().equals("ilike") || filterSigns[i].toLowerCase().equals("like"))
                                                    params.addValue(whereParamName, SqlUtils.toSearchString(filter.get(filterFields[i]).textValue()));
                                                else
                                                    params.addValue(whereParamName, filter.get(filterFields[i]).asText());
                                                break;
                                            case "int":
                                                params.addValue(whereParamName, filter.get(filterFields[i]).asLong());
                                                break;
                                            case "timestamp":
                                                params.addValue(whereParamName, OffsetDateTime.parse(filter.get(filterFields[i]).asText(), DateTimeFormatter.ISO_OFFSET_DATE_TIME));
                                                break;
                                            case "date":
//                                                try {
                                                    params.addValue(whereParamName, new Date(OffsetDateTime.parse(filter.get(filterFields[i]).asText(), DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant().toEpochMilli()));
//                                                } catch (ParseException e) {
//                                                    e.printStackTrace();
//                                                }
                                                break;
                                            case "time":
                                                params.addValue(whereParamName, OffsetTime.parse(filter.get(filterFields[i]).asText(), DateTimeFormatter.ISO_OFFSET_DATE_TIME));
                                                break;
                                            case "bool":
                                                params.addValue(whereParamName, Boolean.valueOf(filter.get(filterFields[i]).asText()));
                                                break;
                                            case "double":
                                                params.addValue(whereParamName, filter.get(filterFields[i]).asDouble());
                                                break;
                                            default:
                                                params.addValue(whereParamName, filter.get(filterFields[i]).textValue());

                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Сортировка из конфига по полю
                if(field.getOrderByInside() != null) {
                    String[] orderByIn = field.getOrderByInside().split("\\.");
                    orderByInside.put(Integer.valueOf(orderByIn[0]), String.format("%s %s", field.getName(), orderByIn[1]));
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

            // Добавили пагинацию в SQL (если pageSize == 0, то ограничение на кол-во не накладывается)
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
