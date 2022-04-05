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

    private Boolean visible = false;
    private String header = "---";
    private String name = null;
    private String align = "left";
    private String headAlign = "left";
    private Long width = 200L;
    private String typeData = "text";
    private String dataFormat = getDefaultFormat(typeData);
    private Integer colSpan = 1;
    private Integer rowSpan = 1;
    private ExcelStyle headerStyle = null;
    private ExcelStyle cellStyle = null;
    private String cellFormula = null;
    private String defValue = "-";


    public ExcelCol(QueryField queryField) {
        header = queryField.getHeader();
        name = queryField.getAliasOrName();
        align = queryField.getAlign();
        headAlign = queryField.getAlign();
        width = queryField.getWidth();
        typeData = queryField.getTypeData();
        dataFormat = getDefaultFormat(typeData);
        colSpan = 1;
        rowSpan = 1;
        headerStyle = null;
        cellStyle = null;
        cellFormula = null;
        defValue = "-";
    }

    public void setExcelCol(JsonNode jsonField) {
        visible = !jsonField.path("visible").isMissingNode() && jsonField.get("visible").asBoolean();
        header = jsonField.path("header").isMissingNode() ? header : jsonField.get("header").asText();
        name = jsonField.path("name").isMissingNode() ? name : jsonField.get("name").asText();
        align = jsonField.path("align").isMissingNode() ? align : jsonField.get("align").asText();
        headAlign = jsonField.path("headAlign").isMissingNode() ? headAlign : jsonField.get("headAlign").asText();
        width = jsonField.path("width").isMissingNode() ? width : jsonField.get("width").asLong();
        typeData = jsonField.path("typeData").isMissingNode() ? typeData : jsonField.get("typeData").asText();
        dataFormat = jsonField.path("dataFormat").isMissingNode() ? getDefaultFormat(typeData) : jsonField.get("dataFormat").asText();
        colSpan = jsonField.path("colSpan").isMissingNode() ? colSpan : jsonField.get("colSpan").asInt();
        rowSpan = jsonField.path("rowSpan").isMissingNode() ? rowSpan : jsonField.get("rowSpan").asInt();
        try {
            headerStyle = jsonField.path("headerStyle").isMissingNode() ? headerStyle : OBJECT_MAPPER.treeToValue(jsonField.get("headerStyle"), ExcelStyle.class);
            cellStyle = jsonField.path("cellStyle").isMissingNode() ? cellStyle : OBJECT_MAPPER.treeToValue(jsonField.get("cellStyle"), ExcelStyle.class);
        } catch (JsonProcessingException ignored) {}
        cellFormula = jsonField.path("cellFormula").isMissingNode() ? cellFormula : jsonField.get("cellFormula").asText();
        defValue = jsonField.path("defValue").isMissingNode() ? defValue : jsonField.get("defValue").asText();

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
