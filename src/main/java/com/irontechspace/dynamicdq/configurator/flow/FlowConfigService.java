package com.irontechspace.dynamicdq.configurator.flow;

import com.irontechspace.dynamicdq.configurator.core.ConfigService;
import com.irontechspace.dynamicdq.configurator.flow.model.FlowConfig;
import com.irontechspace.dynamicdq.configurator.save.model.SaveField;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public class FlowConfigService extends ConfigService<FlowConfig> {

//    private final FlowConfigRepository flowConfigRepository;

    public FlowConfigService(FlowConfigRepository flowConfigRepository) {
        super(flowConfigRepository);
//        this.flowConfigRepository = flowConfigRepository;
        log.info("#> Flow Config Service init saveConfig");
        saveConfig.setTableName("dynamicdq.flow_configs");
        saveConfig.getFields().add(SaveField.builder().name("execDiagram").type("text").build());
        saveConfig.getFields().add(SaveField.builder().name("uiDiagram").type("text").build());
    }

    @Scheduled(fixedRateString = "${dynamicdq.caching-configs.delay:5000}")
    private void loadConfigs(){
        if(cachingConfigs)
            loadConfigs("Flow configs:");
        else
            log.info("#> NO load flow configs");
    }
}
