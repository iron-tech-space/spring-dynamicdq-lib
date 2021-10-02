package com.irontechspace.dynamicdq.executor.events;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@Log4j2
@Service
public class SystemEventsService {

    @Value("${dynamicdq.caching-configs.enabled:false}")
    public Boolean cachingConfigs;

    @Autowired
    SystemEventsRepository systemEventsRepository;

    private List<SystemEventType> eventTypes;

    @Scheduled(fixedRateString = "${dynamicdq.caching-configs.delay:5000}")
    private void loadEventTypes(){
        if(cachingConfigs) {
            eventTypes = systemEventsRepository.getAll();
            log.info("#> {} [{}]", "System event types: ", eventTypes.size());
        } else
            log.info("#> NO load save configs");
    }

    public String pushEvent(UUID userId, UUID typeId, JsonNode dataTemplate, JsonNode data) {
        return pushEvent(null, userId, typeId, dataTemplate, data);

    }
    public String pushEvent(DataSource ds, UUID userId, UUID typeId, JsonNode dataTemplate, JsonNode data) {
        SystemEventType type = eventTypes.stream().filter(et -> et.getId().equals(typeId)).findFirst().orElse(null);
        if(type != null){
            Map<String, String> templateValues = new HashMap<>();
            if (dataTemplate.isObject()) {
                dataTemplate.fields().forEachRemaining(field -> {
                    if (field.getValue().getNodeType() == JsonNodeType.STRING) {
                        templateValues.put(field.getKey(),
                                findValueByPath(field.getValue().asText().split("\\."), data));
                    }
                });
            }
            String textEvent = type.getTemplate();
            for(String key : templateValues.keySet()){
//                log.info("Push event key: [{}] value: [{}]", key, templateValues.get(key));
                if(templateValues.get(key) == null)
                    textEvent = textEvent.replaceAll(":" + key, "");
                else
                    textEvent = textEvent.replaceAll(":" + key, templateValues.get(key));
            }
//            log.info("Push event textEvent: [{}]", textEvent);
            if(ds == null) systemEventsRepository.insert(userId, typeId, textEvent);
            else systemEventsRepository.insert(new NamedParameterJdbcTemplate(ds), userId, typeId, textEvent);
            return textEvent;
        }
        return null;
    }

    public String pushEvent(UUID userId, UUID typeId, JsonNode dataTemplate) {
        SystemEventType type = eventTypes.stream().filter(et -> et.getId().equals(typeId)).findFirst().orElse(null);
        if(dataTemplate != null && dataTemplate.isObject()){
            AtomicReference<String> textEvent = new AtomicReference<>();
            textEvent.set(type.getTemplate());
            dataTemplate.fields().forEachRemaining(field -> {
                textEvent.set(textEvent.get().replaceAll(":" + field.getKey(), field.getValue().asText()));
            });
//            log.info("Push event textEvent: [{}]", textEvent);
            systemEventsRepository.insert(userId, typeId, textEvent.get());
            return textEvent.get();
        }
        return null;
    }

    private String findValueByPath(String[] path, JsonNode data) {
        if (data.isObject() && path.length > 0) {
            AtomicReference<String> res = new AtomicReference<>();
            data.fields().forEachRemaining(field -> {
                if(field.getKey().equals(path[0])){
                    if(path.length > 1) {
                        res.set(findValueByPath(Arrays.copyOfRange(path, 1, path.length), field.getValue()));
                    } else {
                        res.set(field.getValue().asText());
                    }
                }
            });
            return res.get();
        } else {
            return null;
        }
    }
}
