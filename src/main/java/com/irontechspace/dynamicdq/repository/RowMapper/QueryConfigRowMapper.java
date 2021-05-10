package com.irontechspace.dynamicdq.repository.RowMapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.irontechspace.dynamicdq.model.Query.QueryField;
import com.irontechspace.dynamicdq.model.Query.QueryConfig;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static com.irontechspace.dynamicdq.utils.SqlUtils.inspect;

@Log4j2
public class QueryConfigRowMapper implements RowMapper<QueryConfig> {

    ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public QueryConfig mapRow(ResultSet rs, int counts) throws SQLException {
        List<String> fields = inspect(QueryConfig.class);
        QueryConfig mappedObject = BeanUtils.instantiateClass(QueryConfig.class);
        BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(mappedObject);

        for (String field : fields){
            Object value = rs.getObject(field.replaceAll("([^_A-Z])([A-Z])", "$1_$2").toLowerCase());

            if(field.equals("fields") && value != null){
                try {
                    List<QueryField> fieldd = objectMapper.readValue(value.toString(), new TypeReference<List<QueryField>>(){});
                    bw.setPropertyValue(field, fieldd);
                } catch (JsonProcessingException e){
                    e.printStackTrace();
                }
            } else
                bw.setPropertyValue(field, value);
        }
        return mappedObject;
    }
}
