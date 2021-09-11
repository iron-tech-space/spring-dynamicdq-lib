package com.irontechspace.dynamicdq.executor.export.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.irontechspace.dynamicdq.configurator.query.model.QueryField;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExcelCol {
    private final static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final static String dateFormatPattern = "dd.MM.yyyy";
    private final static String timeFormatPattern = "HH:mm:ss";
    private final static String timestampFormatPattern = "yyyy-MM-dd'T'HH:mm:ssXXX";
    private final static String doubleFormatPattern = "#,##0.00000";

    private String header;
    private String name;
    private String align;
    private Long width;
    private String typeData;
    private String dataFormat;
    private Integer colSpan;
    private Integer rowSpan;
    private ExcelStyle headerStyle;
    private ExcelStyle cellStyle;


    public ExcelCol(QueryField queryField) {
        header = queryField.getHeader();
        name = queryField.getAliasOrName();
        align = queryField.getAlign();
        width = queryField.getWidth();
        typeData = queryField.getTypeData();
        dataFormat = getDefaultFormat(typeData);
        colSpan = 1;
        rowSpan = 1;
        headerStyle = null;
        cellStyle = null;
    }

    public ExcelCol(JsonNode jsonField) {
        header = jsonField.path("header").isMissingNode() ? "---" : jsonField.get("header").asText();
        name = jsonField.path("name").isMissingNode() ? null : jsonField.get("name").asText();
        align = jsonField.path("align").isMissingNode() ? "left" : jsonField.get("align").asText();
        width = jsonField.path("width").isMissingNode() ? 200L : jsonField.get("width").asLong();
        typeData = jsonField.path("typeData").isMissingNode() ? "text" : jsonField.get("typeData").asText();
        dataFormat = jsonField.path("dataFormat").isMissingNode() ? getDefaultFormat(typeData) : jsonField.get("dataFormat").asText();
        colSpan = jsonField.path("colSpan").isMissingNode() ? 1 : jsonField.get("colSpan").asInt();
        rowSpan = jsonField.path("rowSpan").isMissingNode() ? 1 : jsonField.get("rowSpan").asInt();
        try {
            headerStyle = jsonField.path("headerStyle").isMissingNode() ? null : OBJECT_MAPPER.treeToValue(jsonField.get("headerStyle"), ExcelStyle.class);
            cellStyle = jsonField.path("cellStyle").isMissingNode() ? null : OBJECT_MAPPER.treeToValue(jsonField.get("cellStyle"), ExcelStyle.class);
        } catch (JsonProcessingException ignored) {}
    }

    private String getDefaultFormat(String typeData){
        switch (typeData){
            case "date": return dateFormatPattern;
            case "time": return timeFormatPattern;
            case "timestamp": return timestampFormatPattern;
            case "double": return doubleFormatPattern;
            default: return "";
        }
    }

}
