package com.irontechspace.dynamicdq.quartz;

import com.irontechspace.dynamicdq.executor.events.SystemEventType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class QuartzRepository {
    private final static String SELECT_SYSTEM_EVENT_TYPES = "select id, name, template from dynamicdq.log_system_event_types;";
    private final static String INSERT_SYSTEM_EVENT = "insert into dynamicdq.log_system_events (type_id, user_id, data) values (:typeId, :userId, :data) returning id;";

    private static final RowMapper<SystemEventType> ROW_MAPPER_SYSTEM_EVENT_TYPE = BeanPropertyRowMapper.newInstance(SystemEventType.class);

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    public List<SystemEventType> getAll(){
        return jdbcTemplate.query(SELECT_SYSTEM_EVENT_TYPES, new HashMap<>(), ROW_MAPPER_SYSTEM_EVENT_TYPE);
    }

    public UUID insert(UUID userId, UUID typeId, String data){
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("userId", userId);
        params.addValue("typeId", typeId);
        params.addValue("data", data);
        return jdbcTemplate.queryForObject(INSERT_SYSTEM_EVENT, params, UUID.class);
    }
}
