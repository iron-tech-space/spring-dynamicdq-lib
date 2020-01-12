package com.irontechspace.dynamicdq.repository.RowMapper;

import com.irontechspace.dynamicdq.model.ConfigField;
import com.irontechspace.dynamicdq.model.ConfigTable;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.log4j.Log4j2;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Log4j2
public class DataRowMapper implements RowMapper<ObjectNode> {

    private ConfigTable configTable;

    private List<String> whiteListNames; //  = Arrays.asList("id", "parent_id");

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
                    case "int":
                        rowObject.put(fieldName, rs.getLong(fieldName));
                        break;
                    case "timestamp":
                        rowObject.put(fieldName,
                                rs.getTimestamp(fieldName) != null ?
                                        rs.getTimestamp(fieldName).toLocalDateTime().toString() :
                                        null);
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
