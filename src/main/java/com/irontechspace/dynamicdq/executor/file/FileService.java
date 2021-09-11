package com.irontechspace.dynamicdq.executor.file;

import com.irontechspace.dynamicdq.configurator.save.SaveConfigService;
import com.irontechspace.dynamicdq.executor.file.model.File;
import com.irontechspace.dynamicdq.executor.query.QueryService;
import com.irontechspace.dynamicdq.executor.save.SaveService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Log4j2
@Service
public class FileService {

    /** Директория на файловой системе в которую сохранять файлы */
    @Value("${dynamicdq.files.dir}")
    String rootDir;

    @Autowired
    FileHashSum fileHashSum;

    @Autowired
    QueryService queryService;

    @Autowired
    SaveConfigService saveConfigService;

    @Autowired
    SaveService saveService;

    /** Получить Response с файлом */
    public ResponseEntity<Resource> getFileResponse(String configName, UUID userId, List<String> userRoles, JsonNode filter){
        File file = getFile(configName, userId, userRoles, filter);
        return ResponseEntity.ok()
                .headers(getHeadersWithFileName(file.getName()))
                .contentLength(file.getContent().length)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new ByteArrayResource(file.getContent()));
    }

    /** Получить Response с файлами */
    public ResponseEntity<Resource> getFilesResponse(String configName, UUID userId, List<String> userRoles, JsonNode filter){
        return getZipArchive(getFiles(configName, userId, userRoles, filter));
    }

    public ResponseEntity<Resource> getZipArchive(List<File> files){
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(byteArrayOutputStream);
        ZipOutputStream zipOutputStream = new ZipOutputStream(bufferedOutputStream);
        Map<String, Integer> zipNames = new HashMap<>();
        try {
            for(File file : files){
                ZipEntry zipEntry;

                if (zipNames.containsKey(file.getName())) {
                    Integer newIndex = zipNames.get(file.getName()) + 1;
                    zipEntry = new ZipEntry(createEntryName(file.getName(), newIndex));
                    zipNames.put(zipEntry.getName(), newIndex);
                } else {
                    zipEntry = new ZipEntry(file.getName());
                    zipNames.put(zipEntry.getName(), 0);
                }

                zipOutputStream.putNextEntry(zipEntry);
                zipOutputStream.write(file.getContent(), 0, file.getContent().length);
                zipOutputStream.closeEntry();
            }
            bufferedOutputStream.close();
            byteArrayOutputStream.close();

            zipOutputStream.finish();
            zipOutputStream.flush();

            byte[] zipContent = byteArrayOutputStream.toByteArray();

            return ResponseEntity.ok()
                    .headers(getHeadersWithFileName("files.zip"))
                    .contentLength(zipContent.length)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(new ByteArrayResource(zipContent));
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Ошибка записи в архив", ex);
        }
    }

    /** Получить файл по параметрам */
    public File getFile(String configName, UUID userId, List<String> userRoles, JsonNode filter) {
        ObjectNode fileData = queryService.getObject(configName, userId, userRoles, filter, PageRequest.of(0, 1));
        try {
            return getFile(fileData);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Ошибка чтения файла", ex);
        }
    }
    /** Получить файлы по параметрам */
    public List<File> getFiles(String configName, UUID userId, List<String> userRoles, JsonNode filter) {
        List<ObjectNode> filesData = queryService.getFlatData(configName, userId, userRoles, filter, PageRequest.of(0, 1));
        List<File> files = new ArrayList<>();
        try {
            for(ObjectNode fileData : filesData) {
                files.add(getFile(fileData));
            }
            return files;
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Ошибка чтения файла", ex);
        }
    }

    private File getFile (ObjectNode fileData) throws IOException {
        String fileAbsolutePath = Paths.get(rootDir, fileData.get("path").asText()).toAbsolutePath().toString();
        File file = new File();
        file.setAbsolutePath(fileAbsolutePath);
        file.setName(fileData.get("name").asText());
        file.setContent(getContent(fileAbsolutePath));
        return file;
    }

    /**
     * Сохранить файл в БД и на файловую систему
     * @param configName - имя конфигурации для сохранения
     * @param file - загруженный файл
     * @param dataObject - дополнительные данные по файлу
     * @return - результат сохранения (throw или id файла в БД)
     */
    public Object saveFile(String configName, UUID userId, List<String> userRoles, MultipartFile file, JsonNode dataObject) {

        saveConfigService.getByName(configName, userId, userRoles);

        byte[] content;
        try { content = file.getBytes(); }
        catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Не удалось загрузить файл. Файл поврежден", ex); }

        String originalName = Optional.ofNullable(file.getOriginalFilename()).orElse("");

        long time = new Date().toInstant().toEpochMilli();
//        Long.valueOf(time)

        byte[] hashData;
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
            outputStream.write( ByteBuffer.allocate(Long.BYTES).putLong(time).array() );
            outputStream.write( originalName.getBytes() );
            outputStream.write( content );
            hashData = outputStream.toByteArray();
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Не удалось загрузить файл. Ошибка расчета хэш суммы файла", ex); }
//        byte[] hashData = Bytes.concat(Longs.toByteArray(time), originalName.getBytes(), content);

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

        if(!Files.exists(directoryPath))
            try { Files.createDirectories(directoryPath); }
            catch (IOException ex) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Не удалось загрузить файл. Ошибка создание дерриктории", ex); }

        try { saveFileIntoStorageDirectory(fileAbsolutePath, content); }
        catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Не удалось загрузить файл. Ошибка записи файла на файловую систему", ex); }

//        Object key = saveService.saveData(configName, userId, userRoles, fileData);
//        return ResponseEntity.ok().body(key);
        return saveService.saveData(configName, userId, userRoles, fileData);
    }

    /**
     * Получить содержимое файла по абсольтному пути
     * @param absolutePath - абсолютный путь к файлу
     * @return - содержимое файла
     * @throws IOException - исключение при неудачном чтении файла
     */
    private byte[] getContent(String absolutePath) throws IOException {
        try(val contentStream = new FileInputStream(absolutePath)) {
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

    private String createEntryName(String fileName, Integer index) {
        return String.format("%s(%s).%s", getFileNameWithoutExtension(fileName), index, getFileExtension(fileName));
    }

    /**
     * Получить имя файла без расширения по его имени
     * @param fileName - имя файла
     * @return - стркоа с именем файла
     */
    private String getFileNameWithoutExtension(String fileName) {
        return Optional.ofNullable(fileName)
                .filter(f -> f.contains("."))
                .map(f -> f.substring(0, fileName.lastIndexOf(".") - 1)).orElse("");
    }

    /**
     * Получить расширение файла по его имени
     * @param fileName - имя файла
     * @return - стркоа с расширением
     */
    private String getFileExtension(String fileName) {
        return Optional.ofNullable(fileName)
                .filter(f -> f.contains("."))
                .map(f -> f.substring(fileName.lastIndexOf(".") + 1)).orElse("");
    }

    /**
     * Сохранение файла на файловую систему
     * @param absolutePath - абсолютный путь к файлу
     * @param content - содержимое файла
     * @throws IOException - исключение при неудачной записи файла
     */
    private void saveFileIntoStorageDirectory(String absolutePath, byte[] content) throws IOException {
        try (FileOutputStream fileStream = new FileOutputStream(absolutePath)) {
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
