package com.irontechspace.dynamicdq.repository;

import com.irontechspace.dynamicdq.repository.RowMapper.DataRowMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class QueryRepository {

    @Autowired
    NamedParameterJdbcTemplate jdbcTemplate;

    public <T> List<T> getTable(String sql, Map<String, Object> params, Class<T> clazz){
        return jdbcTemplate.queryForList(sql, params, clazz);
    }

    public List<ObjectNode> getTable(String sql, MapSqlParameterSource params, DataRowMapper rowMapper){
        return jdbcTemplate.query(sql, params, rowMapper);
    }

    public <T> T getCount(String sql, MapSqlParameterSource params, Class<T> clazz){
        return jdbcTemplate.queryForObject(sql, params, clazz);
    }

    public <T> T findByPK(String sql, Map<String, Object> params,  Class<T> clazz){
        List <T> result = jdbcTemplate.query(sql, params, BeanPropertyRowMapper.newInstance(clazz));
        if(result.size() == 0) return null;
        else return result.get(0);
    }

    public <T> T insert(String sql, Map<String, Object> params, Class<T> clazz){
        return jdbcTemplate.queryForObject(sql, params, clazz);
    }

    public <T> void update(String sql, Map<String, Object> params){
        jdbcTemplate.update(sql, params);
    }

    public <T> void delete(String sql, Map<String, Object> params){
        jdbcTemplate.update(sql, params);
    }
}
