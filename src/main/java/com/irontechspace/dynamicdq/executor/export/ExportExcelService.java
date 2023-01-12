package com.irontechspace.dynamicdq.executor.export;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.irontechspace.dynamicdq.configurator.query.QueryConfigService;
import com.irontechspace.dynamicdq.configurator.query.model.QueryConfig;
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
import java.util.concurrent.atomic.AtomicReference;
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
        if (configName != null) {
            QueryConfig config = queryConfigService.getByName(configName, userId, userRoles);
//            fields = config.getFields().stream().filter(QueryField::getVisible).map(ExcelCol::new).collect(Collectors.toList());
            fields = config.getFields().stream().map(ExcelCol::new).collect(Collectors.toList());
        }
        if (body.get("fields") != null && body.get("fields").isArray()) {
            ArrayNode fieldsArray = (ArrayNode) body.get("fields");
//            fields.add(new ExcelCol(jsonField));
            for (JsonNode jsonField : fieldsArray)
                for (ExcelCol field : fields)
                    if (!jsonField.path("name").isMissingNode() && field.getName().equals(jsonField.get("name").asText()))
                        field.setExcelCol(jsonField);
        }

        fields = fields.stream().filter(ExcelCol::getVisible).collect(Collectors.toList());

        XSSFWorkbook workbook = null;
        try {
            workbook = OBJECT_MAPPER.treeToValue(body.get("file"), XSSFWorkbook.class);
        } catch (IOException e) {
            logException(log, e);
            return OBJECT_MAPPER.nullNode();
        }
        Sheet sheet = workbook.getSheet(body.get("sheet").asText());
        CellRangeAddress region;
        ArrayNode data = (ArrayNode) body.get("data");
        boolean hiddenHeader = !body.path("hiddenHeader").isMissingNode() && body.get("hiddenHeader").asBoolean();
        float headerHeight = body.path("headerHeight").isMissingNode() ? -1 : body.get("headerHeight").floatValue();
        float rowHeight = body.path("rowHeight").isMissingNode() ? -1 : body.get("rowHeight").floatValue();
//        int startRowIndex = body.get("startCell").get("row").asInt();
//        int startColIndex = body.get("startCell").get("col").asInt();
        int startRowIndex = Objects.requireNonNull(Objects.requireNonNull(body.get("startCell"), "Не заполнено startCell").get("row"), "Не заполнено startCell.row").asInt();
        int startColIndex = Objects.requireNonNull(Objects.requireNonNull(body.get("startCell"), "Не заполнено startCell").get("col"), "Не заполнено startCell.col").asInt();

        if (hiddenHeader) {
            for (int fIdx = 0, colIdx = startColIndex; fIdx < fields.size(); fIdx++) {
                ExcelCol field = fields.get(fIdx);
                sheet.setColumnWidth(colIdx, pixel2WidthUnits(field.getWidth().intValue()));
                colIdx += field.getColSpan();
            }
        } else {
            Row headers = Optional.ofNullable(sheet.getRow(startRowIndex)).orElse(sheet.createRow(startRowIndex));
            headers.setHeightInPoints(headerHeight);
            Integer maxRowSpan = 1;
            for (int fIdx = 0, colIdx = startColIndex; fIdx < fields.size(); fIdx++) {
                ExcelCol field = fields.get(fIdx);
                Cell cell = headers.createCell(colIdx);
                cell.setCellStyle(getHeaderStyle(workbook, field));
                cell.setCellValue(field.getHeader());

                sheet.setColumnWidth(colIdx, pixel2WidthUnits(field.getWidth().intValue()));
                if (field.getColSpan() > 1 || field.getRowSpan() > 1) {
                    region = new CellRangeAddress(startRowIndex, startRowIndex + field.getRowSpan() - 1, colIdx, colIdx + field.getColSpan() - 1);
                    sheet.addMergedRegion(region);
                    maxRowSpan = field.getRowSpan() > maxRowSpan ? field.getRowSpan() : maxRowSpan;
                } else {
                    region = new CellRangeAddress(startRowIndex, startRowIndex, colIdx, colIdx);
                }
                if (field.getHeaderStyle() != null)
                    setBorders(field.getHeaderStyle().getBorder(), region, sheet);
                colIdx += field.getColSpan();
            }
            startRowIndex += maxRowSpan;
        }

        // Создание стилей на колонки
        List<CellStyle> cellStyles = new ArrayList<>();
        for (ExcelCol field : fields) {
            cellStyles.add(getCellStyle(workbook, field));
        }

        for (int dataIndex = 0, rowIndex = startRowIndex; dataIndex < data.size(); dataIndex++, rowIndex++) {
            JsonNode rowData = data.get(dataIndex);
            Row row = Optional.ofNullable(sheet.getRow(rowIndex)).orElse(sheet.createRow(startRowIndex + dataIndex));
            row.setHeightInPoints(rowHeight);
            for (int fIdx = 0, colIdx = startColIndex; fIdx < fields.size(); fIdx++) {
                ExcelCol field = fields.get(fIdx);
                Cell cell = row.createCell(colIdx);
                cell.setCellStyle(cellStyles.get(fIdx));
                setCellValue(cell, field, rowData);
                if (field.getColSpan() > 1 && field.getCellStyle() != null) {
                    region = new CellRangeAddress(rowIndex, rowIndex, colIdx, colIdx + field.getColSpan() - 1);
                    sheet.addMergedRegion(region);
                    setBorders(field.getCellStyle().getBorder(), region, sheet);
                }
                colIdx += field.getColSpan();
            }
//            logDur(startNanosRow, "Row: [" + rowIndex + "] => ");
        }
//        logDur(startNanos, "Table => ");
        return OBJECT_MAPPER.createObjectNode().put("rowIndex", startRowIndex + data.size()).put("colIndex", startColIndex + fields.size());
    }

    private long logDur (long startNanos, String msg) {
        long finishNanos = System.nanoTime();
        long durationNanos = finishNanos - startNanos;
        log.info("{} => {}ms {}ns", msg,durationNanos / 1000000L, durationNanos % 1000000L);
        return finishNanos;
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
        if(field.getHeaderStyle() != null) {
            style.setFont(getFont(workbook, field.getHeaderStyle().getFont()));

            if(field.getHeadAlign() != null)
                style.setAlignment(HorizontalAlignment.valueOf(field.getHeadAlign().toUpperCase()));

            if (field.getHeaderStyle().getHAlign() != null)
                style.setAlignment(HorizontalAlignment.valueOf(field.getHeaderStyle().getHAlign().toUpperCase()));

            if (field.getHeaderStyle().getVAlign() != null)
                style.setVerticalAlignment(VerticalAlignment.valueOf(field.getHeaderStyle().getVAlign().toUpperCase()));

            if (field.getHeaderStyle().getWrapped() != null)
                style.setWrapText(field.getHeaderStyle().getWrapped());
        }
        return style;
    }

    private CellStyle getCellStyle(XSSFWorkbook workbook, ExcelCol field) {
        CellStyle style = workbook.createCellStyle();
        setGeneralStyle(workbook, style, field);
        if(field.getCellStyle() != null) {
            style.setFont(getFont(workbook, field.getCellStyle().getFont()));

            if(field.getAlign() != null)
                style.setAlignment(HorizontalAlignment.valueOf(field.getAlign().toUpperCase()));

            if (field.getCellStyle().getHAlign() != null)
                style.setAlignment(HorizontalAlignment.valueOf(field.getCellStyle().getHAlign().toUpperCase()));

            if (field.getCellStyle().getVAlign() != null)
                style.setVerticalAlignment(VerticalAlignment.valueOf(field.getCellStyle().getVAlign().toUpperCase()));

            if (field.getCellStyle().getWrapped() != null)
                style.setWrapText(field.getCellStyle().getWrapped());
        }

        if(field.getColSpan() == 1 && field.getCellStyle() != null){
            ExcelBorder border = field.getCellStyle().getBorder();
            if(border != null){
                if(border.getTop() != null && border.getTop()) {
                    style.setBorderTop(BorderStyle.THIN);
                }
                if(border.getRight() != null && border.getRight()){
                    style.setBorderRight(BorderStyle.THIN);
                }
                if(border.getBottom() != null && border.getBottom()){
                    style.setBorderBottom(BorderStyle.THIN);
                }
                if(border.getLeft() != null && border.getLeft()){
                    style.setBorderLeft(BorderStyle.THIN);
                }
            }
        }

        return style;
    }

    private void setGeneralStyle(XSSFWorkbook workbook, CellStyle style, ExcelCol field){
        if(Arrays.asList("date", "time", "timestamp", "double").contains(field.getTypeData())) {
            style.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat(field.getDataFormat()));
        }

        style.setVerticalAlignment(VerticalAlignment.CENTER);
    }

    private void setBorders(ExcelBorder border, CellRangeAddress region, Sheet sheet) {
        if(border != null){
            if(border.getTop() != null && border.getTop()) {
                RegionUtil.setBorderTop(BorderStyle.THIN, region, sheet);
            }
            if(border.getRight() != null && border.getRight()){
//                long st = System.nanoTime();
                // TODO This func very long time executing
                RegionUtil.setBorderRight(BorderStyle.THIN, region, sheet);
//                logDur(st, "[setBorders Right]");
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
        if(rowData.get(field.getName()) == null || rowData.get(field.getName()).isNull() || rowData.get(field.getName()).asText().trim().equals("")){
            cell.setCellValue("---");
            return;
        }

        if(field.getCellFormula() != null) {
            AtomicReference<String> formula = new AtomicReference<>(field.getCellFormula());
            rowData.fields().forEachRemaining(rowField ->
                formula.set(formula.get().replaceAll(rowField.getKey(), rowField.getValue().asText()))
            );
            cell.setCellFormula(formula.get());
        } else {
            if(Arrays.asList("date", "time", "timestamp").contains(field.getTypeData())) {
                cell.setCellValue(LocalDateTime.parse(rowData.get(field.getName()).asText(), DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")));
            } else if(Arrays.asList("int", "double").contains(field.getTypeData())){
                cell.setCellValue(rowData.get(field.getName()).asDouble());
            } else if("bool".equals(field.getTypeData())){
                cell.setCellValue(rowData.get(field.getName()).asBoolean());
            } else {
                cell.setCellValue(rowData.get(field.getName()).asText());
            }
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
            workbook.getCreationHelper().createFormulaEvaluator().evaluateAll();
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
