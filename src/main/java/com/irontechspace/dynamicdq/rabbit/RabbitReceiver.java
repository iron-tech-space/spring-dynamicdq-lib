package com.irontechspace.dynamicdq.rabbit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.irontechspace.dynamicdq.executor.ExecutorService;
import com.irontechspace.dynamicdq.rabbit.model.RabbitNotification;
import com.irontechspace.dynamicdq.rabbit.model.RabbitTask;
import lombok.extern.log4j.Log4j2;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Log4j2
@Component
public class RabbitReceiver {

    @Autowired
    RabbitService service;

    public RabbitReceiver() {
    }

    @RabbitListener(queues = "${spring.rabbitmq.queues.tasks}")
    public void receiveTasks(RabbitTask task) {
        log.info("Received task:\t[{}]", task.toString());
        service.executeTask(task, 0);
    }

    @RabbitListener(queues = "${spring.rabbitmq.queues.notifications}")
    public void receiveTasks(JsonNode notification) {
        try {
            log.info("Received notification:\t[{}]", notification.toString());
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private long millis(long nanos) {
        return nanos / 1000000L;
    }

    private long nanos(long nanos) {
        return nanos % 1000000L;
    }
}