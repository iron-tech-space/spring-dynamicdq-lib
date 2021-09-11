package com.irontechspace.dynamicdq.executor.export;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.irontechspace.dynamicdq.configurator.query.QueryConfigService;
import com.irontechspace.dynamicdq.configurator.query.model.QueryConfig;
import com.irontechspace.dynamicdq.configurator.query.model.QueryField;
import com.irontechspace.dynamicdq.executor.export.model.ExcelBorder;
import com.irontechspace.dynamicdq.executor.export.model.ExcelCol;
import com.irontechspace.dynamicdq.executor.export.model.ExcelFont;
import com.irontechspace.dynamicdq.executor.file.FileService;
import lombok.extern.log4j.Log4j2;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.RegionUtil;
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

import static com.irontechspace.dynamicdq.exceptions.ExceptionUtils.logException;

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
            CellRangeAddress region;
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
                headers.setHeightInPoints(headerHeight);
                Integer maxRowSpan = 1;
                for (int fIdx = startColIndex, colIdx = startColIndex;  fIdx < fields.size(); fIdx++) {
                    ExcelCol field = fields.get(fIdx);
                    Cell cell = headers.createCell(colIdx);
                    cell.setCellStyle(getHeaderStyle(workbook, field));
                    cell.setCellValue(field.getHeader());

                    sheet.setColumnWidth(colIdx, pixel2WidthUnits(field.getWidth().intValue()));
                    if(field.getColSpan() > 1 || field.getRowSpan() > 1) {
                        region = new CellRangeAddress(startRowIndex, startRowIndex + field.getRowSpan() - 1, colIdx, colIdx + field.getColSpan() - 1);
                        sheet.addMergedRegion(region);
                        maxRowSpan = field.getRowSpan() > maxRowSpan ? field.getRowSpan() : maxRowSpan;
                    } else {
                        region = new CellRangeAddress(startRowIndex, startRowIndex, colIdx, colIdx);
                    }
                    if(field.getHeaderStyle() != null)
                        setBorders(field.getHeaderStyle().getBorder(), region, sheet);
                    colIdx += field.getColSpan();
                }
                startRowIndex += maxRowSpan;
            }

            for (int dataIndex = 0, rowIndex = startRowIndex; dataIndex < data.size(); dataIndex++, rowIndex++) {
                JsonNode rowData = data.get(dataIndex);
                Row row = Optional.ofNullable(sheet.getRow(rowIndex)).orElse(sheet.createRow(startRowIndex + dataIndex));
                row.setHeightInPoints(rowHeight);
                for (int fIdx = startColIndex, colIdx = startColIndex; fIdx < fields.size(); fIdx++) {
                    ExcelCol field = fields.get(fIdx);
                    Cell cell = row.createCell(colIdx);
                    cell.setCellStyle(getCellStyle(workbook, field));
                    setCellValue(cell, field, rowData);

                    if(field.getColSpan() > 1) {
                        region = new CellRangeAddress(rowIndex, rowIndex, colIdx, colIdx + field.getColSpan() - 1);
                        sheet.addMergedRegion(region);
                    } else {
                        region = new CellRangeAddress(rowIndex, rowIndex, colIdx, colIdx);
                    }
                    if(field.getCellStyle() != null)
                        setBorders(field.getCellStyle().getBorder(), region, sheet);
                    colIdx += field.getColSpan();
                }
            }

            return OBJECT_MAPPER.createObjectNode().put("rowIndex", startRowIndex + data.size()).put("colIndex", startColIndex + fields.size());
        } catch (IOException e) {
//            e.printStackTrace();
            logException(log, e);
            return OBJECT_MAPPER.nullNode();
        }
    }

    private XSSFFont getFont(XSSFWorkbook workbook, ExcelFont excelFont) {
        XSSFFont font = workbook.createFont();
        if(excelFont != null){
            if(excelFont.getSize() != null)
                font.setFontHeight(excelFont.getSize());
            if(excelFont.getBold() != null)
                font.setBold(excelFont.getBold());
        }
        return font;
    }

    private CellStyle getHeaderStyle(XSSFWorkbook workbook, ExcelCol field) {
        CellStyle style = workbook.createCellStyle();
        setGeneralStyle(workbook, style, field);
        if(field.getHeaderStyle() != null)
            style.setFont(getFont(workbook, field.getHeaderStyle().getFont()));
        return style;
    }

    private CellStyle getCellStyle(XSSFWorkbook workbook, ExcelCol field) {
        CellStyle style = workbook.createCellStyle();
        setGeneralStyle(workbook, style, field);
        if(field.getCellStyle() != null)
            style.setFont(getFont(workbook, field.getCellStyle().getFont()));

        return style;
    }

    private void setGeneralStyle(XSSFWorkbook workbook, CellStyle style, ExcelCol field){
        if(Arrays.asList("date", "time", "timestamp", "double").contains(field.getTypeData())) {
            style.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat(field.getDataFormat()));
        }
        style.setAlignment(HorizontalAlignment.valueOf(field.getAlign().toUpperCase()));
        style.setVerticalAlignment(VerticalAlignment.CENTER);
    }

    private void setBorders(ExcelBorder border, CellRangeAddress region, Sheet sheet) {
        if(border != null){
            if(border.getTop() != null && border.getTop()) {
                RegionUtil.setBorderTop(BorderStyle.THIN, region, sheet);
            }
            if(border.getRight() != null && border.getRight()){
                RegionUtil.setBorderRight(BorderStyle.THIN, region, sheet);
            }
            if(border.getBottom() != null && border.getBottom()){
                RegionUtil.setBorderBottom(BorderStyle.THIN, region, sheet);
            }
            if(border.getLeft() != null && border.getLeft()){
                RegionUtil.setBorderLeft(BorderStyle.THIN, region, sheet);
            }
        }
    }

    private void setCellValue(Cell cell, ExcelCol field, JsonNode rowData){
        if(Arrays.asList("date", "time", "timestamp").contains(field.getTypeData())) {
            cell.setCellValue(LocalDateTime.parse(rowData.get(field.getName()).asText(), DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")));
        } else if("double".equals(field.getTypeData())){
            cell.setCellValue(rowData.get(field.getName()).asDouble());
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
//            e.printStackTrace();
            logException(log, e);
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
//            e.printStackTrace();
            logException(log, e);
            return null;
        }
    }
}
