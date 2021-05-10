package com.irontechspace.dynamicdq.repository.Base;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.irontechspace.dynamicdq.model.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.UUID;

public class ConfigRepository {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final static String INSERT_HISTORY =
            "INSERT INTO dynamicdq.configs_history (id, ts, operation, config_type, column_data) " +
                    "VALUES (:id, current_timestamp, :operation, :configType, :columnData) returning id;";

    @Autowired
    public NamedParameterJdbcTemplate jdbcTemplate;

    public <T extends Config> void logHistory(T queryConfig, String operation, String configType) {

        //:id, current_timestamp, :operation, :configType, :columnData
        try {
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue("id", queryConfig.getId());
            params.addValue("operation", operation);
            params.addValue("configType", configType);
            params.addValue( "columnData", objectMapper.writeValueAsString(queryConfig));
            jdbcTemplate.queryForObject(INSERT_HISTORY, params, UUID.class);
        } catch (JsonProcessingException ex) {
            ex.printStackTrace();
        }
    }
}
