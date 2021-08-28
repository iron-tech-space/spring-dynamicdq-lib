package com.irontechspace.dynamicdq.rabbit;

import lombok.extern.log4j.Log4j2;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public class RabbitSender {

    @Value("${spring.rabbitmq.queues.tasks}")
    String tasksQueue;

    private final RabbitTemplate rabbitTemplate;

    public RabbitSender(RabbitTemplate rabbitTemplate){
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendTask(Object task) {
        send(tasksQueue, task);
    }

    public Boolean send(String routingKey, Object data) {
        try {
            rabbitTemplate.convertAndSend(routingKey, data);
            log.info("Sent message:\t[{}]", data.toString());
            return true;
        } catch (Exception e) {
            log.error("Failed to send message: \t [{}]", data.toString(), e);
            return false;
        }
    }
}
