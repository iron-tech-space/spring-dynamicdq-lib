package com.irontechspace.dynamicdq.repository.Base;

import com.irontechspace.dynamicdq.model.Query.QueryConfig;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.List;
import java.util.UUID;

public interface IConfigRepository<T> {

    List<T> getAll();

    List<T> getAll(NamedParameterJdbcTemplate jdbcTemplate);

    void savePosition(T config);

    void savePosition(NamedParameterJdbcTemplate jdbcTemplate, T config);

    T save(T config);

    T save(NamedParameterJdbcTemplate jdbcTemplate, T config);

    void delete(UUID configId);

    void delete(NamedParameterJdbcTemplate jdbcTemplate, UUID configId);
}
