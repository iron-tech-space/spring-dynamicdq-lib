package com.irontechspace.dynamicdq.service;

import com.irontechspace.dynamicdq.model.Save.SaveConfig;
import com.irontechspace.dynamicdq.repository.SaveConfigRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Log4j2
@Service
public class SaveConfigService {

    private final ConfigService<SaveConfig> configService;

    @Autowired
    public SaveConfigService(SaveConfigRepository saveConfigRepository){
        this.configService = new ConfigService(saveConfigRepository);
    }

    @Scheduled(fixedRate = 5000)
    private void loadConfigs(){
        configService.loadConfigs("Save configs:");
    }

    public List<SaveConfig> getAll(UUID userId, List<String> userRoles){
        return configService.getAll(userId, userRoles);
    }

    public SaveConfig getByName(String configName, UUID userId, List<String> userRoles){
        return configService.getByName(configName, userId, userRoles);
    }

    public SaveConfig save(SaveConfig config){
        return configService.save(config);
    }

    public void delete(UUID configId){
        configService.delete(configId);
    }
}
