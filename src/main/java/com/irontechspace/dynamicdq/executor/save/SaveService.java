package com.irontechspace.dynamicdq.executor.save;

import com.irontechspace.dynamicdq.configurator.save.SaveConfigService;
import com.irontechspace.dynamicdq.exceptions.NotFoundException;
import com.irontechspace.dynamicdq.configurator.save.model.SaveConfig;
import com.irontechspace.dynamicdq.configurator.save.model.SaveField;
import com.irontechspace.dynamicdq.configurator.save.model.SaveLogic;
import com.irontechspace.dynamicdq.executor.query.QueryRepository;
import com.irontechspace.dynamicdq.configurator.core.model.TypeSql;
import com.irontechspace.dynamicdq.utils.TypeConverter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.*;
import java.util.stream.Collectors;

import static com.irontechspace.dynamicdq.utils.SqlUtils.*;

@Log4j2
@Service
public class SaveService {

    private final static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    SaveConfigService saveConfigService;

    @Autowired
    QueryRepository queryRepository;

    @Autowired
    TypeConverter typeConverter;

    @Transactional
    public Object saveData(String configName, UUID userId, List<String> userRoles, JsonNode dataObject) {
        return saveData(null, configName, userId, userRoles, dataObject);
    }

    @Transactional
    public Object saveData(DataSource dateSource, String configName, UUID userId, List<String> userRoles, JsonNode dataObject) {
        Object result = null;
        try {
            SaveConfig saveConfig = dateSource == null
                    ? saveConfigService.getByName(configName, userId, userRoles)
                    : saveConfigService.getByName(dateSource, configName, userId, userRoles);
            SaveLogic saveLogic = OBJECT_MAPPER.readValue(saveConfig.getLogic(), SaveLogic.class);
            result = analysisLogic(dateSource, saveLogic, dataObject, null, userId, userRoles, saveConfig.getLoggingQueries());
        } catch (JsonProcessingException e){
            e.printStackTrace();
        }
        return result;
    }

    public Object analysisLogic(DataSource dateSource, SaveLogic saveLogic, JsonNode dataObject, Object parentResult, UUID userId, List<String> userRoles, Boolean loggingQueries) {
        // Если элемент вложенной логики - массив
        if (saveLogic.getFieldType().equals("array")) {
            if (dataObject.has(saveLogic.getFieldName()) && dataObject.get(saveLogic.getFieldName()).getNodeType() == JsonNodeType.ARRAY) {
                List<Object> results = new ArrayList<>();
                Object subResult = null;
                for (JsonNode data : dataObject.get(saveLogic.getFieldName())) {
                    subResult = save(dateSource, saveLogic, data, parentResult, userId, userRoles, loggingQueries);
                    results.add(subResult);
                    if(data.has("children") && data.get("children").isArray() && data.get("children").size() > 0){
                        ObjectNode children = OBJECT_MAPPER.createObjectNode();
                        children.put(saveLogic.getFieldName(), data.get("children"));
                        analysisLogic(dateSource, saveLogic, children, subResult, userId, userRoles, loggingQueries);
                    }
                }
                deleteRecursive(dateSource, saveLogic, results, parentResult, loggingQueries);
                return results;
            } else {
                // TODO вернуть ошибку, что описанной логики не найдено во объекте сохранения
                throw new NotFoundException(String.format("Не найдено массива с именем [%s]", saveLogic.getFieldName()));
            }
        } else if (saveLogic.getFieldType().equals("root")) {
            return save(dateSource, saveLogic, dataObject, parentResult, userId, userRoles, loggingQueries);
        } else {
            if(dataObject.has(saveLogic.getFieldName()))
                return save(dateSource, saveLogic, dataObject.get(saveLogic.getFieldName()), parentResult, userId, userRoles, loggingQueries);
            else {
                // TODO вернуть ошибку, что описанной логики не найдено во объекте сохранения
                throw new NotFoundException(String.format("Не найдено логики сохранения с типом [%s]", saveLogic.getFieldType()));
            }
        }
    }

    private Object save(DataSource dateSource, SaveLogic saveLogic, JsonNode dataObject, Object parentResult, UUID userId, List<String> userRoles, Boolean loggingQueries) {
        if(loggingQueries) log.info("LOGIC => [{}] = [{}]", saveLogic.getFieldType(), saveLogic.getFieldName());
        String selectById = generateSql(TypeSql.SELECT_BY_ID, saveLogic.getTableName(), saveLogic.getFields().stream().map(SaveField::getName).collect(Collectors.toList()), saveLogic.getPrimaryKey(), saveLogic.getExcludePrimaryKey());

        // Точка сохранения результата select by id
        Boolean existObject = false;
        // Точка сохранения результата insert or update
        Object result = null;
        // Параметры запроса
        Map<String, Object> params;


        if(dataObject.has(saveLogic.getPrimaryKey()) && !dataObject.get(saveLogic.getPrimaryKey()).isNull()) {
            params = getParams(saveLogic.getFields(), dataObject, parentResult, userId, userRoles);
            existObject = dateSource == null
                    ? queryRepository.checkExistObject(selectById, params, Object.class)
                    : queryRepository.checkExistObject(new NamedParameterJdbcTemplate(dateSource), selectById, params, Object.class);
            if(loggingQueries) log.info("\n DATA: [{}]\n SQL: [{}]\n", params.toString(), selectById);
        }


        if(!existObject) {
            String insertSql = generateSql(TypeSql.INSERT, saveLogic.getTableName(), saveLogic.getFields().stream().map(SaveField::getName).collect(Collectors.toList()), saveLogic.getPrimaryKey(), saveLogic.getExcludePrimaryKey());
            if(saveLogic.getAutoGenerateCode())
                insertSql = insertSql.replace(":code", generateCodeSql(saveLogic.getTableName()));
            params = getParams(saveLogic.getFields(), dataObject, parentResult, userId, userRoles);

            // Поиск type для primary key в описанных полях логики.
            // Если не найдено, то UUID
            result = dateSource == null
                    ? queryRepository.insert(insertSql, params, getTypePK(saveLogic))
                    : queryRepository.insert(new NamedParameterJdbcTemplate(dateSource), insertSql, params, getTypePK(saveLogic));

            params.put(saveLogic.getPrimaryKey(), result);
            if(loggingQueries) log.info("\n DATA: [{}]\n SQL: [{}]\n", params.toString(), insertSql);
        } else {
            String updateSql = generateSql(TypeSql.UPDATE, saveLogic.getTableName(), saveLogic.getFields().stream().map(SaveField::getName).collect(Collectors.toList()), saveLogic.getPrimaryKey(), saveLogic.getExcludePrimaryKey());
            params = getParams(saveLogic.getFields(), dataObject, parentResult, userId, userRoles);
            result = params.get(saveLogic.getPrimaryKey());
            if(dateSource == null)
                queryRepository.update(updateSql, params);
            else
                queryRepository.update(new NamedParameterJdbcTemplate(dateSource), updateSql, params);
            if(loggingQueries) log.info("\n DATA: [{}]\n SQL: [{}]\n", params.toString(), updateSql);
        }

        if(result != null) {
            // Если существуют вложенные логики
            if (saveLogic.getChildren() != null && saveLogic.getChildren().size() > 0) {
                for (SaveLogic childSaveLogic : saveLogic.getChildren()) {
                    analysisLogic(dateSource, childSaveLogic, dataObject, result, userId, userRoles, loggingQueries);
                }
            }
        }
        return result;
    }

    private Class getTypePK (SaveLogic saveLogic){
        String typePK = saveLogic.getFields().stream().filter(saveField -> saveField.getName().equals(saveLogic.getPrimaryKey())).findFirst().orElse(new SaveField(saveLogic.getPrimaryKey(), "uuid")).getType();
        switch (typePK) {
            case "text":
                return String.class;
            case "int":
                return Long.class;
            default:
                return UUID.class;
        }
    }

    private void deleteRecursive(DataSource dateSource, SaveLogic saveLogic, List<Object> excludeResults, Object parentResult, Boolean loggingQueries){

        String where;
        String selectSql;
        String deleteSql;
        Map<String, Object> params = new HashMap<>();

        Optional<SaveField> whereDeleteField = saveLogic.getFields().stream().filter(saveField -> saveField.getType().equals("parentResult")).findFirst();
        if(whereDeleteField.isPresent()){
            // Если исключать есть что, то добавляем в SQL
            if(excludeResults.size() > 0) {
                where = String.format("%s=:%s and %s not in (:%s)",
                        whereDeleteField.get().getName().replaceAll("([^_A-Z])([A-Z])", "$1_$2").toLowerCase(),
                        whereDeleteField.get().getName(),
                        saveLogic.getPrimaryKey().replaceAll("([^_A-Z])([A-Z])", "$1_$2").toLowerCase(),
                        saveLogic.getPrimaryKey());

                selectSql = getSqlForRecursiveSelect(saveLogic.getTableName(), where, saveLogic.getPrimaryKey());
                deleteSql = getSqlForRecursiveDelete(saveLogic.getTableName(), where);
            } else {
                where = String.format("%s=:%s",
                        whereDeleteField.get().getName().replaceAll("([^_A-Z])([A-Z])", "$1_$2").toLowerCase(),
                        whereDeleteField.get().getName());
                selectSql = getSqlForRecursiveSelect(saveLogic.getTableName(), where, saveLogic.getPrimaryKey());
                deleteSql = getSqlForRecursiveDelete(saveLogic.getTableName(), where);
            }
            params.put(whereDeleteField.get().getName(), parentResult);
            params.put(saveLogic.getPrimaryKey(), excludeResults);
        } else return;

        if (saveLogic.getChildren() != null && saveLogic.getChildren().size() > 0) {

            // Для каждого значения перебрать логики с удалением
            List<Object> results = dateSource == null
                    ? queryRepository.selectTable(selectSql, params, getTypePK(saveLogic))
                    : queryRepository.selectTable(new NamedParameterJdbcTemplate(dateSource), selectSql, params, getTypePK(saveLogic));
            if(loggingQueries) log.info("\n DATA: [{}]\n SQL: [{}]\n", params.toString(), selectSql);
            for (Object result : results) {
                for (SaveLogic childSaveLogic : saveLogic.getChildren()) {
                    deleteRecursive(dateSource, childSaveLogic, new ArrayList<>(), result, loggingQueries);
                }
            }
        }
        if(dateSource == null)
            queryRepository.delete(deleteSql, params);
        else
            queryRepository.delete(new NamedParameterJdbcTemplate(dateSource), deleteSql, params);
        if(loggingQueries) log.info("\n DATA: [{}]\n SQL: [{}]\n", params.toString(), deleteSql);

    }

    private Map<String, Object> getParams(List<SaveField> saveFields, JsonNode dataObject, Object parentResult, UUID userId, List<String> userRoles){
        Map<String, Object> params = new HashMap<>();
        for(SaveField saveField : saveFields){
            params.put(saveField.getName(), getParamByType(saveField.getType(), saveField.getName(), dataObject, parentResult));
        }
        params.putIfAbsent("userId", userId);
        params.putIfAbsent("userRoles", userRoles);
        return params;
    }

    private Object getParamByType (String type, String  field, JsonNode dataObject, Object parentResult){
        return typeConverter.getObjectByType(type, field, dataObject, parentResult);
    }
}