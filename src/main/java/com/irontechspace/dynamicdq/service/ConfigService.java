package com.irontechspace.dynamicdq.service;

import com.irontechspace.dynamicdq.exceptions.ForbiddenException;
import com.irontechspace.dynamicdq.model.ConfigTable;
import com.irontechspace.dynamicdq.model.Db.Field;
import com.irontechspace.dynamicdq.repository.ConfigRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Log4j2
@Service
public class ConfigService {

    private List<ConfigTable> configs;

    @Autowired
    ConfigRepository configRepository;

    @Scheduled(fixedRate = 5000)
    private void updateConfig(){
        getConfigs();
        log.info("#> ConfigService update configs [{}]", configs.size());
    }

    @Bean
    private void initConfigs(){
        configs = getConfigs();
    }

    public List<ConfigTable> getConfigs(){
        return getConfigs(UUID.fromString("0be7f31d-3320-43db-91a5-3c44c99329ab"), Collections.singletonList("ROLE_ADMIN"));
    }

    public List<ConfigTable> getConfigs(UUID userId, List<String> userRoles){
//        log.info("Reload configs");
        return configs = configRepository.getConfigs(userId, userRoles);
    }

    public ConfigTable getConfig(String configName, UUID userId, List<String> userRoles){
        return configRepository.getConfig(configName, userId, userRoles);
    }

    public ConfigTable getConfigByConfigName(String configName, UUID userId, List<String> userRoles){
        for (ConfigTable configTable : configs)
            if(configTable.getConfigName().equals(configName)
                    && (configTable.getUserId().equals(userId) || userRoles.stream().anyMatch(role -> configTable.getSharedForRoles() != null && configTable.getSharedForRoles().contains(role))))
                return configTable;

        throw new ForbiddenException("Конфигурация недоступна");
    }

    public ConfigTable save(ConfigTable configTable){
        ConfigTable savedConfigTable = configRepository.save(configTable);
        getConfigs();
        return savedConfigTable;
    }

    public void deleteTableById(UUID tableId){
        configRepository.deleteTableById(tableId);
        getConfigs();
    }

    public void deleteFieldById(UUID fieldId){
        configRepository.deleteFieldById(fieldId);
        getConfigs();
    }

    public void deleteFieldsByTableId(UUID tableId){
        configRepository.deleteFieldsByTableId(tableId);
        getConfigs();
    }

    public List<String> getDbTables(){
        return configRepository.getDbTables();
    }

    public List<Field> getDbFieldsByTable(String tableName) {
        return configRepository.getDbFieldsByTable(tableName);
    }
}
