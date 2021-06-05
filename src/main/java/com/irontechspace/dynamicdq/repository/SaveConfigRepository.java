package com.irontechspace.dynamicdq.repository;

import com.irontechspace.dynamicdq.model.Query.QueryConfig;
import com.irontechspace.dynamicdq.model.Save.SaveConfig;
import com.irontechspace.dynamicdq.repository.Base.ConfigRepository;
import com.irontechspace.dynamicdq.repository.Base.IConfigRepository;
import com.irontechspace.dynamicdq.utils.SqlUtils;
import com.irontechspace.dynamicdq.model.TypeSql;
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
public class SaveConfigRepository extends ConfigRepository implements IConfigRepository<SaveConfig> {

    // Наименование таблицы с конфигурациями сохранения
    @Value("${dynamicdq.save.configs}")
    private String tableNameSaveConfigs;

    private final static String SELECT_SAVE_CONFIGS = "select * from %s a order by position";

    private static final RowMapper<SaveConfig> ROW_MAPPER_SAVE_CONFIGS = BeanPropertyRowMapper.newInstance(SaveConfig.class);
    private static final List<String> EXCLUDE_FIELDS_NAMES = Collections.singletonList("id");

    @PostConstruct
    void init() {
        log.info("Config param [tableNameSaveConfigs]: [{}]", tableNameSaveConfigs);
    }

    /** Получить все конфигурации */
    @Override
    public List<SaveConfig> getAll(){
        return getAll(jdbcTemplate);
    }

    /** Получить все конфигурации */
    @Override
    public List<SaveConfig> getAll(NamedParameterJdbcTemplate jdbcTemplate){
        return jdbcTemplate.query(String.format(SELECT_SAVE_CONFIGS, tableNameSaveConfigs), new HashMap<>(), ROW_MAPPER_SAVE_CONFIGS);
    }
    @Override
    public void savePosition(SaveConfig config){
        savePosition(jdbcTemplate, config, tableNameSaveConfigs);
    }

    @Override
    public void savePosition(NamedParameterJdbcTemplate jdbcTemplate, SaveConfig config){
        savePosition(jdbcTemplate, config, tableNameSaveConfigs);
    }

    /** Сохранить конфигурацию сохранения */
    @Override
    public SaveConfig save(SaveConfig queryConfig){
        return save(jdbcTemplate, queryConfig);
    }

    /** Сохранить конфигурацию сохранения */
    @Override
    @Transactional
    public SaveConfig save(NamedParameterJdbcTemplate jdbcTemplate, SaveConfig saveConfig){

        String operation;
        String sql;
        Map<String, Object> params = SqlUtils.getParams(saveConfig);


        if(saveConfig.getUserId() == null)
            saveConfig.setUserId(UUID.fromString("0be7f31d-3320-43db-91a5-3c44c99329ab"));

        // Save
        if(saveConfig.getId() == null){
            operation = "INSERT";
            sql = SqlUtils.generateSql(TypeSql.INSERT, tableNameSaveConfigs, SaveConfig.class, EXCLUDE_FIELDS_NAMES);
            log.info("\n DATA: [{}]\n SQL: [{}]\n", params.toString(), sql);
            jdbcTemplate.queryForObject(sql, params, UUID.class);
        }
        // Update
        else{
            operation = "UPDATE";
            sql = SqlUtils.generateSql(TypeSql.UPDATE, tableNameSaveConfigs, SaveConfig.class, EXCLUDE_FIELDS_NAMES);
            log.info("\n DATA: [{}]\n SQL: [{}]\n", params.toString(), sql);
            jdbcTemplate.update(sql, params);
        }
        logHistory(saveConfig, operation, "SAVE");
        return saveConfig;
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
        jdbcTemplate.update("DELETE FROM " + tableNameSaveConfigs + " WHERE id=:id ", new MapSqlParameterSource("id", configId));
    }
}