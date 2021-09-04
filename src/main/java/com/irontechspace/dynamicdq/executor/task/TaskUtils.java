package com.irontechspace.dynamicdq.executor.task;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Log4j2
@Service
public class TaskUtils {

    private final static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public JsonNode resolveOutputData(JsonNode body, Map<String, Object> outputData) {
        if(body.getNodeType() == JsonNodeType.STRING){
            String value = body.asText();
            if(outputData.containsKey(value)){
                return OBJECT_MAPPER.valueToTree(outputData.get(value));
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
                        b.setValue(OBJECT_MAPPER.valueToTree(outputData.get(value)));
                    }
                }
            });
            return newBody;
        }
        return body;
    }

    public JsonNode resolveOutputData(JsonNode body, ObjectNode outputData) {
        if(body.getNodeType() == JsonNodeType.STRING){
            // Если body строка, то получить значение из outputData
            return getValueByPath(body.asText().split("\\."), outputData, body);
        } else if (body.isObject()) {
            // Если body объект, то переберем все поля
            body.fields().forEachRemaining(field -> {
                if (field.getValue().isObject())
                    // Если после объект, то рекурсивный запрос
                    resolveOutputData(field.getValue(), outputData);
                else if (field.getValue().getNodeType() == JsonNodeType.STRING)
                    // Если строка, то получить значение из outputData
                    field.setValue(getValueByPath(field.getValue().asText().split("\\."), outputData, field.getValue()));
            });
        }
        return body;
    }

    public JsonNode getValueByPath(String[] path, JsonNode outputData, JsonNode defaultValue) {
        if (outputData.isObject() && path.length > 0) {
            AtomicReference<JsonNode> res = new AtomicReference<>(defaultValue);
//            for(Map.Entry<String, JsonNode> field : outputData.fields()){
            outputData.fields().forEachRemaining(field -> {
                if(field.getKey().equals(path[0])){
                    if(path.length > 1) {
                        res.set(getValueByPath(Arrays.copyOfRange(path, 1, path.length), field.getValue(), defaultValue));
                    } else {
                        res.set(field.getValue());
                    }
                }
            });
            return res.get();
        } else {
            return null;
        }
    }

    public void setOutputData(String[] path, ObjectNode outputData, JsonNode value){
        if (outputData.isObject() && path.length > 0) {
            // Ноды нет - создаем
            if(outputData.findPath(path[0]).isMissingNode())
                outputData.set(path[0], OBJECT_MAPPER.createObjectNode());

            if(path.length > 1)
                setOutputData(Arrays.copyOfRange(path, 1, path.length), (ObjectNode) outputData.get(path[0]), value);
            else
                outputData.set(path[0], value);
        }
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


}
