package com.irontechspace.dynamicdq.executor.task;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.irontechspace.dynamicdq.executor.task.model.TaskType;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Map;

@Log4j2
@Service
public class TaskUtils {

    private final static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public JsonNode resolveOutputData(JsonNode body, Map<String, Object> outputData) {
        if(body.getNodeType() == JsonNodeType.STRING){
            String value = body.asText();
            if(outputData.containsKey(value)){
                return new ObjectMapper().valueToTree(outputData.get(value));
            }
        } else if (body.isObject()) {
            ObjectNode newBody = (ObjectNode) body;
            newBody.fields().forEachRemaining(b -> {
                if (b.getValue().isObject()){
                    resolveOutputData(b.getValue(), outputData);
                }
                else if (b.getValue().getNodeType() == JsonNodeType.STRING) {
                    String value = b.getValue().asText();
                    if (outputData.containsKey(value)) {
                        // log.info("replace [{}]", value);
                        b.setValue(new ObjectMapper().valueToTree(outputData.get(value)));
                    }
                }
            });
            return newBody;
        }
        return body;
    }

    public JsonNode pageableToJson (Pageable pageable){
        ObjectNode jsonPageable = OBJECT_MAPPER.createObjectNode();
        jsonPageable.put("page", pageable.getPageNumber());
        jsonPageable.put("size", pageable.getPageSize());
        jsonPageable.put("sort", pageable.getSort().isUnsorted() ? null : pageable.getSort().toString());
        return jsonPageable;
    }

    public Pageable jsonToPageable (JsonNode jsonPageable){
        Pageable pageable = PageRequest.of(0, 50);;
//        if(config.getPageable() != null) {
        if(jsonPageable != null) {
            if(jsonPageable.get("sort") != null && !jsonPageable.get("sort").isNull()) {
                String[] sort = jsonPageable.get("sort").asText().split(",", 2);
                Sort sortBy = Sort.by(new Sort.Order(Sort.Direction.fromString(sort[1]), sort[0]));
                pageable = PageRequest.of(jsonPageable.get("page").asInt(), jsonPageable.get("size").asInt(), sortBy);
            } else if(jsonPageable.get("page") != null && !jsonPageable.get("page").isNull()
                    && jsonPageable.get("size") != null && !jsonPageable.get("size").isNull() ){
                pageable = PageRequest.of(jsonPageable.get("page").asInt(), jsonPageable.get("size").asInt());
            }
        }
        return pageable;
    }

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
