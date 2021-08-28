package com.irontechspace.dynamicdq.configurator.save;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.irontechspace.dynamicdq.configurator.core.ConfigService;
import com.irontechspace.dynamicdq.configurator.save.model.SaveConfig;
import com.irontechspace.dynamicdq.configurator.save.SaveConfigRepository;
import com.irontechspace.dynamicdq.configurator.save.model.SaveField;
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
        log.info("#> Save Config Service init saveConfig");
        saveConfig.setTableName("dynamicdq.save_configs");
        saveConfig.getFields().add(SaveField.builder().name("logic").type("text").build());
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
