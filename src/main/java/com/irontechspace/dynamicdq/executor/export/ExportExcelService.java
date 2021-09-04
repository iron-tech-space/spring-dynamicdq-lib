package com.irontechspace.dynamicdq.executor.export;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.irontechspace.dynamicdq.configurator.query.QueryConfigService;
import com.irontechspace.dynamicdq.configurator.query.model.QueryConfig;
import com.irontechspace.dynamicdq.configurator.query.model.QueryField;
import com.irontechspace.dynamicdq.executor.file.FileService;
import lombok.extern.log4j.Log4j2;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
        QueryConfig config = queryConfigService.getByName(configName, userId, userRoles);
        try {
            XSSFWorkbook workbook = OBJECT_MAPPER.treeToValue(body.get("file"), XSSFWorkbook.class);
            Sheet sheet = workbook.getSheet(body.get("sheet").asText());
            ArrayNode data = (ArrayNode) body.get("data");
            int startRowIndex = body.get("startCell").get("row").asInt();
            int startColIndex = body.get("startCell").get("col").asInt();
            List<QueryField> fields = config.getFields().stream().filter(QueryField::getVisible).collect(Collectors.toList());

            Row headers = sheet.createRow(startRowIndex++);
            for (int colIndex = startColIndex; colIndex < fields.size(); colIndex++) {
                QueryField field = fields.get(colIndex);
                Cell cell = headers.createCell(colIndex);
//                XSSFFont font = workbook.createFont();
//                font.setBold(true);
                CellStyle style = workbook.createCellStyle();
//                style.setFont(font);
                style.setAlignment(HorizontalAlignment.valueOf(field.getAlign().toUpperCase()));
                style.setVerticalAlignment(VerticalAlignment.CENTER);
                sheet.setColumnWidth(colIndex, pixel2WidthUnits(field.getWidth().intValue()));
                cell.setCellStyle(style);
                cell.setCellValue(field.getHeader());
            }

//            int lastRowIndex = startRowIndex - 1 + data.size();
            for (int dataIndex = 0; dataIndex < data.size(); dataIndex++) {
                JsonNode rowData = data.get(dataIndex);
                Row row = sheet.createRow(startRowIndex + dataIndex);
                for (int colIndex = startColIndex; colIndex < fields.size(); colIndex++) {
                    QueryField field = fields.get(colIndex);
                    Cell cell = row.createCell(colIndex);
                    CellStyle style = workbook.createCellStyle();
                    style.setAlignment(HorizontalAlignment.valueOf(field.getAlign().toUpperCase()));
                    style.setVerticalAlignment(VerticalAlignment.CENTER);
//                    sheet.setColumnWidth(colIndex, field.getWidth().intValue());
                    cell.setCellStyle(style);
                    cell.setCellValue(rowData.get(field.getAliasOrName()).asText());
                }
            }

            return OBJECT_MAPPER.createObjectNode().put("rowIndex", startRowIndex + data.size()).put("colIndex", startColIndex + fields.size());
        } catch (IOException e) {
            e.printStackTrace();
            return OBJECT_MAPPER.nullNode();
        }
    }

//    private void addRowExcel() {
//
//    }

    public Boolean addRowExcel(JsonNode body) {
        try {
            XSSFWorkbook workbook = OBJECT_MAPPER.treeToValue(body.get("file"), XSSFWorkbook.class);
            Sheet sheet = workbook.getSheet(body.get("sheet").asText());
            Row row = sheet.createRow(body.get("startCell").get("row").asInt());
            JsonNode data = body.get("data");
            AtomicInteger colIndex = new AtomicInteger(body.get("startCell").get("col").asInt());
            data.fields().forEachRemaining(field -> {
                sheet.autoSizeColumn(colIndex.get());
                addCell(row, colIndex.getAndIncrement(), field.getValue().asText());
            });
//            if(sheet.getMergedRegion(0) != null)
//            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));
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
