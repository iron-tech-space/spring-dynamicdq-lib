package com.irontechspace.dynamicdq.repository;

import com.irontechspace.dynamicdq.repository.RowMapper.DataRowMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Log4j2
@Repository
public class QueryRepository {

    @Autowired
    NamedParameterJdbcTemplate jdbcTemplate;

    public <T> List<T> selectTable(String sql, Map<String, Object> params, Class<T> clazz){
        return selectTable(jdbcTemplate, sql, params, clazz);
    }

    public <T> List<T> selectTable(NamedParameterJdbcTemplate jdbcTemplate, String sql, Map<String, Object> params, Class<T> clazz){
        return jdbcTemplate.queryForList(sql, params, clazz);
    }

    public List<ObjectNode> selectTable(String sql, MapSqlParameterSource params, DataRowMapper rowMapper){
        return selectTable(jdbcTemplate, sql, params, rowMapper);
    }

    public List<ObjectNode> selectTable(NamedParameterJdbcTemplate jdbcTemplate, String sql, MapSqlParameterSource params, DataRowMapper rowMapper){
        return jdbcTemplate.query(sql, params, rowMapper);
    }

    public <T> T selectCount(String sql, MapSqlParameterSource params, Class<T> clazz){
        return selectCount(jdbcTemplate, sql, params, clazz);
    }

    public <T> T selectCount(NamedParameterJdbcTemplate jdbcTemplate, String sql, MapSqlParameterSource params, Class<T> clazz){
        return jdbcTemplate.queryForObject(sql, params, clazz);
    }

    public <T> T selectObject(String sql, Map<String, Object> params, Class<T> clazz){
        return selectObject(jdbcTemplate, sql, params, clazz);
    }

    public <T> T selectObject(NamedParameterJdbcTemplate jdbcTemplate, String sql, Map<String, Object> params, Class<T> clazz){
        List <T> result = jdbcTemplate.query(sql, params, BeanPropertyRowMapper.newInstance(clazz));
        if(result.size() == 0) {
            log.error("\n DATA: [{}]\n SQL: [{}]\n", params.toString(), sql);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Объект не найден");
        } else if(result.size() > 1) {
            log.error("\n DATA: [{}]\n SQL: [{}]\n", params.toString(), sql);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "По заданным параметрам найдено слишком много объектов");
        } else {
            return result.get(0);
        }
    }

    public <T> T insert(String sql, Map<String, Object> params, Class<T> clazz){
        return insert(jdbcTemplate, sql, params, clazz);
    }

    public <T> T insert(NamedParameterJdbcTemplate jdbcTemplate, String sql, Map<String, Object> params, Class<T> clazz){
        return jdbcTemplate.queryForObject(sql, params, clazz);
    }

    public <T> void update(String sql, Map<String, Object> params){
        update(jdbcTemplate, sql, params);
    }

    public <T> void update(NamedParameterJdbcTemplate jdbcTemplate, String sql, Map<String, Object> params){
        jdbcTemplate.update(sql, params);
    }

    public <T> void delete(String sql, Map<String, Object> params){
        delete(jdbcTemplate, sql, params);
    }

    public <T> void delete(NamedParameterJdbcTemplate jdbcTemplate, String sql, Map<String, Object> params){
        jdbcTemplate.update(sql, params);
    }
}
