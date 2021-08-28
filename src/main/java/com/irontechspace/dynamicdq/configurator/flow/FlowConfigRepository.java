package com.irontechspace.dynamicdq.configurator.flow;

import com.irontechspace.dynamicdq.configurator.core.model.TypeSql;
import com.irontechspace.dynamicdq.configurator.core.repository.ConfigRepository;
import com.irontechspace.dynamicdq.configurator.core.repository.IConfigRepository;
import com.irontechspace.dynamicdq.configurator.flow.model.FlowConfig;
import com.irontechspace.dynamicdq.utils.SqlUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.*;

@Log4j2
@Repository
public class FlowConfigRepository extends ConfigRepository implements IConfigRepository<FlowConfig> {

    // Наименование таблицы с конфигурациями сохранения
    @Value("${dynamicdq.flow.configs}")
    private String tableNameFlowConfigs;

//    private final static String SELECT_FLOW_CONFIGS = "select * from %s a order by position";

    private static final RowMapper<FlowConfig> ROW_MAPPER_FLOW_CONFIGS = BeanPropertyRowMapper.newInstance(FlowConfig.class);
    private static final List<String> EXCLUDE_FIELDS_NAMES = Collections.singletonList("id");

    @PostConstruct
    void init() {
        log.info("Config param [tableNameFlowConfigs]: [{}]", tableNameFlowConfigs);
    }

    /** Получить все конфигурации */
    @Override
    public List<FlowConfig> getAll(){
        return getAll(jdbcTemplate);
    }

    /** Получить все конфигурации */
    @Override
    public List<FlowConfig> getAll(NamedParameterJdbcTemplate jdbcTemplate){
        return jdbcTemplate.query(String.format(SELECT_CONFIGS, tableNameFlowConfigs, tableNameFlowConfigs), new HashMap<>(), ROW_MAPPER_FLOW_CONFIGS);
    }
    @Override
    public void savePosition(FlowConfig config){
        savePosition(jdbcTemplate, config, tableNameFlowConfigs);
    }

    @Override
    public void savePosition(NamedParameterJdbcTemplate jdbcTemplate, FlowConfig config){
        savePosition(jdbcTemplate, config, tableNameFlowConfigs);
    }

    /** Сохранить конфигурацию сохранения */
    @Override
    public FlowConfig save(FlowConfig config){
        return save(jdbcTemplate, config);
    }

    /** Сохранить конфигурацию сохранения */
    @Override
    @Transactional
    public FlowConfig save(NamedParameterJdbcTemplate jdbcTemplate, FlowConfig config){

        String operation;
        String sql;
        Map<String, Object> params = SqlUtils.getParams(config);


        if(config.getUserId() == null)
            config.setUserId(UUID.fromString("0be7f31d-3320-43db-91a5-3c44c99329ab"));

        // Save
        if(config.getId() == null){
            operation = "INSERT";
            sql = SqlUtils.generateSql(TypeSql.INSERT, tableNameFlowConfigs, FlowConfig.class, EXCLUDE_FIELDS_NAMES);
            log.info("\n DATA: [{}]\n SQL: [{}]\n", params.toString(), sql);
            config.setId(jdbcTemplate.queryForObject(sql, params, UUID.class));
        }
        // Update
        else{
            operation = "UPDATE";
            sql = SqlUtils.generateSql(TypeSql.UPDATE, tableNameFlowConfigs, FlowConfig.class, EXCLUDE_FIELDS_NAMES);
            log.info("\n DATA: [{}]\n SQL: [{}]\n", params.toString(), sql);
            jdbcTemplate.update(sql, params);
        }
        logHistory(jdbcTemplate, config, operation, "FLOW");
        return config;
    }

    /** Удалить конфигурацию сохранения */
    @Override
    @Transactional
    public void delete(UUID configId){
        delete(jdbcTemplate, configId);
    }

    /** Удалить конфигурацию сохранения */
    @Override
    @Transactional
    public void delete(NamedParameterJdbcTemplate jdbcTemplate, UUID configId){
        jdbcTemplate.update("DELETE FROM " + tableNameFlowConfigs + " WHERE id=:id ", new MapSqlParameterSource("id", configId));
    }
}