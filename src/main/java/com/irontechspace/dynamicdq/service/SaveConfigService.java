package com.irontechspace.dynamicdq.service;

import com.irontechspace.dynamicdq.exceptions.ForbiddenException;
import com.irontechspace.dynamicdq.model.SaveData.SaveConfig;
import com.irontechspace.dynamicdq.repository.SaveConfigRepository;
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
public class SaveConfigService {

    private List<SaveConfig> configs;

    @Autowired
    private SaveConfigRepository saveConfigRepository;

    @Scheduled(fixedRate = 5000)
    private void updateSaveConfig(){
        getConfigs();
        log.info("#> SaveConfigService update configs [{}]", configs.size());
    }

    @Bean
    private void initSaveConfigs(){
        configs = getConfigs();
    }

    public List<SaveConfig> getConfigs(){
        return getConfigs(UUID.fromString("0be7f31d-3320-43db-91a5-3c44c99329ab"), Collections.singletonList("ROLE_ADMIN"));
    }

    public List<SaveConfig> getConfigs(UUID userId, List<String> userRoles){
        return configs = saveConfigRepository.getConfigs(userId, userRoles);
    }

    public SaveConfig getConfig(String configName, UUID userId, List<String> userRoles){
        return saveConfigRepository.getConfig(configName, userId, userRoles);
    }

    public String getSaveConfigByConfigName(String configName, UUID userId, List<String> userRoles){
        for (SaveConfig saveConfig : configs)
            if(saveConfig.getConfigName().equals(configName)
                    && (saveConfig.getUserId().equals(userId) || userRoles.stream().anyMatch(role -> saveConfig.getSharedForRoles() != null && saveConfig.getSharedForRoles().contains(role))))
                return saveConfig.getLogic();

        throw new ForbiddenException("Конфигурация недоступна");
    }

    public void save(SaveConfig saveConfig){
        saveConfigRepository.save(saveConfig);
    }

    public void deleteSaveConfigById(UUID configId){
        saveConfigRepository.deleteSaveConfigById(configId);
    }
}
