package com.irontechspace.dynamicdq.repository;

import com.irontechspace.dynamicdq.repository.RowMapper.DataRowMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class DataRepository {

    @Autowired
    NamedParameterJdbcTemplate jdbcTemplate;

    public List<ObjectNode> getTable(String sql, MapSqlParameterSource params, DataRowMapper rowMapper){
        return jdbcTemplate.query(sql, params, rowMapper);
    }

    public <T> T getCount(String sql, MapSqlParameterSource params, Class<T> clazz){
        return jdbcTemplate.queryForObject(sql, params, clazz);
    }
}
