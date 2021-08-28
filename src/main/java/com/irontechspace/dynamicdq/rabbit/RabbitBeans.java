package com.irontechspace.dynamicdq.rabbit;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitBeans
{
    @Value("${spring.rabbitmq.queues.tasks}")
    String tasksQueue;

    @Value("${spring.rabbitmq.queues.notifications}")
    String notificationsQueue;

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public Queue tasksQueue() {
        return new Queue(tasksQueue);
    }

    @Bean
    public Queue notificationsQueue() {
        return new Queue(notificationsQueue);
    }
}
