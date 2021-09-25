package com.irontechspace.dynamicdq.executor.task.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class TaskConfig {

    private String id;

    // Тип задачи для правильного выбора сервиса конфигураций
    private TaskType typeExecutor;
    // For execute config
    private String configName;
    private JsonNode body;
    private JsonNode pageable;

    private String output;

//    private TaskConfigEvents events;
    private JsonNode events;
}
