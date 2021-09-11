package com.irontechspace.dynamicdq.executor.task;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.irontechspace.dynamicdq.executor.task.model.TaskType;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.irontechspace.dynamicdq.exceptions.ExceptionUtils.logException;

@Log4j2
@Service
public class TaskUtils {

    private final static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public JsonNode resolveOutputData(JsonNode body, ObjectNode outputData){
        if(body.getNodeType() == JsonNodeType.STRING){
            // Если body строка, то получить значение из outputData
//            return getValueByPath(body.asText().split("\\."), outputData, body);
            return getValueByPath(body.asText(), outputData, body);
        } else if (body.isObject()) {
            ObjectNode res = (ObjectNode) body;
            // Если body объект, то переберем все поля
            res.fields().forEachRemaining(field -> {
                if (field.getValue().getNodeType() == JsonNodeType.STRING)
                    // Если строка, то получить значение из outputData
//                    field.setValue(getValueByPath(field.getValue().asText().split("\\."), outputData, field.getValue()));
                    field.setValue(getValueByPath(field.getValue().asText(), outputData, field.getValue()));
                else if (field.getValue().isObject())
                    // Если после объект, то рекурсивный запрос
                    field.setValue(resolveOutputData(field.getValue(), outputData));
                else if(field.getValue().isArray()) {
                    // Если после массив, то перебор и рекурсивный запрос
                    ArrayNode an = (ArrayNode) field.getValue();
                    for (int i = 0; i < an.size(); i++)
                        an.set(i, resolveOutputData(an.get(i), outputData));
                    field.setValue(an);
                }
            });
            return res;
        } else if(body.isArray()) {
            // Если после массив, то перебор и рекурсивный запрос
            ArrayNode an = (ArrayNode) body;
            for(int i = 0; i < an.size(); i++)
                an.set(i, resolveOutputData(an.get(i), outputData));
            return an;
        }
        return body;
    }

    public JsonNode getValueByPath(String path, JsonNode outputData, JsonNode defaultValue) {
        Matcher scriptMatcher = Pattern.compile("JS\\{(.*?)\\}JS").matcher(path);
        while (scriptMatcher.find()) {
            // Скрипт без обработки
            String rawScript = path.substring(scriptMatcher.start() + 3, scriptMatcher.end() - 3);
            // Исполняемы скрипт
            String executeScript = rawScript;
            Matcher valueMatcher = Pattern.compile("\\[(.*?)\\]").matcher(rawScript);
            ScriptEngineManager mgr = new ScriptEngineManager();
            ScriptEngine jsEngine = mgr.getEngineByName("JavaScript");
            try {
                while (valueMatcher.find()) {
                    String valuePath = rawScript.substring(valueMatcher.start() + 1, valueMatcher.end() - 1);
                    String replacePath = rawScript.substring(valueMatcher.start(), valueMatcher.end());
                    String replaceValue = OBJECT_MAPPER.writeValueAsString(getValueByPath(valuePath.split("\\."), outputData, defaultValue));
                    executeScript = executeScript.replace(replacePath, replaceValue);
                }
                Object result = jsEngine.eval(executeScript);
                path = path.replace(path.substring(scriptMatcher.start(), scriptMatcher.end()), OBJECT_MAPPER.writeValueAsString(result));
            } catch (Exception e) {
//                ex.printStackTrace();
                logException(log, e);
            }
        }
        return getValueByPath(path.split("\\."), outputData, defaultValue);
    }

    public JsonNode getValueByPath(String[] path, JsonNode outputData, JsonNode defaultValue) {
        if (path.length > 0) {
            AtomicReference<JsonNode> res = new AtomicReference<>(defaultValue);
            if (outputData.isObject()) {
                outputData.fields().forEachRemaining(field -> {
                    if (field.getKey().equals(path[0])) {
                        res.set( path.length > 1
                                ? getValueByPath(Arrays.copyOfRange(path, 1, path.length), field.getValue(), defaultValue)
                                : field.getValue() );
                    } // Инга )
                });
            } else if (outputData.isArray())
                res.set( path.length > 1
                        ? getValueByPath(Arrays.copyOfRange(path, 1, path.length), outputData.get(path[0]), defaultValue)
                        : outputData.get(Integer.parseInt(path[0])) );
            return res.get();
        } else {
            return null;
        }
    }
//    private void execJS() {

//    }

    public void setOutputData(TaskType type, String[] path, ObjectNode outputData, Object value){
        if (outputData.isObject() && path.length > 0) {
            // Ноды нет - создаем
            if(outputData.findPath(path[0]).isMissingNode())
                outputData.set(path[0], OBJECT_MAPPER.createObjectNode());

            if(path.length > 1)
                setOutputData(type, Arrays.copyOfRange(path, 1, path.length), (ObjectNode) outputData.get(path[0]), value);
            else {
                if(type == TaskType.createExcel)
                    outputData.putPOJO(path[0], value);
                else {
                    JsonNode _value = OBJECT_MAPPER.valueToTree(value);
//                    OBJECT_MAPPER.createObjectNode().put("rowIndex", startRowIndex + data.size()).put("colIndex", startColIndex + fields.size());
                    if(_value.isArray())
                        outputData.set(path[0], OBJECT_MAPPER.createObjectNode().setAll(new HashMap<String, JsonNode>() {{ put("rows", _value); put("length", OBJECT_MAPPER.valueToTree(_value.size()) ); }}) );
                    else
                        outputData.set(path[0], _value);
                }
            }
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
