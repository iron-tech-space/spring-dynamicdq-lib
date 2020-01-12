package com.irontechspace.dynamicdq.service;

import com.irontechspace.dynamicdq.model.SaveData.Field;
import com.irontechspace.dynamicdq.model.SaveData.Logic;
import com.irontechspace.dynamicdq.repository.DataRepository;
import com.irontechspace.dynamicdq.model.TypeSql;
import com.irontechspace.dynamicdq.utils.TypeConverter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static com.irontechspace.dynamicdq.utils.SqlUtils.*;

@Log4j2
@Service
public class SaveDataService {

    @Autowired
    SaveConfigService saveConfigService;

    @Autowired
    DataRepository dataRepository;

    @Autowired
    TypeConverter typeConverter;

    @Transactional
    public Object saveData(String configName, UUID userId, List<String> userRoles, JsonNode dataObject) {

        ObjectMapper objectMapper = new ObjectMapper();
        Object result = null;
        try {
            String logicJson = saveConfigService.getSaveConfigByConfigName(configName, userId, userRoles);
            Logic logic = objectMapper.readValue(logicJson, Logic.class);
            result = analysisLogic(logic, dataObject, null, userId, userRoles);
        } catch (JsonProcessingException e){
            e.printStackTrace();
        }
        return result;
    }

    private Object analysisLogic(Logic logic, JsonNode dataObject, Object parentResult, UUID userId, List<String> userRoles) {
        // Если элемент вложенной логики - массив
        if (logic.getFieldType().equals("array")) {
            if (dataObject.has(logic.getFieldName()) && dataObject.get(logic.getFieldName()).getNodeType() == JsonNodeType.ARRAY) {
                List<Object> results = new ArrayList<>();
                Object subResult = null;
                for (JsonNode data : dataObject.get(logic.getFieldName())) {
                    subResult = save(logic, data, parentResult, userId, userRoles);
                    results.add(subResult);
                    if(data.has("children") && data.get("children").isArray() && data.get("children").size() > 0){
                        ObjectNode children = new ObjectMapper().createObjectNode();
                        children.put(logic.getFieldName(), data.get("children"));
                        analysisLogic(logic, children, subResult, userId, userRoles);
                    }
                }
                deleteRecursive(logic, results, parentResult);
                return results;
            } else {
                //TODO вернуть ошибку, что описанной логики не найдено во объекте сохранения
                return null;
            }
        } else if (logic.getFieldType().equals("root")) {
            return save(logic, dataObject, parentResult, userId, userRoles);
        } else {
            if(dataObject.has(logic.getFieldName()))
                return  save(logic, dataObject.get(logic.getFieldName()), parentResult, userId, userRoles);
            else {
                //TODO вернуть ошибку, что описанной логики не найдено во объекте сохранения
                return null;
            }
        }
    }

    private Object save(Logic logic, JsonNode dataObject, Object parentResult, UUID userId, List<String> userRoles) {
        log.info("LOGIC => [{}] = [{}]", logic.getFieldType(), logic.getFieldName());
        String selectById = generateSql(TypeSql.SELECT_BY_ID, logic.getTableName(), logic.getFields().stream().map(Field::getName).collect(Collectors.toList()), logic.getPrimaryKey(), logic.getExcludePrimaryKey());

        // Точка сохранения результата select by id
        Object selectObject = null;
        // Точка сохранения результата insert or update
        Object result = null;
        // Параметры запроса
        Map<String, Object> params;


        if(dataObject.has(logic.getPrimaryKey()) && !dataObject.get(logic.getPrimaryKey()).isNull()) {
            params = getParams(logic.getFields(), dataObject, parentResult, userId, userRoles);
            selectObject = dataRepository.findByPK(selectById, params, Object.class);
            log.info("\n DATA: [{}]\n SQL: [{}]\n", params.toString(), selectById);
        }


        if(selectObject == null) {
            String insertSql = generateSql(TypeSql.INSERT, logic.getTableName(), logic.getFields().stream().map(Field::getName).collect(Collectors.toList()), logic.getPrimaryKey(), logic.getExcludePrimaryKey());
            if(logic.getAutoGenerateCode())
                insertSql = insertSql.replace(":code", generateCodeSql(logic.getTableName()));
            params = getParams(logic.getFields(), dataObject, parentResult, userId, userRoles);

            // Поиск type для primary key в описанных полях логики.
            // Если не найдено, то UUID
//            String typePK = logic.getFields().stream().filter(field -> field.getName().equals(logic.getPrimaryKey())).findFirst().orElse(new Field(logic.getPrimaryKey(), "uuid")).getType();
//
//            switch (typePK) {
//                case "uuid":
//                    result = dataRepository.insert(insertSql, params, UUID.class);
//                    break;
//                case "text":
//                    result = dataRepository.insert(insertSql, params, String.class);
//                    break;
//                case "int":
//                    result = dataRepository.insert(insertSql, params, Long.class);
//                    break;
//            }

            result = dataRepository.insert(insertSql, params, getTypePK(logic));

            params.put(logic.getPrimaryKey(), result);
            log.info("\n DATA: [{}]\n SQL: [{}]\n", params.toString(), insertSql);
        } else {
            String updateSql = generateSql(TypeSql.UPDATE, logic.getTableName(), logic.getFields().stream().map(Field::getName).collect(Collectors.toList()), logic.getPrimaryKey(), logic.getExcludePrimaryKey());
            params = getParams(logic.getFields(), dataObject, parentResult, userId, userRoles);
            result = params.get(logic.getPrimaryKey());
            dataRepository.update(updateSql, params);
            log.info("\n DATA: [{}]\n SQL: [{}]\n", params.toString(), updateSql);
        }

        if(result != null) {
            // Если существуют вложенные логики
            if (logic.getChildren() != null && logic.getChildren().size() > 0) {
                for (Logic childLogic : logic.getChildren()) {
                    analysisLogic(childLogic, dataObject, result, userId, userRoles);
                }
            }
        }
        return result;
    }

    private Class getTypePK (Logic logic){
        String typePK = logic.getFields().stream().filter(field -> field.getName().equals(logic.getPrimaryKey())).findFirst().orElse(new Field(logic.getPrimaryKey(), "uuid")).getType();
        switch (typePK) {
            case "text":
                return String.class;
            case "int":
                return Long.class;
            default:
                return UUID.class;
        }
    }

    private void deleteRecursive(Logic logic, List<Object> excludeResults, Object parentResult){

        String where;
        String selectSql;
        String deleteSql;
        Map<String, Object> params = new HashMap<>();

        Optional<Field> whereDeleteField = logic.getFields().stream().filter(field -> field.getType().equals("parentResult")).findFirst();
        if(whereDeleteField.isPresent()){
            // Если исключать есть что, то добавляем в SQL
            if(excludeResults.size() > 0) {
                where = String.format("%s=:%s and %s not in (:%s)",
                        whereDeleteField.get().getName().replaceAll("([^_A-Z])([A-Z])", "$1_$2").toLowerCase(),
                        whereDeleteField.get().getName(),
                        logic.getPrimaryKey().replaceAll("([^_A-Z])([A-Z])", "$1_$2").toLowerCase(),
                        logic.getPrimaryKey());

                selectSql = getSqlForRecursiveSelect(logic.getTableName(), where, logic.getPrimaryKey());
                deleteSql = getSqlForRecursiveDelete(logic.getTableName(), where);
            } else {
                where = String.format("%s=:%s",
                        whereDeleteField.get().getName().replaceAll("([^_A-Z])([A-Z])", "$1_$2").toLowerCase(),
                        whereDeleteField.get().getName());
                selectSql = getSqlForRecursiveSelect(logic.getTableName(), where, logic.getPrimaryKey());
                deleteSql = getSqlForRecursiveDelete(logic.getTableName(), where);
            }
            params.put(whereDeleteField.get().getName(), parentResult);
            params.put(logic.getPrimaryKey(), excludeResults);
        } else return;

        if (logic.getChildren() != null && logic.getChildren().size() > 0) {

            // Для каждого значения перебрать логики с удалением
            List<Object> results = dataRepository.getTable(selectSql, params, getTypePK(logic));
            log.info("\n DATA: [{}]\n SQL: [{}]\n", params.toString(), selectSql);
            for (Object result : results) {
                for (Logic childLogic : logic.getChildren()) {
                    deleteRecursive(childLogic, new ArrayList<>(), result);
                }
            }
        }
        dataRepository.delete(deleteSql, params);
        log.info("\n DATA: [{}]\n SQL: [{}]\n", params.toString(), deleteSql);

    }

    private Map<String, Object> getParams(List<Field> fields, JsonNode dataObject, Object parentResult, UUID userId, List<String> userRoles){
        Map<String, Object> params = new HashMap<>();
        for(Field field : fields){
            params.put(field.getName(), getParamByType(field.getType(), field.getName(), dataObject, parentResult));
        }
        params.putIfAbsent("userId", userId);
        params.putIfAbsent("userRoles", userRoles);
        return params;
    }

    private Object getParamByType (String type, String  field, JsonNode dataObject, Object parentResult){
        return typeConverter.getObjectByType(type, field, dataObject, parentResult);
    }
}
