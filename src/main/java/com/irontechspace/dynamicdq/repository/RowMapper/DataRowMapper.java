package com.irontechspace.dynamicdq.repository.RowMapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.irontechspace.dynamicdq.model.ConfigField;
import com.irontechspace.dynamicdq.model.ConfigTable;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.log4j.Log4j2;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Log4j2
public class DataRowMapper implements RowMapper<ObjectNode> {

    private final ConfigTable configTable;

    private final List<String> whiteListNames; //  = Arrays.asList("id", "parent_id");

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");

    public DataRowMapper(ConfigTable configTable){
        this.configTable = configTable;
        whiteListNames = configTable.getHierarchyField() != null ? Arrays.asList(configTable.getHierarchyField().split("/")) : new ArrayList<>();
    }

    @Override
    public ObjectNode mapRow(ResultSet rs, int counts) throws SQLException {
        ObjectNode rowObject = new ObjectMapper().createObjectNode();
        for(ConfigField field : configTable.getFields()){
            if(whiteListNames.contains(field.getAliasOrName()) || field.getVisible()) {
                String fieldName = field.getAliasOrName();

                switch (field.getTypeData()) {
                    case "uuid":
                    case "text":
                        rowObject.put(fieldName, rs.getString(fieldName));
                        break;
                    case "json":
                        try {
                            String value = rs.getString(fieldName);
                            if(value != null)
                                rowObject.put(fieldName, objectMapper.readTree(value));
                            else
                                rowObject.put(fieldName, objectMapper.nullNode());
                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                            rowObject.put(fieldName, objectMapper.nullNode());
                        }
                        break;
                    case "int":
                        rowObject.put(fieldName, rs.getLong(fieldName));
                        break;
                    case "timestamp":
                        if(rs.getTimestamp(fieldName) != null)
                            rowObject.put(fieldName, dateFormatter.format(rs.getTimestamp(fieldName)));
                        else
                            rowObject.put(fieldName, objectMapper.nullNode());
                        break;
                    case "time":
                        rowObject.put(fieldName,
                                rs.getTime(fieldName) != null ?
                                        rs.getTime(fieldName).toLocalTime().toString() :
                                        null);
                        break;
                    case "bool":
                        rowObject.put(fieldName,
                                rs.getString(fieldName) != null ?
                                        rs.getBoolean(fieldName) :
                                        null);
                        break;
                    case "double":
                        rowObject.put(fieldName, rs.getDouble(fieldName));
                        break;
                    default:
                        rowObject.put(fieldName, rs.getString(fieldName));
                        break;
                }
            }
        }
        return rowObject;
    }

}
