package com.irontechspace.dynamicdq.repository;

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
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Log4j2
@Repository
public class SaveConfigRepository extends ConfigRepository implements IConfigRepository<SaveConfig> {

    @Value("${dynamicdq.save.configs}")
    private String saveTableName;

    private final static String SELECT_SAVE_CONFIGS = "select * from %s a order by position";

    private static final RowMapper<SaveConfig> ROW_MAPPER_SAVE_CONFIGS = BeanPropertyRowMapper.newInstance(SaveConfig.class);
    private static final List<String> EXCLUDE_FIELDS_NAMES = Collections.singletonList("id");

    /** Получить все конфигурации */
    @Override
    public List<SaveConfig> getAll(){
        return jdbcTemplate.query(String.format(SELECT_SAVE_CONFIGS, saveTableName), new HashMap<>(), ROW_MAPPER_SAVE_CONFIGS);
    }

    /** Сохранить конфигурацию сохранения */
    @Override
    @Transactional
    public SaveConfig save(SaveConfig saveConfig){

        String operation;
        String sql;
        Map<String, Object> params = SqlUtils.getParams(saveConfig);


        if(saveConfig.getUserId() == null)
            saveConfig.setUserId(UUID.fromString("0be7f31d-3320-43db-91a5-3c44c99329ab"));

        // Save
        if(saveConfig.getId() == null){
            operation = "INSERT";
            sql = SqlUtils.generateSql(TypeSql.INSERT, saveTableName, SaveConfig.class, EXCLUDE_FIELDS_NAMES);
            log.info("\n DATA: [{}]\n SQL: [{}]\n", params.toString(), sql);
            jdbcTemplate.queryForObject(sql, params, UUID.class);
        }
        // Update
        else{
            operation = "UPDATE";
            sql = SqlUtils.generateSql(TypeSql.UPDATE, saveTableName, SaveConfig.class, EXCLUDE_FIELDS_NAMES);
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
        jdbcTemplate.update("DELETE FROM " + saveTableName + " WHERE id=:id ", new MapSqlParameterSource("id", configId));
    }
}
