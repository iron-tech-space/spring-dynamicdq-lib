package com.irontechspace.dynamicdq.rabbit.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.irontechspace.dynamicdq.executor.ExecutorType;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@ToString
public class RabbitTaskConfig {

    private String id;

    // Тип задачи для привильного выбора сервиса конфигураций
    private ExecutorType executorType;
    // For execute config
    private String configName;
    private JsonNode body;
    private JsonNode pageable;

    private String output;
}
