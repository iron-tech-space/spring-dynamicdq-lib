package com.irontechspace.dynamicdq.executor.task.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class TaskConfigEvent {
    private UUID id;
    private JsonNode dataTemplate;
}
