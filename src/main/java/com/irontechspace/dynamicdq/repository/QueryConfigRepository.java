package com.irontechspace.dynamicdq.repository;

import com.irontechspace.dynamicdq.model.Query.QueryField;
import com.irontechspace.dynamicdq.model.Query.QueryConfig;
import com.irontechspace.dynamicdq.repository.Base.ConfigRepository;
import com.irontechspace.dynamicdq.repository.Base.IConfigRepository;
import com.irontechspace.dynamicdq.utils.SqlUtils;
import com.irontechspace.dynamicdq.model.TypeSql;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Log4j2
@Repository
public class QueryConfigRepository extends ConfigRepository implements IConfigRepository<QueryConfig> {

    // Наименование таблицы с конфигурациями запросов
    @Value("${dynamicdq.get.configs}")
    private String tableNameQueryConfig;

    // Наименование таблицы с полями конфигурации запросов
    @Value("${dynamicdq.get.params}")
    private String tableNameQueryField;

    private final static String SELECT_QUERY_CONFIGS = "select * from %s a order by position";
    private final static String SELECT_QUERY_FIELDS =  "select * from %s a where a.config_id = :id order by position";

    private static final RowMapper<QueryConfig> ROW_MAPPER_TABLES = BeanPropertyRowMapper.newInstance(QueryConfig.class); // new QueryConfigRowMapper();
    private static final RowMapper<QueryField> ROW_MAPPER_FIELDS = BeanPropertyRowMapper.newInstance(QueryField.class);
    private static final List<String> EXCLUDE_FIELDS_NAMES = Arrays.asList("id", "fields");

    /** Получить все конфигурации */
    @Override
    public List<QueryConfig> getAll(){

        String tablesSql = String.format(SELECT_QUERY_CONFIGS, tableNameQueryConfig);
        List<QueryConfig> tables = jdbcTemplate.query(tablesSql, new HashMap<>(), ROW_MAPPER_TABLES);

        for(QueryConfig table : tables){
            List<QueryField> queryFields = jdbcTemplate.query(String.format(SELECT_QUERY_FIELDS, tableNameQueryField), new MapSqlParameterSource("id", table.getId()), ROW_MAPPER_FIELDS);
            table.setFields(queryFields);
        }

        return tables;
    }

    /** Сохранить полную конфигурацию */
    @Override
    @Transactional
    public QueryConfig save(QueryConfig queryConfig){

        String operation;
        String table_sql;

        if(queryConfig.getUserId() == null)
            queryConfig.setUserId(UUID.fromString("0be7f31d-3320-43db-91a5-3c44c99329ab"));

        // Insert
        if(queryConfig.getId() == null){
            operation = "INSERT";
            table_sql = SqlUtils.generateSql(TypeSql.INSERT, tableNameQueryConfig, QueryConfig.class, EXCLUDE_FIELDS_NAMES);
            Map<String, Object> tableParams = SqlUtils.getParams(queryConfig);
            log.info("table_sql (new generateSql) => \n {}", table_sql);
            queryConfig.setId(jdbcTemplate.queryForObject(table_sql, tableParams, UUID.class));
        }
        // Update
        else{
            operation = "UPDATE";
            table_sql = SqlUtils.generateSql(TypeSql.UPDATE, tableNameQueryConfig, QueryConfig.class, EXCLUDE_FIELDS_NAMES);
            Map<String, Object> tableParams = SqlUtils.getParams(queryConfig);
            log.info("table_sql (new generateSql) => \n {}", table_sql);
            jdbcTemplate.update(table_sql, tableParams);
            deleteFieldsByTableId(queryConfig.getId());
        }

        for(QueryField queryField : queryConfig.getFields()){
            queryField.setConfigId(queryConfig.getId());
            String field_sql = SqlUtils.generateSql(TypeSql.INSERT, tableNameQueryField, QueryField.class, EXCLUDE_FIELDS_NAMES);
            Map<String, Object> params = SqlUtils.getParams(queryField);
            log.info("field_sql (new generateSql) => \n {}", field_sql);
            queryField.setId(jdbcTemplate.queryForObject(field_sql, params, UUID.class));
        }
        logHistory(queryConfig, operation, "QUERY");
        return queryConfig;
    }

    @Override
    @Transactional
    public void delete(UUID tableId){
        deleteFieldsByTableId(tableId);
        jdbcTemplate.update("DELETE FROM " + tableNameQueryConfig + " WHERE id=:id ", new MapSqlParameterSource("id", tableId));
    }

    private void deleteFieldsByTableId(UUID configId){
        jdbcTemplate.update("DELETE FROM " + tableNameQueryField + " WHERE config_id=:id ", new MapSqlParameterSource("id", configId));
    }
}
