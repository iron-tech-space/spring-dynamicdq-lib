package com.irontechspace.dynamicdq.repository;

import com.irontechspace.dynamicdq.model.ConfigField;
import com.irontechspace.dynamicdq.model.ConfigTable;
import com.irontechspace.dynamicdq.repository.utils.SqlUtils;
import com.irontechspace.dynamicdq.repository.utils.TypeGenerateSql;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static com.irontechspace.dynamicdq.repository.utils.SqlUtils.generateSql;

@Log4j2
@Repository
public class ConfigRepository {

    private final static String SQL_GET_TABLES = "" +
            "SELECT * \n" +
            "   FROM %s a \n" +
            "   %s \n" +
            "   order by position ";

    private final static String SQL_GET_FIELDS = "" +
            "select b.* \n" +
            "   from %s b \n" +
            "   where b.id_config = :id \n" +
            "   order by position \n";

    @Value("${sys.configuration.tables}")
    private String tablesTableName;

    @Value("${sys.configuration.fields}")
    private String fieldsTableName;


    private static final RowMapper<ConfigTable> ROW_MAPPER_TABLES = BeanPropertyRowMapper.newInstance(ConfigTable.class);
    private static final RowMapper<ConfigField> ROW_MAPPER_FIELDS = BeanPropertyRowMapper.newInstance(ConfigField.class);

    @Autowired
    NamedParameterJdbcTemplate jdbcTemplate;

    List<String> excludeFieldsNames = Arrays.asList("id", "fields");

    /** Получить все конфигурации */
    public List<ConfigTable> getConfigs(Long userId){
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("userId", userId);

        String where = " where user_id = " + userId + " or shared_for_users::jsonb @> '[" + userId + "]' ";

        List<ConfigTable> tables = jdbcTemplate.query(String.format(SQL_GET_TABLES, tablesTableName, where), params, ROW_MAPPER_TABLES);

        for(ConfigTable table : tables){
            List<ConfigField> fields = jdbcTemplate.query(String.format(SQL_GET_FIELDS, fieldsTableName), new MapSqlParameterSource("id", table.getId()), ROW_MAPPER_FIELDS);
            table.setFields(fields);
        }

        return tables;
    }

    /** Получить конфигурацию по наименованию и id пользователя */
    public ConfigTable getConfig(String configName, Long userId){

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("configName", configName);
        params.addValue("userId", userId);

        String tablesSql = String.format(SQL_GET_TABLES, tablesTableName, " where config_name = :configName and user_id = :userId");

        List<ConfigTable> tables = jdbcTemplate.query(tablesSql, params, ROW_MAPPER_TABLES);

        if(tables.size() == 0) {
            params.addValue("userId", 1);
            tables = jdbcTemplate.query(tablesSql, params, ROW_MAPPER_TABLES);
        }

        if(tables.size() != 0) {
            List<ConfigField> fields = jdbcTemplate.query(String.format(SQL_GET_FIELDS, fieldsTableName), new MapSqlParameterSource("id", tables.get(0).getId()), ROW_MAPPER_FIELDS);
            tables.get(0).setFields(fields);
            return tables.get(0);
        } else {
            return null;
        }
    }

    /** Сохранить полную конфигурацию */
    @Transactional
    public ConfigTable save(ConfigTable configTable){

        String table_sql;

        if(configTable.getUserId() == null)
            configTable.setUserId(1L);

        // Save
        if(configTable.getId() == null){
            table_sql = SqlUtils.generateSql(TypeGenerateSql.INSERT, tablesTableName, ConfigTable.class, excludeFieldsNames);
            Map<String, Object> tableParams = SqlUtils.getParams(configTable);
            log.info("table_sql (new generateSql) => \n {}", table_sql);
            configTable.setId(jdbcTemplate.queryForObject(table_sql, tableParams, UUID.class));
        }
        // Update
        else{
            table_sql = SqlUtils.generateSql(TypeGenerateSql.UPDATE, tablesTableName, ConfigTable.class, excludeFieldsNames);
            Map<String, Object> tableParams = SqlUtils.getParams(configTable);
            log.info("table_sql (new generateSql) => \n {}", table_sql);
            jdbcTemplate.update(table_sql, tableParams);
            deleteFieldsByTableId(configTable.getId());
        }

        for(ConfigField field : configTable.getFields()){
            field.setIdConfig(configTable.getId());
            String field_sql = SqlUtils.generateSql(TypeGenerateSql.INSERT, fieldsTableName, ConfigField.class, excludeFieldsNames);
            Map<String, Object> params = SqlUtils.getParams(field);
            log.info("field_sql (new generateSql) => \n {}", field_sql);
            jdbcTemplate.queryForObject(field_sql, params, UUID.class);
        }

        return configTable;
    }

    @Transactional
    public void deleteTableById(UUID tableId){
        deleteFieldsByTableId(tableId);
        jdbcTemplate.update("DELETE FROM " + tablesTableName + " WHERE id=:id ", new MapSqlParameterSource("id", tableId));
    }

    public void deleteFieldById(UUID fieldId){
        jdbcTemplate.update("DELETE FROM " + fieldsTableName + " WHERE id=:id ", new MapSqlParameterSource("id", fieldId));
    }

    public void deleteFieldsByTableId(UUID configId){
        jdbcTemplate.update("DELETE FROM " + fieldsTableName + " WHERE id_config=:id ", new MapSqlParameterSource("id", configId));
    }


}
