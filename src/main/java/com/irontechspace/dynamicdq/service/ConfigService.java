package com.irontechspace.dynamicdq.service;

import com.irontechspace.dynamicdq.model.ConfigTable;
import com.irontechspace.dynamicdq.repository.ConfigRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

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
        log.info("inside N updateConfig");
    }

    @Bean
    private void initConfigs(){
        configs = getConfigs();
    }

    public List<ConfigTable> getConfigs(){
        return getConfigs(1L);
    }

    public List<ConfigTable> getConfigs(Long userId){
//        log.info("Reload configs");
        return configs = configRepository.getConfigs(userId);
    }

    public ConfigTable getConfig(String configName, Long userId){
        return configRepository.getConfig(configName, userId);
    }

    public ConfigTable getConfigByConfigName(String configName){
        for (ConfigTable configTable : configs)
            if(configTable.getConfigName().equals(configName))
                return configTable;
        return null;
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
}
