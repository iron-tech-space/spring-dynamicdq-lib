package com.irontechspace.dynamicdq.executor.export;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.irontechspace.dynamicdq.configurator.query.QueryConfigService;
import com.irontechspace.dynamicdq.configurator.query.model.QueryConfig;
import com.irontechspace.dynamicdq.configurator.query.model.QueryField;
import com.irontechspace.dynamicdq.executor.file.FileService;
import lombok.extern.log4j.Log4j2;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Log4j2
@Service
public class ExportExcelService {

    private final static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // Services
    private final QueryConfigService queryConfigService;
    private final FileService fileService;

    public static final short EXCEL_COLUMN_WIDTH_FACTOR = 256;
    public static final short EXCEL_ROW_HEIGHT_FACTOR = 20;
    public static final int UNIT_OFFSET_LENGTH = 7;
    public static final int[] UNIT_OFFSET_MAP = new int[] { 0, 36, 73, 109, 146, 182, 219 };

    public short pixel2WidthUnits(int pxs) {
        short widthUnits = (short) (EXCEL_COLUMN_WIDTH_FACTOR * (pxs / UNIT_OFFSET_LENGTH));
        widthUnits += UNIT_OFFSET_MAP[(pxs % UNIT_OFFSET_LENGTH)];
        return widthUnits;
    }

    @Autowired
    public ExportExcelService(QueryConfigService queryConfigService, FileService fileService) {
        this.queryConfigService = queryConfigService;
        this.fileService = fileService;
    }

    public XSSFWorkbook createExcel(JsonNode body) {
        XSSFWorkbook workbook = new XSSFWorkbook();
        for(JsonNode sheet : body.get("sheets")){
            workbook.createSheet(sheet.asText());
        }
        return workbook;
    }

    public JsonNode addTableExcel(String configName, UUID userId, List<String> userRoles, JsonNode body) {

        List<ExcelCol> fields = new ArrayList<>();
        if(configName != null) {
            QueryConfig config = queryConfigService.getByName(configName, userId, userRoles);
            fields = config.getFields().stream().filter(QueryField::getVisible).map(ExcelCol::new).collect(Collectors.toList());
        } else if (body.get("fields") != null && body.get("fields").isArray()) {
            ArrayNode fieldsArray = (ArrayNode) body.get("fields");
            for(JsonNode jsonField : fieldsArray)
                fields.add(new ExcelCol(jsonField));
        }

        try {
            XSSFWorkbook workbook = OBJECT_MAPPER.treeToValue(body.get("file"), XSSFWorkbook.class);
            Sheet sheet = workbook.getSheet(body.get("sheet").asText());
            ArrayNode data = (ArrayNode) body.get("data");
            boolean hiddenHeader = !body.path("hiddenHeader").isMissingNode() && body.get("hiddenHeader").asBoolean();
            float headerHeight = body.path("headerHeight").isMissingNode() ? -1 : body.get("headerHeight").floatValue();
            float rowHeight = body.path("rowHeight").isMissingNode() ? -1 : body.get("rowHeight").floatValue();
            int startRowIndex = body.get("startCell").get("row").asInt();
            int startColIndex = body.get("startCell").get("col").asInt();

            if(hiddenHeader) {
                for (int colIndex = startColIndex; colIndex < fields.size(); colIndex++) {
                    sheet.setColumnWidth(colIndex, pixel2WidthUnits(fields.get(colIndex).getWidth().intValue()));
                }
            } else {
                Row headers = Optional.ofNullable(sheet.getRow(startRowIndex)).orElse(sheet.createRow(startRowIndex));
                startRowIndex++;
                headers.setHeightInPoints(headerHeight);
                for (int colIndex = startColIndex; colIndex < fields.size(); colIndex++) {
                    ExcelCol field = fields.get(colIndex);
                    sheet.setColumnWidth(colIndex, pixel2WidthUnits(field.getWidth().intValue()));

                    Cell cell = headers.createCell(colIndex);
                    cell.setCellStyle(getHeaderStyle(workbook, field));
                    cell.setCellValue(field.getHeader());
                }
            }

            for (int dataIndex = 0; dataIndex < data.size(); dataIndex++) {
                JsonNode rowData = data.get(dataIndex);
                Row row = Optional.ofNullable(sheet.getRow(startRowIndex + dataIndex)).orElse(sheet.createRow(startRowIndex + dataIndex));
                row.setHeightInPoints(rowHeight);
                for (int colIndex = startColIndex; colIndex < fields.size(); colIndex++) {
                    ExcelCol field = fields.get(colIndex);
                    Cell cell = row.createCell(colIndex);
                    cell.setCellStyle(getCellStyle(workbook, field));
                    setCellValue(cell, field, rowData);
                }
            }

            return OBJECT_MAPPER.createObjectNode().put("rowIndex", startRowIndex + data.size()).put("colIndex", startColIndex + fields.size());
        } catch (IOException e) {
            e.printStackTrace();
            return OBJECT_MAPPER.nullNode();
        }
    }

    private XSSFFont getHeaderFont(XSSFWorkbook workbook) {
        XSSFFont font = workbook.createFont();
        font.setBold(true);
        return font;
    }

    private CellStyle getHeaderStyle(XSSFWorkbook workbook, ExcelCol field) {
        CellStyle style = getCellStyle(workbook, field);
        style.setFont(getHeaderFont(workbook));
        return style;
    }

    private CellStyle getCellStyle(XSSFWorkbook workbook, ExcelCol field) {
        CellStyle style = workbook.createCellStyle();
        if(Arrays.asList("date", "time", "timestamp").contains(field.getTypeData())) {
            style.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat(field.getDateFormat()));
        }
        style.setAlignment(HorizontalAlignment.valueOf(field.getAlign().toUpperCase()));
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private void setCellValue(Cell cell, ExcelCol field, JsonNode rowData){
        if(Arrays.asList("date", "time", "timestamp").contains(field.getTypeData())) {
            cell.setCellValue(LocalDateTime.parse(rowData.get(field.getName()).asText(), DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")));
        } else {
            cell.setCellValue(rowData.get(field.getName()).asText());
        }
    }

    public Boolean addRowExcel(JsonNode body) {
        try {
            XSSFWorkbook workbook = OBJECT_MAPPER.treeToValue(body.get("file"), XSSFWorkbook.class);
            Sheet sheet = workbook.getSheet(body.get("sheet").asText());
            Row row = sheet.createRow(body.get("startCell").get("row").asInt());
            JsonNode data = body.get("data");
            AtomicInteger colIndex = new AtomicInteger(body.get("startCell").get("col").asInt());
            data.fields().forEachRemaining(field -> {
                addCell(row, colIndex.getAndIncrement(), field.getValue().asText());
            });
            // sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }



    public void addCell(Row row, int col, String value) {
        Cell cell = row.createCell(col);

        XSSFRichTextString rt = new XSSFRichTextString();
        XSSFFont font1 = (XSSFFont) row.getSheet().getWorkbook().createFont();
        font1.setBold(true);
//        font1.setColor(new XSSFColor(new java.awt.Color(75, 15, 75), new DefaultIndexedColorMap()));
        font1.setColor(new XSSFColor(new byte[]{12, 22, 2}, new DefaultIndexedColorMap()));
        rt.append(value, font1);
        XSSFFont font3 = (XSSFFont) row.getSheet().getWorkbook().createFont();
        font3.setColor(IndexedColors.BLUE.getIndex());
        rt.append("Jumped over the lazy dog", font3);
        cell.setCellValue(rt);
    }

    public Object saveExcel(String configName, UUID userId, List<String> userRoles, JsonNode body) {
        String fileName = body.get("fileName").asText();
        try {
            XSSFWorkbook workbook = OBJECT_MAPPER.treeToValue(body.get("file"), XSSFWorkbook.class);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);
            MultipartFile multipartFile = new MockMultipartFile(fileName, fileName, "application/vnd.ms-excel", bos.toByteArray());
            // (String configName, UUID userId, List<String> userRoles, MultipartFile file, JsonNode dataObject)
            return fileService.saveFile(configName, userId, userRoles, multipartFile, OBJECT_MAPPER.createObjectNode());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
