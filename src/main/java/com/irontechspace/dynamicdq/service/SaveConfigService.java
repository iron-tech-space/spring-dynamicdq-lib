package com.irontechspace.dynamicdq.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.irontechspace.dynamicdq.model.Save.SaveConfig;
import com.irontechspace.dynamicdq.repository.SaveConfigRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.List;

@Log4j2
@Service
public class SaveConfigService extends ConfigService<SaveConfig> {

    private final SaveConfigRepository saveConfigRepository;

    public SaveConfigService(SaveConfigRepository saveConfigRepository) {
        super(saveConfigRepository);
        this.saveConfigRepository = saveConfigRepository;
    }

    public List<String> getDbTables(DataSource dateSource) {
        return saveConfigRepository.getDbTables(new NamedParameterJdbcTemplate(dateSource));
    }

    public List<ObjectNode> getDbFieldsByTable(DataSource dateSource, String tableName) {
        return saveConfigRepository.getDbFieldsByTable(new NamedParameterJdbcTemplate(dateSource), tableName);
    }

    @Scheduled(fixedRateString = "${dynamicdq.caching-configs.delay:5000}")
    private void loadConfigs(){
        if(cachingConfigs)
            loadConfigs("Save configs:");
        else
            log.info("#> NO load save configs");
    }
}
