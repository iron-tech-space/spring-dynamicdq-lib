package com.irontechspace.dynamicdq.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.irontechspace.dynamicdq.exceptions.ForbiddenException;
import com.irontechspace.dynamicdq.exceptions.NotFoundException;
import com.irontechspace.dynamicdq.model.Config;
import com.irontechspace.dynamicdq.model.Query.QueryConfig;
import com.irontechspace.dynamicdq.repository.Base.IConfigRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
public class ConfigService<T extends Config> {

    @Value("${dynamicdq.caching-configs.enabled:false}")
    public Boolean cachingConfigs;

    private List<T> configs;

    IConfigRepository<T> IConfigRepository;

    public ConfigService (IConfigRepository<T> repository) {
        IConfigRepository = repository;
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
        T savedQueryConfig = IConfigRepository.save(config);
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
        T savedQueryConfig = IConfigRepository.save(new NamedParameterJdbcTemplate(dateSource), config);
        loadConfigs();
        return savedQueryConfig;
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
        loadConfigs();
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
                    throw new ForbiddenException("Конфигурация недоступна");

        throw new NotFoundException("Конфигурация не найдена");
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
