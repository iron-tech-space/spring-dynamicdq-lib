package com.irontechspace.dynamicdq.executor.task.model;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Task {

    // Ид задачи для логирования и возврата результата пользователю
    private UUID id;
    private UUID userId;
    private List<String> userRoles;
    private List<TaskConfig> configs;

    private TaskConfigEvents events;

    // Рекурсивная задача после выполнения текущей
//    private String resultPath;
//    private RabbitTask outputTask;

    // Положить результат в указанную очередь
//    private String outputQueue;
}
