package com.irontechspace.dynamicdq.rabbit;

import com.fasterxml.jackson.databind.JsonNode;
import com.irontechspace.dynamicdq.executor.task.TaskService;
import com.irontechspace.dynamicdq.executor.task.model.Task;
import lombok.extern.log4j.Log4j2;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Log4j2
@Component
public class RabbitReceiver {

    @Autowired
    TaskService taskService;

    public RabbitReceiver() {
    }

    @RabbitListener(queues = "${spring.rabbitmq.queues.tasks}")
    public void receiveTasks(Task task) {
        log.info("Received task:\t{}", task.toString());
        taskService.executeTask(task);
    }

    @RabbitListener(queues = "${spring.rabbitmq.queues.notifications}")
    public void receiveTasks(JsonNode notification) {
        try {
            log.info("Received notification:\t{}", notification.toString());
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