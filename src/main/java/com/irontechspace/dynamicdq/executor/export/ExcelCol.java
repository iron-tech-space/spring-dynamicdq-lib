package com.irontechspace.dynamicdq.executor.export;

import com.fasterxml.jackson.databind.JsonNode;
import com.irontechspace.dynamicdq.configurator.query.model.QueryField;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExcelCol {

    private final static String dateFormatPattern = "dd.MM.yyyy";
    private final static String timeFormatPattern = "HH:mm:ss";
    private final static String timestampFormatPattern = "yyyy-MM-dd'T'HH:mm:ssXXX";

    private String header;
    private String name;
    private String align;
    private Long width;
    private String typeData;
    private String dateFormat;

    public ExcelCol(QueryField queryField) {
        header = queryField.getHeader();
        name = queryField.getAliasOrName();
        align = queryField.getAlign();
        width = queryField.getWidth();
        typeData = queryField.getTypeData();
    }

    public ExcelCol(JsonNode jsonField) {
        header = jsonField.path("header").isMissingNode() ? "---" : jsonField.get("header").asText();
        name = jsonField.path("name").isMissingNode() ? null : jsonField.get("name").asText();
        align = jsonField.path("align").isMissingNode() ? "left" : jsonField.get("align").asText();
        width = jsonField.path("width").isMissingNode() ? 200L : jsonField.get("width").asLong();
        typeData = jsonField.path("typeData").isMissingNode() ? "text" : jsonField.get("typeData").asText();
        dateFormat = jsonField.path("dateFormat").isMissingNode() ? getDefaultFormat(typeData) : jsonField.get("dateFormat").asText();
    }

    private String getDefaultFormat(String typeData){
        switch (typeData){
            case "time": return timeFormatPattern;
            case "timestamp": return timestampFormatPattern;
            default: return dateFormatPattern;
        }
    }

}
