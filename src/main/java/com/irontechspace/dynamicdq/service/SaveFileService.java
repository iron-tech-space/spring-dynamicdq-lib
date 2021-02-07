package com.irontechspace.dynamicdq.service;

import com.irontechspace.dynamicdq.utils.FileHashSum;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.Longs;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.yaml.snakeyaml.util.UriEncoder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

@Log4j2
@Service
public class SaveFileService {

    /**
     * Директория на файловой системе в которую сохранять файлы
     */
    @Value("${dynamicdq.files.dir}")
    String rootDir;

    @Autowired
    FileHashSum fileHashSum;

    @Autowired
    DataService dataService;

    @Autowired
    SaveConfigService saveConfigService;

    @Autowired
    SaveDataService saveDataService;

    private static final int EXTENSION_SHIFT = 1;

    /**
     * Получение файла
     * @param configName - имя конфигурации для получения данных о файле
     * @param id - id файла в БД
     * @return - ResponseEntity с файлом или throw
     */
    public ResponseEntity<Resource> getFileById(String configName, UUID userId, List<String> userRoles, String id){
        ObjectNode filter = new ObjectMapper().createObjectNode();
        filter.put("id", id);
        List<ObjectNode> files = dataService.getFlatData(configName, userId, userRoles, filter, PageRequest.of(0, 1));

        if(files.size() == 0)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        else if(files.size() > 1)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "По данно ID найдено слишком много файлов");
        else {
            ObjectNode fileData = files.get(0);
            try {
                String fileAbsolutePath = Paths.get(rootDir, fileData.get("path").asText()).toAbsolutePath().toString();
                byte[] content = getContent(fileAbsolutePath);
                return ResponseEntity.ok()
                        .headers(getHeadersWithFileName(fileData.get("name").asText()))
                        .contentLength(content.length)
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .body(new ByteArrayResource(content));
            } catch (IOException ex) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Error load file", ex);
            }
        }
    }


    /**
     * Сохранить файл в БД и на файловую систему
     * @param configName - имя конфигурации для сохранения
     * @param file - загруженный файл
     * @param dataObject - дополнительные данные по файлу
     * @return - результат сохранения (throw или id файла в БД)
     */
    public ResponseEntity<Object> saveFile(String configName, UUID userId, List<String> userRoles, MultipartFile file, JsonNode dataObject) {

        saveConfigService.getSaveConfigByConfigName(configName, userId, userRoles);

        byte[] content;
        try { content = file.getBytes(); }
        catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Не удалось загрузить файл. Файл поврежден", ex); }

        String originalName = file.getOriginalFilename();

        long time = new Date().toInstant().toEpochMilli();
        byte[] hashData = Bytes.concat(Longs.toByteArray(time), originalName.getBytes(), content);

        String fileHash = fileHashSum.getHashSum(hashData);

        Path directoryPath;
        String fileAbsolutePath;
        String fileRelativePath;


        if(dataObject.has("saveDir") && !dataObject.get("saveDir").isNull()) {
            directoryPath = Paths.get(rootDir, dataObject.get("saveDir").asText(), getDate());

            fileRelativePath = Paths.get(dataObject.get("saveDir").asText(), getDate(), fileHash).toString();
            fileAbsolutePath = Paths.get(rootDir, fileRelativePath).toAbsolutePath().toString();

        } else {
            directoryPath = Paths.get(rootDir, getDate());

            fileRelativePath = Paths.get(getDate(), fileHash).toString();
            fileAbsolutePath = Paths.get(rootDir, fileRelativePath).toAbsolutePath().toString();
        }

        ObjectNode fileData = (ObjectNode) dataObject; ///new ObjectMapper().createObjectNode();
        fileData.put("name", originalName);
        fileData.put("extension", getFileExtension(originalName));
        fileData.put("path", fileRelativePath);
        fileData.put("md5Sum", fileHash);
        fileData.put("archived", false);
        fileData.put("deleted", false);
        fileData.put("isGroup", false);

        log.info("fileData               = [{}]", fileData.toString());

        if(!Files.exists(directoryPath))
            try { Files.createDirectories(directoryPath); }
            catch (IOException ex) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Не удалось загрузить файл. Ошибка создание дерриктории", ex); }

        try { saveFileIntoStorageDirectory(fileAbsolutePath, content); }
        catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Не удалось загрузить файл. Ошибка записи файла на файловую систему", ex); }

        Object key = saveDataService.saveData(configName, userId, userRoles, fileData);
        return ResponseEntity.ok().body(key);
    }

    /**
     * Получить содержимое файла по абсольтному пути
     * @param filePath - абсолютный путь к файлу
     * @return - содержимое файла
     * @throws IOException - исключение при неудачном чтении файла
     */
    private byte[] getContent(String filePath) throws IOException {
        try(val contentStream = new FileInputStream(new File(filePath))) {
            val content = new byte[contentStream.available()];
            contentStream.read(content);
            return content;
        }
    }

    /**
     * Получить заголовки для скачивания файла
     * @param fileName - имя файла
     * @return - объект заголовков
     */
    private HttpHeaders getHeadersWithFileName(String fileName) {
//        String finalEncodeFileName = Base64.getEncoder().encodeToString(fileName.getBytes());
        String finalEncodeFileName = UriEncoder.encode(fileName);
        return new HttpHeaders() {{
            add(HttpHeaders.CONTENT_DISPOSITION, String.format("attachment; filename*=UTF-8''%s", finalEncodeFileName));
            add(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
            add(HttpHeaders.PRAGMA, "no-cache");
            add(HttpHeaders.EXPIRES, "0");
        }};
    }

    /**
     * Получить расширение файла по его имени
     * @param fileName - имя файла
     * @return - стркоа с расширением
     */
    private String getFileExtension(String fileName) {
        return Optional.ofNullable(fileName)
                .filter(f -> f.contains("."))
                .map(f -> f.substring(fileName.lastIndexOf(".") + EXTENSION_SHIFT)).orElse("");
    }

    /**
     * Сохранение файла на файловую систему
     * @param absolutePath - абсолютный путь к файлу
     * @param content - содержимое файла
     * @throws IOException - исключение при неудачной записи файла
     */
    private void saveFileIntoStorageDirectory(String absolutePath, byte[] content) throws IOException {
        try (FileOutputStream fileStream = new FileOutputStream(new File(absolutePath))) {
            fileStream.write(content);
        } catch (IOException e) {
            log.error(String.format("File %s not saved", absolutePath), e);
            throw e;
        }
    }

    /**
     * Получить дату в формате YYYY-MM-DD для названия папки
     * @return - строка с датой в формате YYYY-MM-DD
     */
    private String getDate () {
        SimpleDateFormat formatter= new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date();
        log.info("Date folder            = [{}]", date.toString());
        return formatter.format(date);
    }
}