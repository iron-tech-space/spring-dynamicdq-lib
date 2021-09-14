package com.irontechspace.dynamicdq.executor.task;

import com.fasterxml.jackson.databind.JsonNode;
import com.irontechspace.dynamicdq.executor.task.model.TaskType;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public class SimpleTaskConfigs {

    public Object log(JsonNode body) {
        log.info(body);
        return null;
    }

    public Boolean compare(TaskType operation, JsonNode body) {
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

    private Boolean compareString(TaskType operation, String leftCondition, String rightCondition) {
        switch (operation) {
            case equal: return leftCondition.equals(rightCondition);
            case notEqual: return !leftCondition.equals(rightCondition); //not_equal
            default: return false;
        }
    }

    private Boolean compareBoolean(TaskType operation, boolean leftCondition, boolean rightCondition) {
        switch (operation) {
            case equal: return leftCondition == rightCondition;
            case notEqual: return leftCondition != rightCondition;
            default: return false;
        }
    }

    private Boolean compareInt(TaskType operation, int leftCondition, int rightCondition) {
        return compareNumber(operation, leftCondition, rightCondition);
    }

    private Boolean compareDouble(TaskType operation, double leftCondition, double rightCondition) {
        return compareNumber(operation, leftCondition, rightCondition);
    }

    private Boolean compareNumber(TaskType operation, double leftCondition, double rightCondition) {
        switch (operation) {
            case equal: return leftCondition == rightCondition;
            case notEqual: return leftCondition != rightCondition;
            case greaterEqual: return leftCondition >= rightCondition;
            case lessEqual: return leftCondition <= rightCondition;
            case greater: return leftCondition > rightCondition;
            case less: return leftCondition < rightCondition;
            default: return false;
        }
    }
}
