package com.irontechspace.dynamicdq.executor.task.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;

@Getter
@Setter
public class TaskConfigEvents {
    private TaskConfigEvent start;
    private TaskConfigEvent finish;
    private TaskConfigEvent error;
}
