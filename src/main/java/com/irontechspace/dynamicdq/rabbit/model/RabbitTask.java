package com.irontechspace.dynamicdq.rabbit.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@ToString
public class RabbitTask {

    // Ид задачи для логирования и возврата результата пользователю
    private UUID id;

    private UUID userId;
    private List<String> userRoles;

    private List<RabbitTaskConfig> configs;


    // Рекурсивная задача после выполнения текущей
//    private String resultPath;
//    private RabbitTask outputTask;

    // Положить результат в указанную очередь
//    private String outputQueue;
}
