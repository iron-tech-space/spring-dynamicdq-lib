package com.irontechspace.dynamicdq.repository;

import com.irontechspace.dynamicdq.model.Config;
import com.irontechspace.dynamicdq.model.Query.QueryConfig;
import com.irontechspace.dynamicdq.repository.Base.ConfigRepository;
import com.irontechspace.dynamicdq.repository.Base.IConfigRepository;
import com.irontechspace.dynamicdq.repository.RowMapper.QueryConfigRowMapper;
import com.irontechspace.dynamicdq.utils.SqlUtils;
import com.irontechspace.dynamicdq.model.TypeSql;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
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

    private static final RowMapper<QueryConfig> ROW_MAPPER_TABLES = new QueryConfigRowMapper(); // BeanPropertyRowMapper.newInstance(QueryConfig.class); // new QueryConfigRowMapper();
    private static final List<String> EXCLUDE_FIELDS_NAMES = Collections.singletonList("id");

    @PostConstruct
    void init() {
        log.info("Config param [tableNameQueryConfig]: [{}]", tableNameQueryConfig);
        log.info("Config param [tableNameQueryField]: [{}]", tableNameQueryField);
    }

    /** Получить все конфигурации */
    @Override
    public List<QueryConfig> getAll(){
        return getAll(jdbcTemplate);
    }

    @Override
    public List<QueryConfig> getAll(NamedParameterJdbcTemplate jdbcTemplate){
        String tablesSql = String.format(SELECT_QUERY_CONFIGS, tableNameQueryConfig);
        return jdbcTemplate.query(tablesSql, new HashMap<>(), ROW_MAPPER_TABLES);
    }
    @Override
    public void savePosition(QueryConfig config){
        savePosition(jdbcTemplate, config, tableNameQueryConfig);
    }

    @Override
    public void savePosition(NamedParameterJdbcTemplate jdbcTemplate, QueryConfig config){
        savePosition(jdbcTemplate, config, tableNameQueryConfig);
    }

    @Override
    public QueryConfig save(QueryConfig queryConfig){
        return save(jdbcTemplate, queryConfig);
    }

    /** Сохранить полную конфигурацию */
    @Override
    @Transactional
    public QueryConfig save(NamedParameterJdbcTemplate jdbcTemplate, QueryConfig queryConfig){

        String operation;
        String table_sql;

        if(queryConfig.getUserId() == null)
            queryConfig.setUserId(UUID.fromString("0be7f31d-3320-43db-91a5-3c44c99329ab"));

        // INSERT
        if(queryConfig.getId() == null){
            operation = "INSERT";
            table_sql = SqlUtils.generateSql(TypeSql.INSERT, tableNameQueryConfig, QueryConfig.class, EXCLUDE_FIELDS_NAMES);
            Map<String, Object> tableParams = SqlUtils.getParams(queryConfig);
            tableParams.put("fields", getJsonFromObject(queryConfig.getFields()));
            log.debug("\n DATA: [{}]\n SQL: [{}]\n", tableParams.toString(), table_sql);
            queryConfig.setId(jdbcTemplate.queryForObject(table_sql, tableParams, UUID.class));
        }
        // UPDATE
        else{
            operation = "UPDATE";
            table_sql = SqlUtils.generateSql(TypeSql.UPDATE, tableNameQueryConfig, QueryConfig.class, EXCLUDE_FIELDS_NAMES);
            Map<String, Object> tableParams = SqlUtils.getParams(queryConfig);
            tableParams.put("fields", getJsonFromObject(queryConfig.getFields()));
            log.debug("\n DATA: [{}]\n SQL: [{}]\n", tableParams.toString(), table_sql);
            jdbcTemplate.update(table_sql, tableParams);
        }

        logHistory(queryConfig, operation, "QUERY");
        return queryConfig;
    }

    @Override
    public void delete(UUID tableId){
        delete(jdbcTemplate, tableId);
    }

    @Override
    @Transactional
    public void delete(NamedParameterJdbcTemplate jdbcTemplate, UUID tableId){
        jdbcTemplate.update("DELETE FROM " + tableNameQueryConfig + " WHERE id=:id ", new MapSqlParameterSource("id", tableId));
    }
}
