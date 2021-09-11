package com.irontechspace.dynamicdq.configurator.query;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.irontechspace.dynamicdq.configurator.query.model.QueryField;
import com.irontechspace.dynamicdq.configurator.query.model.QueryConfig;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static com.irontechspace.dynamicdq.exceptions.ExceptionUtils.logException;
import static com.irontechspace.dynamicdq.utils.SqlUtils.inspect;

@Log4j2
public class QueryConfigRowMapper implements RowMapper<QueryConfig> {

    ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public QueryConfig mapRow(ResultSet rs, int counts) throws SQLException {
        List<String> queryConfigFields = inspect(QueryConfig.class);
        QueryConfig mappedObject = BeanUtils.instantiateClass(QueryConfig.class);
        BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(mappedObject);

        for (String field : queryConfigFields){
            Object value = rs.getObject(field.replaceAll("([^_A-Z])([A-Z])", "$1_$2").toLowerCase());
            try {
                if (field.equals("fields") && value != null) {
                    bw.setPropertyValue(field, objectMapper.readValue(value.toString(), new TypeReference<List<QueryField>>() {}));
                }
                else if (field.equals("userSettings") && value != null) {
                    bw.setPropertyValue(field, objectMapper.readValue(value.toString(), ObjectNode.class));

                } else
                    bw.setPropertyValue(field, value);
            } catch (Exception e) {
//                e.printStackTrace();
                logException(log, e);
            }
        }
        return mappedObject;
    }
}
