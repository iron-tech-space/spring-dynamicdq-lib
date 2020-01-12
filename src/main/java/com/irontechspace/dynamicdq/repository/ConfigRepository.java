package com.irontechspace.dynamicdq.repository;

import com.irontechspace.dynamicdq.exceptions.ForbiddenException;
import com.irontechspace.dynamicdq.exceptions.NotFoundException;
import com.irontechspace.dynamicdq.model.ConfigField;
import com.irontechspace.dynamicdq.model.ConfigTable;
import com.irontechspace.dynamicdq.model.Db.Field;
import com.irontechspace.dynamicdq.utils.SqlUtils;
import com.irontechspace.dynamicdq.model.TypeSql;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.*;

import static com.irontechspace.dynamicdq.utils.SqlUtils.generateSql;

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
            "   where b.config_id = :id \n" +
            "   order by position \n";

    private final static String SQL_GET_DB_TABLES = "" +
            "SELECT table_schema || '.' || table_name as table_name \n" +
            "   FROM information_schema.tables \n" +
            "   WHERE table_type = 'BASE TABLE' and table_schema not in ('pg_catalog', 'information_schema') \n" +
            "   ORDER BY table_schema, table_name;";

    private final static String SQL_GET_DB_FIELDS_BY_TABLE = "" +
            "select lower(substring(pascal_case,1,1)) || substring(pascal_case,2) as column_name, column_name as raw_column_name, data_type \n" +
            "from (\n" +
            "   SELECT column_name, data_type, replace(initcap(replace(column_name, '_', ' ')), ' ', '') as pascal_case \n" +
            "   FROM information_schema.\"columns\" \n" +
            "   where (table_schema|| '.' || table_name) = :tableName \n" +
            "   order by ordinal_position " +
            ") as sub";

    @Value("${dynamicdq.get.configs}")
    private String tablesTableName;

    @Value("${dynamicdq.get.params}")
    private String fieldsTableName;


    private static final RowMapper<ConfigTable> ROW_MAPPER_TABLES = BeanPropertyRowMapper.newInstance(ConfigTable.class);
    private static final RowMapper<ConfigField> ROW_MAPPER_FIELDS = BeanPropertyRowMapper.newInstance(ConfigField.class);
    private static final RowMapper<Field> ROW_MAPPER_DB_FIELDS = BeanPropertyRowMapper.newInstance(Field.class);


    @Autowired
    NamedParameterJdbcTemplate namedParameterJdbcTemplate;

//    @Autowired
//    JdbcTemplate jdbcTemplate;

    @Autowired
    DataSource dataSource;

    private List<String> excludeFieldsNames = Arrays.asList("id", "fields");

    /** Получить все конфигурации */
    public List<ConfigTable> getConfigs(UUID userId, List<String> userRoles){

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("userId", Collections.singletonList(userId));
        params.addValue("userRoles", userRoles);

        String where = " where user_id = :userId " +
                " or ARRAY(SELECT json_array_elements_text((shared_for_users)::json)) && array[ :userId ]::text[] " +
                " or ARRAY(SELECT json_array_elements_text((shared_for_roles)::json)) && array[ :userRoles ]::text[] ";

        String tablesSql = String.format(SQL_GET_TABLES, tablesTableName, where);
        List<ConfigTable> tables = namedParameterJdbcTemplate.query(tablesSql, params, ROW_MAPPER_TABLES);

        for(ConfigTable table : tables){
            List<ConfigField> fields = namedParameterJdbcTemplate.query(String.format(SQL_GET_FIELDS, fieldsTableName), new MapSqlParameterSource("id", table.getId()), ROW_MAPPER_FIELDS);
            table.setFields(fields);
        }

        return tables;
    }

    /** Получить конфигурацию по наименованию и id пользователя */
    public ConfigTable getConfig(String configName, UUID userId, List<String> userRoles){

        String configSql = String.format(SQL_GET_TABLES, tablesTableName, " where config_name = :configName ");
        List<ConfigTable> configs = namedParameterJdbcTemplate.query(configSql, new MapSqlParameterSource("configName", configName), ROW_MAPPER_TABLES);

        if(configs.size() == 0)
            throw new NotFoundException("Конфигурация не найдена");

        // ARRAY(SELECT json_array_elements_text(({0})::json)) && array[ :userRoles ]::text[]

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("configName", configName);
        params.addValue("userId", Collections.singletonList(userId));
        params.addValue("userRoles", userRoles);

        String where = " where config_name = :configName " +
                " and ( user_id = :userId " +
                " or ARRAY(SELECT json_array_elements_text((shared_for_users)::json)) && array[ :userId ]::text[] " +
                " or ARRAY(SELECT json_array_elements_text((shared_for_roles)::json)) && array[ :userRoles ]::text[]) ";

        String tablesSql = String.format(SQL_GET_TABLES, tablesTableName, where);
        List<ConfigTable> tables = namedParameterJdbcTemplate.query(tablesSql, params, ROW_MAPPER_TABLES);

        if(tables.size() == 0)
            throw new ForbiddenException("Конфигурация недоступна");
        else {
            List<ConfigField> fields = namedParameterJdbcTemplate.query(String.format(SQL_GET_FIELDS, fieldsTableName), new MapSqlParameterSource("id", tables.get(0).getId()), ROW_MAPPER_FIELDS);
            tables.get(0).setFields(fields);
            return tables.get(0);
        }
    }

    /** Сохранить полную конфигурацию */
    @Transactional
    public ConfigTable save(ConfigTable configTable){

        String table_sql;

        if(configTable.getUserId() == null)
            configTable.setUserId(UUID.fromString("0be7f31d-3320-43db-91a5-3c44c99329ab"));

        // Save
        if(configTable.getId() == null){
            table_sql = SqlUtils.generateSql(TypeSql.INSERT, tablesTableName, ConfigTable.class, excludeFieldsNames);
            Map<String, Object> tableParams = SqlUtils.getParams(configTable);
            log.info("table_sql (new generateSql) => \n {}", table_sql);
            configTable.setId(namedParameterJdbcTemplate.queryForObject(table_sql, tableParams, UUID.class));
        }
        // Update
        else{
            table_sql = SqlUtils.generateSql(TypeSql.UPDATE, tablesTableName, ConfigTable.class, excludeFieldsNames);
            Map<String, Object> tableParams = SqlUtils.getParams(configTable);
            log.info("table_sql (new generateSql) => \n {}", table_sql);
            namedParameterJdbcTemplate.update(table_sql, tableParams);
            deleteFieldsByTableId(configTable.getId());
        }

        for(ConfigField field : configTable.getFields()){
            field.setConfigId(configTable.getId());
            String field_sql = SqlUtils.generateSql(TypeSql.INSERT, fieldsTableName, ConfigField.class, excludeFieldsNames);
            Map<String, Object> params = SqlUtils.getParams(field);
            log.info("field_sql (new generateSql) => \n {}", field_sql);
            namedParameterJdbcTemplate.queryForObject(field_sql, params, UUID.class);
        }

        return configTable;
    }

    @Transactional
    public void deleteTableById(UUID tableId){
        deleteFieldsByTableId(tableId);
        namedParameterJdbcTemplate.update("DELETE FROM " + tablesTableName + " WHERE id=:id ", new MapSqlParameterSource("id", tableId));
    }

    public void deleteFieldById(UUID fieldId){
        namedParameterJdbcTemplate.update("DELETE FROM " + fieldsTableName + " WHERE id=:id ", new MapSqlParameterSource("id", fieldId));
    }

    public void deleteFieldsByTableId(UUID configId){
        namedParameterJdbcTemplate.update("DELETE FROM " + fieldsTableName + " WHERE config_id=:id ", new MapSqlParameterSource("id", configId));
    }

    public List<String> getDbTables() {
        return namedParameterJdbcTemplate.queryForList(SQL_GET_DB_TABLES, new HashMap<>(), String.class);
    }

    public List<Field> getDbFieldsByTable(String tableName) {
        return namedParameterJdbcTemplate.query(SQL_GET_DB_FIELDS_BY_TABLE, new MapSqlParameterSource("tableName", tableName), ROW_MAPPER_DB_FIELDS);
    }
}
