package com.irontechspace.dynamicdq.configurator.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.log4j.Log4j2;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

@Log4j2
public class ObjectNodeRowMapper implements RowMapper<ObjectNode> {
    private final static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public ObjectNode mapRow(ResultSet rs, int rowNum) throws SQLException {
        ObjectNode rowObject = OBJECT_MAPPER.createObjectNode();
        ResultSetMetaData rsm = rs.getMetaData();
        for (int i = 1; i <= rsm.getColumnCount(); i++ ) {
            char[] oldName = rsm.getColumnName(i).toCharArray();
            StringBuilder newName = new StringBuilder();
            for(int n = 0; n < oldName.length; n++)
                newName.append(oldName[n] == '_' ? Character.toUpperCase(oldName[++n]) : oldName[n]);
            rowObject.put(newName.toString(), rs.getString(i));
        }
        return rowObject;
    }
}
