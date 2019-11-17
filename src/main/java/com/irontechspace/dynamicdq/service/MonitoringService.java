package com.irontechspace.dynamicdq.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

@Log4j2
@Service
public class MonitoringService {

    @Autowired
    private Environment env;

    @Autowired
    private JmsTemplate jmsTemplate;

    private ObjectMapper mapper = new ObjectMapper();

    @Value("${sys.monitoring.active}")
    Boolean monitoringActive;


    @Scheduled(fixedRate = 5000)
    public void SendMonitoringInfo() throws JsonProcessingException {
//        RestTemplate restTemplate = new RestTemplate();
//
//         Send request with GET method and default Headers.
//        String result = restTemplate.getForObject(URL_EMPLOYEES, String.class);
//
//        System.out.println(result);
        if(monitoringActive) {
            ObjectNode on = mapper.createObjectNode();
            on.put("serviceDomain", env.getProperty("service.domain"));
            on.put("serviceAddress", env.getProperty("service.address"));
            on.put("serviceTime", LocalDateTime.now().toEpochSecond(ZoneOffset.ofHours(3)));
            jmsTemplate.convertAndSend("monitoringQueue", mapper.writeValueAsString(on));
            log.info("Sent to monitoringQueue [{}] [{}]", env.getProperty("service.domain"), env.getProperty("service.address"));
        }
    }
}
