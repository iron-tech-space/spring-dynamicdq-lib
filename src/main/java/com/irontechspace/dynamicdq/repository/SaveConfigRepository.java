package com.irontechspace.dynamicdq.repository;

import com.irontechspace.dynamicdq.model.SaveData.SaveConfig;
import com.irontechspace.dynamicdq.utils.SqlUtils;
import com.irontechspace.dynamicdq.model.TypeSql;
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

import static com.irontechspace.dynamicdq.utils.SqlUtils.generateSql;

@Log4j2
@Repository
public class SaveConfigRepository {

    private final static String SQL_GET_SAVE_CONFIGS = "" +
            "SELECT * \n" +
            "   FROM %s a \n" +
            "   %s \n" +
            "   order by position ";

    @Value("${dynamicdq.save.configs}")
    private String saveTableName;

    private static final RowMapper<SaveConfig> ROW_MAPPER_SAVE_CONFIGS = BeanPropertyRowMapper.newInstance(SaveConfig.class);


    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    private List<String> excludeFieldsNames = Collections.singletonList("id");

    /** Получить все конфигурации */
    public List<SaveConfig> getConfigs(UUID userId, List<String> userRoles){
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("userId", Collections.singletonList(userId));
        params.addValue("userRoles", userRoles);


        //        String whered = " where user_id = " + userId + " or shared_for_users::jsonb @> '[" + userId + "]' or ARRAY(SELECT json_array_elements_text((shared_for_roles)::json)) && array[ :userRoles ]::text[]";

        String where = " where user_id = :userId " +
                " or ARRAY(SELECT json_array_elements_text((shared_for_users)::json)) && array[ :userId ]::text[]" +
                " or ARRAY(SELECT json_array_elements_text((shared_for_roles)::json)) && array[ :userRoles ]::text[] ";
        List<SaveConfig> configs = jdbcTemplate.query(String.format(SQL_GET_SAVE_CONFIGS, saveTableName, where), params, ROW_MAPPER_SAVE_CONFIGS);

        return configs;
    }

    /** Получить конфигурацию по наименованию и id пользователя */
    public SaveConfig getConfig(String configName, UUID userId, List<String> userRoles){

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("configName", configName);
        params.addValue("userId", Collections.singletonList(userId));
        params.addValue("userRoles", userRoles);

        String where = " where config_name = :configName and " +
                " (user_id = :userId " +
                " or ARRAY(SELECT json_array_elements_text((shared_for_users)::json)) && array[ :userId ]::text[]" +
                " or ARRAY(SELECT json_array_elements_text((shared_for_roles)::json)) && array[ :userRoles ]::text[])";

        String tablesSql = String.format(SQL_GET_SAVE_CONFIGS, saveTableName, where);

        List<SaveConfig> configs = jdbcTemplate.query(tablesSql, params, ROW_MAPPER_SAVE_CONFIGS);

        if(configs.size() == 0) {
            params.addValue("userId", Collections.singletonList(UUID.fromString("0be7f31d-3320-43db-91a5-3c44c99329ab")));
            configs = jdbcTemplate.query(tablesSql, params, ROW_MAPPER_SAVE_CONFIGS);
        }

        if(configs.size() != 0) {
            return configs.get(0);
        } else {
            return null;
        }
    }

    /** Сохранить конфигурацию сохранения */
    @Transactional
    public void save(SaveConfig saveConfig){

        String sql;
        Map<String, Object> params = SqlUtils.getParams(saveConfig);


        if(saveConfig.getUserId() == null)
            saveConfig.setUserId(UUID.fromString("0be7f31d-3320-43db-91a5-3c44c99329ab"));

        // Save
        if(saveConfig.getId() == null){
            sql = SqlUtils.generateSql(TypeSql.INSERT, saveTableName, SaveConfig.class, excludeFieldsNames);
            log.info("\n DATA: [{}]\n SQL: [{}]\n", params.toString(), sql);
            jdbcTemplate.queryForObject(sql, params, UUID.class);
        }
        // Update
        else{
            sql = SqlUtils.generateSql(TypeSql.UPDATE, saveTableName, SaveConfig.class, excludeFieldsNames);
            log.info("\n DATA: [{}]\n SQL: [{}]\n", params.toString(), sql);
            jdbcTemplate.update(sql, params);
        }
    }

    /** Удалить конфигурацию сохранения */
    @Transactional
    public void deleteSaveConfigById(UUID configId){
        jdbcTemplate.update("DELETE FROM " + saveTableName + " WHERE id=:id ", new MapSqlParameterSource("id", configId));
    }
}
