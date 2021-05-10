package com.irontechspace.dynamicdq.repository.RowMapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.irontechspace.dynamicdq.model.Query.QueryField;
import com.irontechspace.dynamicdq.model.Query.QueryConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.log4j.Log4j2;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Log4j2
public class DataRowMapper implements RowMapper<ObjectNode> {

    private final QueryConfig queryConfig;

    private final List<String> whiteListNames; //  = Arrays.asList("id", "parent_id");

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");

    public DataRowMapper(QueryConfig queryConfig){
        this.queryConfig = queryConfig;
        whiteListNames = queryConfig.getHierarchyField() != null ? Arrays.asList(queryConfig.getHierarchyField().split("/")) : new ArrayList<>();
    }

    @Override
    public ObjectNode mapRow(ResultSet rs, int counts) throws SQLException {
        ObjectNode rowObject = new ObjectMapper().createObjectNode();
        for(QueryField queryField : queryConfig.getFields()){
            if(whiteListNames.contains(queryField.getAliasOrName()) || queryField.getVisible()) {
                String fieldName = queryField.getAliasOrName();

                switch (queryField.getTypeData()) {
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
