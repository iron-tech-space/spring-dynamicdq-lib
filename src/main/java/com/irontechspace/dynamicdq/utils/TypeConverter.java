package com.irontechspace.dynamicdq.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Log4j2
@Component
public class TypeConverter {

    public List<Object> getListObjectsByType (String type, ArrayNode dataObjects) {
        List<Object> values = new ArrayList<>();
        switch (type) {
            case "uuid":
                for (JsonNode jsonNode : dataObjects)
                    if(!jsonNode.isNull())
                        values.add(UUID.fromString(jsonNode.asText()));
                break;
            case "text":
                for (JsonNode jsonNode : dataObjects)
                    if(!jsonNode.isNull())
                        values.add(jsonNode.asText());
                break;
            case "int":
                for (JsonNode jsonNode : dataObjects)
                    if(!jsonNode.isNull())
                        values.add(jsonNode.asLong());
                break;
            case "double":
                for (JsonNode jsonNode : dataObjects)
                    if(!jsonNode.isNull())
                        values.add(jsonNode.asDouble());
                break;
        }
        return values;
    }

    public Object getObjectByType (String type, String field, JsonNode dataObject) {
        return getObjectByType(type, field, dataObject, null);
    }

    public Object getObjectByType (String type, String field, JsonNode dataObject, Object parentResult){
        // boolean has = dataObject.has(field);
        // boolean isNull = dataObject.get(field).isNull();
        if(!type.equals("parentResult") && (!dataObject.has(field) || dataObject.get(field).isNull()))
            return null;

        switch (type) {
            case "uuid":
                return UUID.fromString(dataObject.get(field).asText());
            case "text":
                return dataObject.get(field).asText();
            case "int":
            case "aggregate":
            case "math":
                return dataObject.get(field).asLong();
            case "timestamp":
                return OffsetDateTime.parse(dataObject.get(field).asText(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            case "date":
                return new Date(OffsetDateTime.parse(dataObject.get(field).asText(), DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant().toEpochMilli());
            case "time":
                return OffsetTime.parse(dataObject.get(field).asText(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            case "bool":
                return Boolean.valueOf(dataObject.get(field).asText());
            case "double":
                return dataObject.get(field).asDouble();
            case "parentResult":
                return parentResult;
            case "password":
                return new BCryptPasswordEncoder().encode(dataObject.get(field).asText());
            default:
                return dataObject.get(field).textValue();

        }
    }
}
