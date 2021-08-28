package com.irontechspace.dynamicdq.executor;

import com.fasterxml.jackson.databind.JsonNode;
import com.irontechspace.dynamicdq.executor.query.QueryService;
import com.irontechspace.dynamicdq.executor.save.SaveService;
import com.irontechspace.dynamicdq.rabbit.RabbitSender;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Log4j2
@Service
public class ExecutorService {

    @Autowired
    QueryService queryService;

    @Autowired
    SaveService saveService;

    @Autowired
    RabbitSender rabbitSender;

    public <T> T executeConfig(ExecutorType type, String configName, UUID userId, List<String> userRoles, JsonNode body, Pageable pageable) {
        switch (type){
            case flat:
                return (T) queryService.getFlatData(configName, userId, userRoles, body, pageable);
            case hierarchical:
                return (T) queryService.getHierarchicalData(configName, userId, userRoles, body, pageable);
            case count:
                return (T) queryService.getFlatDataCount(configName, userId, userRoles, body, pageable);
            case object:
                return (T) queryService.getObject(configName, userId, userRoles, body, pageable);
            case sql:
                return (T) queryService.getSql(configName, userId, userRoles, body, pageable);
            case sqlCount:
                return (T) queryService.getSqlCount(configName, userId, userRoles, body, pageable);
            case save:
                return (T) saveService.saveData(configName, userId, userRoles, body);
            case queue:
                return (T) rabbitSender.send(configName, body);
            case equal:
                return (T) compare("equal", body);
            case not_equal:
                return (T) compare("not_equal", body);
            case greater_equal:
                return (T) compare("greater_equal", body);
            case less_equal:
                return (T) compare("less_equal", body);
            case greater:
                return (T) compare("greater", body);
            case less:
                return (T) compare("less", body);
            case log:
                return (T) log(body);
            default:
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ошибка запроса. Указан не существующий execute type");
        }
    }

    private Object log(JsonNode body) {
        log.info(body);
        return null;
    }

    private Boolean compare(String operation, JsonNode body) {
        String type = body.get("type").asText();
        // log.info("Equal type: [{}]", type);
        switch (type){
            case "text": return compareString(operation, body.get("leftCondition").asText(), body.get("rightCondition").asText());
            case "bool": return compareBoolean(operation, body.get("leftCondition").asBoolean(), body.get("rightCondition").asBoolean());
            case "int":  return compareInt(operation, body.get("leftCondition").asInt(), body.get("rightCondition").asInt());
            case "double": return compareDouble(operation, body.get("leftCondition").asDouble(), body.get("rightCondition").asDouble());
            default: return false;
        }
    }

    private Boolean compareString(String operation, String leftCondition, String rightCondition) {
        switch (operation) {
            case "equal": return leftCondition.equals(rightCondition);
            case "not_equal": return !leftCondition.equals(rightCondition);
            default: return false;
        }
    }

    private Boolean compareBoolean(String operation, boolean leftCondition, boolean rightCondition) {
        switch (operation) {
            case "equal": return leftCondition == rightCondition;
            case "not_equal": return leftCondition != rightCondition;
            default: return false;
        }
    }

    private Boolean compareInt(String operation, int leftCondition, int rightCondition) {
        return compareNumber(operation, leftCondition, rightCondition);
    }

    private Boolean compareDouble(String operation, double leftCondition, double rightCondition) {
        return compareNumber(operation, leftCondition, rightCondition);
    }

    private Boolean compareNumber(String operation, double leftCondition, double rightCondition) {
        switch (operation) {
            case "equal": return leftCondition == rightCondition;
            case "not_equal": return leftCondition != rightCondition;
            case "greater_equal": return leftCondition >= rightCondition;
            case "less_equal": return leftCondition <= rightCondition;
            case "greater": return leftCondition > rightCondition;
            case "less": return leftCondition < rightCondition;
            default: return false;
        }
    }


}
