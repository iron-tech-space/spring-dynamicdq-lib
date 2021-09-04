package com.irontechspace.dynamicdq.executor.task;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.irontechspace.dynamicdq.executor.events.SystemEventsService;
import com.irontechspace.dynamicdq.executor.task.model.*;
import com.irontechspace.dynamicdq.executor.query.QueryService;
import com.irontechspace.dynamicdq.executor.save.SaveService;
import com.irontechspace.dynamicdq.rabbit.RabbitSender;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Log4j2
@Service
public class TaskService {

    private final static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    QueryService queryService;

    @Autowired
    SaveService saveService;

    @Autowired
    RabbitSender rabbitSender;

    @Autowired
    SimpleTaskConfigs simpleTaskConfigs;

    @Autowired
    TaskUtils taskUtils;

    @Autowired
    SystemEventsService systemEventsService;

    public Object executeTask(Task task) {

//        Map<String, Object> outputData = new HashMap<>();
        ObjectNode outputData = OBJECT_MAPPER.createObjectNode();
        for (int i = 0; i < task.getConfigs().size(); i++) {

            // Init config params
            TaskConfig config = task.getConfigs().get(i);
            TaskConfigEvents events = config.getEvents();

            try {
                // Init body and pageable
                JsonNode body = taskUtils.resolveOutputData(config.getBody(), outputData);
                Pageable pageable = taskUtils.jsonToPageable(config.getPageable());

                // START Event
                if (events != null) executeEvent("start", task.getUserId(), events.getStart(), body);
                log.info("Execute task type: [{}]", config.getTypeExecutor());

                // Любой конфиг кроме выходного (branch пока исключен совсем)
                if (config.getTypeExecutor() != TaskType.output) {
                    Object res = executeConfig(config.getTypeExecutor(), config.getConfigName(), task.getUserId(), task.getUserRoles(), body, pageable);
                    if (config.getOutput() != null)
                        taskUtils.setOutputData(config.getOutput().split("\\."), outputData, OBJECT_MAPPER.valueToTree(res));
                }

                // FINISH Event
                if (events != null) executeEvent("finish", task.getUserId(), events.getFinish(), body);

                // Выходной конфиг
                if (config.getTypeExecutor() == TaskType.output) return body;
            } catch (Exception e) {
                e.printStackTrace();
                if (events != null)
                    executeEvent("error", task.getUserId(), events.getError(), OBJECT_MAPPER.createObjectNode().put("error", e.getMessage()));
            }
        }
        return null;
    }
// IF NEED REVERT BRANCH
//                String nextConfig = null;
//                if(nextConfig != null && !nextConfig.equals(config.getId()))
//                    continue;
//                if(config.getTaskType() == TaskType.branch) {
//                    JsonNode output = OBJECT_MAPPER.readTree(config.getOutput());
//                    if(body.get("condition").asBoolean()){
//                        nextConfig = output.get("true").asText();
//                    } else {
//                        nextConfig = output.get("false").asText();
//                    }
//                }

    public Object executeConfig(TaskType type, String configName, UUID userId, List<String> userRoles, JsonNode body, Pageable pageable) {
        switch (type){
            case flat:
                return queryService.getFlatData(configName, userId, userRoles, body, pageable);
            case hierarchical:
                return queryService.getHierarchicalData(configName, userId, userRoles, body, pageable);
            case count:
                return queryService.getFlatDataCount(configName, userId, userRoles, body, pageable);
            case object:
                return queryService.getObject(configName, userId, userRoles, body, pageable);
            case sql:
                return queryService.getSql(configName, userId, userRoles, body, pageable);
            case sqlCount:
                return queryService.getSqlCount(configName, userId, userRoles, body, pageable);
            case save:
                return saveService.saveData(configName, userId, userRoles, body);
            case queue: return rabbitSender.send(configName, body);
            case equal: return simpleTaskConfigs.compare(TaskType.equal, body);
            case notEqual: return simpleTaskConfigs.compare(TaskType.notEqual, body);
            case greaterEqual: return simpleTaskConfigs.compare(TaskType.greaterEqual, body);
            case lessEqual: return simpleTaskConfigs.compare(TaskType.lessEqual, body);
            case greater: return simpleTaskConfigs.compare(TaskType.greater, body);
            case less: return simpleTaskConfigs.compare(TaskType.less, body);
            case log: return simpleTaskConfigs.log(body);
            case event: return executeEvent(userId, body);
            default:
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ошибка запроса. Указан не существующий execute type");
        }
    }

    private Object executeEvent(String type, UUID userId, TaskConfigEvent event, JsonNode data){
        if(event != null)
            log.info("Push {} event: {}", type, systemEventsService.pushEvent(userId, event.getId(), event.getDataTemplate(), data));
        return null;
    }

    private Object executeEvent(UUID userId, JsonNode body){
        if(body != null)
            log.info("Push {} event: {}", "task", systemEventsService.pushEvent(userId, UUID.fromString(body.get("id").asText()), body.get("dataTemplate")));
        return null;
    }
}
