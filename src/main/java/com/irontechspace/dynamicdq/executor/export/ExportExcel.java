package com.irontechspace.dynamicdq.executor.export;

import lombok.extern.log4j.Log4j2;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Log4j2
public class ExportExcel {
    private XSSFWorkbook workbook;
    private XSSFSheet sheet;

    public ExportExcel() {
        workbook = new XSSFWorkbook();


        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);
            MultipartFile multipartFile = new MockMultipartFile("file", "file", "text/plain", bos.toByteArray());
        } catch (IOException e) {
//            e.printStackTrace();
            log.error(e);
        }
    }
}
