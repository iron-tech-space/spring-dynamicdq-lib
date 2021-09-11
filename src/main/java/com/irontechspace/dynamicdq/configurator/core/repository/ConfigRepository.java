package com.irontechspace.dynamicdq.configurator.core.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.irontechspace.dynamicdq.configurator.core.model.Config;
import com.irontechspace.dynamicdq.configurator.core.ObjectNodeRowMapper;
import com.irontechspace.dynamicdq.configurator.save.model.SaveField;
import com.irontechspace.dynamicdq.configurator.save.model.SaveLogic;
import com.irontechspace.dynamicdq.executor.save.SaveService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Log4j2
public class ConfigRepository {

    protected final static ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    protected final static String SELECT_CONFIGS = "" +
            "WITH RECURSIVE r AS ( \n" +
            "     (SELECT a.* FROM %s a WHERE a.parent_id IS NULL order by a.position) \n" +
            "     UNION ALL \n" +
            "     (SELECT a.* FROM %s a JOIN r ON a.parent_id = r.id order by a.parent_id, a.position) \n" +
            " ) SELECT a.* FROM r a ";

    private final static RowMapper<ObjectNode> OBJECT_NODE_ROW_MAPPER = new ObjectNodeRowMapper();
    private final static String INSERT_HISTORY =
            "INSERT INTO dynamicdq.configs_history (id, ts, operation, config_type, column_data) " +
                    "VALUES (:id, current_timestamp, :operation, :configType, :columnData) returning id;";

    private String UPDATE_POS =
            "update :tableName " +
                    " set position = :position, \n" +
                    "     parent_id = :parentId \n" +
                    " where id = :id " +
                    " returning id; ";

    private final static String SQL_GET_DB_TABLES = "" +
            "SELECT table_schema || '.' || table_name as table_name \n" +
            "   FROM information_schema.tables \n" +
            "   WHERE table_schema not in ('pg_catalog', 'information_schema') \n" +
            "   ORDER BY table_schema, table_name;";

    private final static String SQL_GET_DB_FIELDS_BY_TABLE = "" +
            "select lower(substring(pascal_case,1,1)) || substring(pascal_case,2) as column_name, column_name as raw_column_name, data_type \n" +
            "from (\n" +
            "   SELECT column_name, data_type, replace(initcap(replace(column_name, '_', ' ')), ' ', '') as pascal_case \n" +
            "   FROM information_schema.columns \n" +
            "   where (table_schema|| '.' || table_name) = :tableName \n" +
            "   order by ordinal_position " +
            ") as sub";

    @Autowired
    public NamedParameterJdbcTemplate jdbcTemplate;

    public <T extends Config> void logHistory(NamedParameterJdbcTemplate jdbcTemplate, T queryConfig, String operation, String configType) {

        //:id, current_timestamp, :operation, :configType, :columnData
        try {
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue("id", queryConfig.getId());
            params.addValue("operation", operation);
            params.addValue("configType", configType);
            params.addValue( "columnData", OBJECT_MAPPER.writeValueAsString(queryConfig));
            jdbcTemplate.queryForObject(INSERT_HISTORY, params, UUID.class);
        } catch (JsonProcessingException ex) {
//            ex.printStackTrace();
            log.error(ex);
        }
    }

    public <T extends Config> void savePosition(NamedParameterJdbcTemplate jdbcTemplate, T config, String tableName) {
        MapSqlParameterSource params = new MapSqlParameterSource();
//        params.addValue("tableName", tableName);
        params.addValue("id", config.getId());
        params.addValue("position", config.getPosition());
        params.addValue("parentId", config.getParentId());
        jdbcTemplate.queryForObject(UPDATE_POS.replace(":tableName", tableName), params, UUID.class);
    }

    public String getJsonFromObject (Object o){
        try {
            return OBJECT_MAPPER.writeValueAsString(o);
        } catch (JsonProcessingException ignored){
            return null;
        }
    }

    public List<String> getDbTables(NamedParameterJdbcTemplate jdbcTemplate) {
        return jdbcTemplate.queryForList(SQL_GET_DB_TABLES, new HashMap<>(), String.class);
    }

    public List<ObjectNode> getDbFieldsByTable(NamedParameterJdbcTemplate jdbcTemplate, String tableName) {
        return jdbcTemplate.query(SQL_GET_DB_FIELDS_BY_TABLE, new MapSqlParameterSource("tableName", tableName), OBJECT_NODE_ROW_MAPPER);
    }
}
