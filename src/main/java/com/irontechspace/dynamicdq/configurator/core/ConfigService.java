package com.irontechspace.dynamicdq.configurator.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.irontechspace.dynamicdq.configurator.save.model.SaveField;
import com.irontechspace.dynamicdq.configurator.save.model.SaveLogic;
import com.irontechspace.dynamicdq.exceptions.ForbiddenException;
import com.irontechspace.dynamicdq.exceptions.NotFoundException;
import com.irontechspace.dynamicdq.configurator.core.model.Config;
import com.irontechspace.dynamicdq.configurator.core.repository.IConfigRepository;
import com.irontechspace.dynamicdq.executor.save.SaveService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.irontechspace.dynamicdq.utils.Auth.DEFAULT_USER_ID;
import static com.irontechspace.dynamicdq.utils.Auth.DEFAULT_USER_ROLE;

@Log4j2
public class ConfigService<T extends Config> {

    private final static ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final IConfigRepository<T> IConfigRepository;
    private List<T> configs;

    @Value("${dynamicdq.caching-configs.enabled:false}")
    public Boolean cachingConfigs;

    @Autowired
    private SaveService saveService;

    public SaveLogic saveConfig;

    public ConfigService (IConfigRepository<T> repository) {
        IConfigRepository = repository;
        List<SaveField> fields = new ArrayList<>();
        fields.add(SaveField.builder().name("id").type("uuid").build());
        fields.add(SaveField.builder().name("parentId").type("uuid").build());
        fields.add(SaveField.builder().name("isGroup").type("bool").build());
        fields.add(SaveField.builder().name("userId").type("uuid").build());
        fields.add(SaveField.builder().name("configName").type("text").build());
        fields.add(SaveField.builder().name("description").type("text").build());
        fields.add(SaveField.builder().name("position").type("int").build());
        fields.add(SaveField.builder().name("sharedForUsers").type("text").build());
        fields.add(SaveField.builder().name("sharedForRoles").type("text").build());
        fields.add(SaveField.builder().name("loggingQueries").type("bool").build());
        saveConfig = SaveLogic.builder()
                .fieldType("root")
                .primaryKey("id")
                .excludePrimaryKey(false)
                .autoGenerateCode(false)
                .fields(fields).build();
    }

    protected void loadConfigs(String msg){
        loadConfigs();
        log.info("#> {} [{}]", msg, configs.size());
    }

    private void loadConfigs(){
        configs = IConfigRepository.getAll();
    }

    /**
     * Получить конфигурации из КЭША с фильтрацией по userId и userRoles
     * @param userId ид пользователя для фильтрации конфигураций
     * @param userRoles роли пользователя для фильтрации конфигураций
     * @return список конфигураций
     */
    public List<T> getAll(UUID userId, List<String> userRoles){
        if(cachingConfigs)
            return getAll(configs, userId, userRoles);
        else
            return getAll(IConfigRepository.getAll(), userId, userRoles);

    }

    /**
     * Получить конфигурации из БД по dateSource с фильтрацией по userId и userRoles
     * @param dateSource подключение к БД
     * @param userId ид пользователя для фильтрации конфигураций
     * @param userRoles роли пользователя для фильтрации конфигураций
     * @return список конфигураций
     */
    public List<T> getAll(DataSource dateSource, UUID userId, List<String> userRoles){
        return getAll(IConfigRepository.getAll(new NamedParameterJdbcTemplate(dateSource)), userId, userRoles);
    }

    /**
     * Получить конфигурацию из КЭША
     * @param configName имя конфигурации для поиска
     * @param userId ид пользователя для фильтрации конфигураций
     * @param userRoles роли пользователя для фильтрации конфигураций
     * @return конфигурация
     */
    public T getByName(String configName, UUID userId, List<String> userRoles){
        if(cachingConfigs)
            return getByName(configs, configName, userId, userRoles);
        else
            return getByName(IConfigRepository.getAll(), configName, userId, userRoles);
    }

    /**
     * Получить конфигурацию из указанной БД
     * @param dateSource подключение к БД
     * @param configName имя конфигурации для поиска
     * @param userId ид пользователя для фильтрации конфигураций
     * @param userRoles роли пользователя для фильтрации конфигураций
     * @return конфигурация
     */
    public T getByName(DataSource dateSource, String configName, UUID userId, List<String> userRoles){
        return getByName(IConfigRepository.getAll(new NamedParameterJdbcTemplate(dateSource)), configName, userId, userRoles);
    }

    public void savePosition(T config){
        IConfigRepository.savePosition(config);
    }

    public void savePosition(DataSource dateSource, T config){
        IConfigRepository.savePosition(new NamedParameterJdbcTemplate(dateSource), config);
    }

    /**
     * Сохранить конфигурацию в БД по умолчанию
     * @param config объект конфигурации
     * @return сохраненная конфигурация
     */
    public T save(T config){
        T savedQueryConfig = save(null, config); //IConfigRepository.save(config);
        loadConfigs();
        return savedQueryConfig;
    }

    /**
     * Сохранить конфигурацию в указанную БД
     * @param dateSource подключение к БД
     * @param config объект конфигурации
     * @return сохраненная конфигурация
     */
    public T save(DataSource dateSource, T config){
//        T savedQueryConfig = IConfigRepository.save(new NamedParameterJdbcTemplate(dateSource), config);
        if(config.getUserId() == null)
            config.setUserId(DEFAULT_USER_ID);
        UUID id = (UUID) saveService.analysisLogic(dateSource, saveConfig, OBJECT_MAPPER.valueToTree(config), null, config.getUserId(), DEFAULT_USER_ROLE, true);
        config.setId(id);
        return config;
    }

    /**
     * Удаление конфигурации в БД по умолчанию
     * @param configId объект конфигурации
     */
    public void delete(UUID configId){
        IConfigRepository.delete(configId);
        loadConfigs();
    }

    /**
     * даление конфигурации в указанной БД
     * @param dateSource подключение к БД
     * @param configId объект конфигурации
     */
    public void delete(DataSource dateSource, UUID configId){
        IConfigRepository.delete(new NamedParameterJdbcTemplate(dateSource), configId);
//        loadConfigs();
    }

    /** Фильтрация списка конфигураций по userId и userRoles */
    private List<T> getAll(List<T> configs, UUID userId, List<String> userRoles) {
        return configs
                .stream()
                .filter(queryConfig -> checkConfig(queryConfig, userId, userRoles))
                .collect(Collectors.toList());
    }

    /** Поиск в списке конфигураций по configName, userId и userRoles */
    private T getByName(List<T> configs, String configName, UUID userId, List<String> userRoles){
        for (T queryConfig : configs)
            if(queryConfig.getConfigName().equals(configName))
                if(checkConfig(queryConfig, userId, userRoles))
                    return queryConfig;
                else
                    throw new ForbiddenException(String.format("Конфигурация [%s] недоступна", configName));

        throw new NotFoundException(String.format("Конфигурация [%s] не найдена", configName));
    }

    /** Проверка достума к конфигурации по userId и userRoles  */
    private Boolean checkConfig(T queryConfig, UUID userId, List<String> userRoles){
        return  // Проверка на владельца
                queryConfig.getUserId().equals(userId)
                // Проверка на доступ по пользователю
                || (queryConfig.getSharedForUsers() != null && queryConfig.getSharedForUsers().contains(userId.toString()))
                // Проверка на доступ по роли
                || userRoles.stream().anyMatch(role -> queryConfig.getSharedForRoles() != null && queryConfig.getSharedForRoles().contains(role));
    }
}
