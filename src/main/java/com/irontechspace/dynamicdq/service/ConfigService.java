package com.irontechspace.dynamicdq.service;

import com.irontechspace.dynamicdq.exceptions.ForbiddenException;
import com.irontechspace.dynamicdq.model.Config;
import com.irontechspace.dynamicdq.repository.Base.IConfigRepository;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
public class ConfigService<T extends Config> {

    private List<T> configs;

    IConfigRepository<T> IConfigRepository;

    public ConfigService (IConfigRepository<T> repository) {
        IConfigRepository = repository;
    }

    public void loadConfigs(String msg){
        loadConfigs();
        log.info("#> {} [{}]", msg, configs.size());
    }
    private void loadConfigs(){
        configs = IConfigRepository.getAll();
    }

    public List<T> getAll(UUID userId, List<String> userRoles){
        return configs.stream().filter(queryConfig -> checkConfig(queryConfig, userId, userRoles)).collect(Collectors.toList());
    }

    public T getByName(String configName, UUID userId, List<String> userRoles){
        for (T queryConfig : configs)
            if(queryConfig.getConfigName().equals(configName))
                if(checkConfig(queryConfig, userId, userRoles))
                    return queryConfig;
                else
                    throw new ForbiddenException("Конфигурация недоступна");

        throw new ForbiddenException("Конфигурация не найдена");
    }

    public T save(T config){
        T savedQueryConfig = IConfigRepository.save(config);
        loadConfigs();
        return savedQueryConfig;
    }

    public void delete(UUID configId){
        IConfigRepository.delete(configId);
        loadConfigs();
    }

    private Boolean checkConfig(T queryConfig, UUID userId, List<String> userRoles){
        return  // Проверка на владельца
                queryConfig.getUserId().equals(userId)
                // Проверка на доступ по пользователю
                || (queryConfig.getSharedForUsers() != null && queryConfig.getSharedForUsers().contains(userId.toString()))
                // Проверка на доступ по роли
                || userRoles.stream().anyMatch(role -> queryConfig.getSharedForRoles() != null && queryConfig.getSharedForRoles().contains(role));
    }
}
