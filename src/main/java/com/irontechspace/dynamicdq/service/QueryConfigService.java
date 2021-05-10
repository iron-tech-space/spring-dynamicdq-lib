package com.irontechspace.dynamicdq.service;

import com.irontechspace.dynamicdq.model.Query.QueryConfig;
import com.irontechspace.dynamicdq.repository.QueryConfigRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Log4j2
@Service
public class QueryConfigService {

    private final ConfigService<QueryConfig> configService;

    @Autowired
    public QueryConfigService(QueryConfigRepository queryConfigRepository){
        this.configService = new ConfigService(queryConfigRepository);
    }

    @Scheduled(fixedRate = 5000)
    private void loadConfigs(){
        configService.loadConfigs("Query configs:");
    }

    public List<QueryConfig> getAll(UUID userId, List<String> userRoles){
        return configService.getAll(userId, userRoles);
    }

    public QueryConfig getByName(String configName, UUID userId, List<String> userRoles){
        return configService.getByName(configName, userId, userRoles);
    }

    public QueryConfig save(QueryConfig config){
        return configService.save(config);
    }

    public void delete(UUID configId){
        configService.delete(configId);
    }
}
