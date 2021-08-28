package com.irontechspace.dynamicdq.rabbit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.irontechspace.dynamicdq.executor.ExecutorService;
import com.irontechspace.dynamicdq.executor.ExecutorType;
import com.irontechspace.dynamicdq.rabbit.model.RabbitTask;
import com.irontechspace.dynamicdq.rabbit.model.RabbitTaskConfig;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Log4j2
@Service
public class RabbitService {

    @Autowired
    ExecutorService executorService;

    public void executeTask(RabbitTask task){
        try {
            String nextConfig = null;
            Map<String, Object> outputData = new HashMap<>();
            for (int i = 0; i < task.getConfigs().size(); i++){


                RabbitTaskConfig config = task.getConfigs().get(i);
                if(nextConfig != null && !nextConfig.equals(config.getId()))
                    continue;;

                Pageable pageable = PageRequest.of(0, 50);;
                if(config.getPageable() != null) {
                    if(config.getPageable().get("sort") != null) {
                        String[] sort = config.getPageable().get("sort").asText().split(",", 2);
                        Sort sortBy = Sort.by(new Sort.Order(Sort.Direction.fromString(sort[1]), sort[0]));
                        pageable = PageRequest.of(config.getPageable().get("page").asInt(), config.getPageable().get("size").asInt(), sortBy);
                    } else if(config.getPageable().get("page") != null && config.getPageable().get("size") != null){
                        pageable = PageRequest.of(config.getPageable().get("page").asInt(), config.getPageable().get("size").asInt());
                    }
                }
                ObjectNode body = (ObjectNode) config.getBody();
                replace(body, outputData);
                log.info("Execute task type: [{}] body {}", config.getExecutorType(), body);

                if(config.getExecutorType() == ExecutorType.branch) {
                    JsonNode output = new ObjectMapper().readTree(config.getOutput());
                    if(body.get("condition").asBoolean()){
                        nextConfig = output.get("true").asText();
                    } else {
                        nextConfig = output.get("false").asText();
                    }
                } else {
                    Object res = executorService.executeConfig(config.getExecutorType(), config.getConfigName(), task.getUserId(), task.getUserRoles(), body, pageable);
                    if(config.getOutput() != null) outputData.put(config.getOutput(), res);
//                    log.info("Execute task res {}", res);
                }
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private void replace(JsonNode body, Map<String, Object> outputData) {
        body.fields().forEachRemaining(b -> {
            if(b.getValue().getNodeType() == JsonNodeType.STRING){
                String value = b.getValue().asText();
                if(outputData.containsKey(value)){
//                    log.info("replace [{}]", value);
                    b.setValue(new ObjectMapper().valueToTree(outputData.get(value)));
                }
            }
            if(b.getValue().getNodeType() == JsonNodeType.OBJECT){
//                log.info("call recursive replace");
                replace(b.getValue(), outputData);
            }
        });
    }
}