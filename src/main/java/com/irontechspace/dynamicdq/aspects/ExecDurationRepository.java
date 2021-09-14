package com.irontechspace.dynamicdq.aspects;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.irontechspace.dynamicdq.annotations.ExecDuration;
import lombok.extern.log4j.Log4j2;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Log4j2
@Repository
public class ExecDurationRepository {

    private final static ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final static String INSERT_HTTP_REQUESTS = "insert into dynamicdq.log_http_requests (data) values (:data) returning id;";

    @Autowired
    public NamedParameterJdbcTemplate jdbcTemplate;

    @Async("AsyncExecDuration")
    public void logExecDuration(ProceedingJoinPoint joinPoint, HttpServletRequest request, long[] duration) {

        // Получить структуру метода
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        // Получить класс
        Class<?> cls = signature.getDeclaringType();
        // Получить имя метода
        // String methodName = signature.getName();
        // Логгер
        org.apache.logging.log4j.Logger log = org.apache.logging.log4j.LogManager.getLogger(cls);
        // Заданная аннотация
        ExecDuration dl = signature.getMethod().getAnnotation(ExecDuration.class);

        Map<String, Object> logParams = new HashMap<>();
        if(dl.params().length > 0){
            Parameter[] params = signature.getMethod().getParameters();
            for(int i = 0; i < dl.params().length; i++) {
                for (int j = 0; j < params.length; j++) {
                    if (dl.params()[i].equals(params[j].getName())) {
                        logParams.put(dl.params()[i], joinPoint.getArgs()[j]);
                    }
                }
            }
        }

        ObjectNode httpInfo = OBJECT_MAPPER.createObjectNode();
        httpInfo.put("userId", request.getHeader("userId"));
        httpInfo.put("method", request.getMethod());
        httpInfo.put("url", request.getRequestURI());
        httpInfo.put("params", OBJECT_MAPPER.valueToTree(request.getParameterMap()));
        httpInfo.put("body", OBJECT_MAPPER.valueToTree(logParams));
        httpInfo.put("duration", OBJECT_MAPPER.createObjectNode().put("millis", duration[0]).put("nanos", duration[1]));

        insert(httpInfo.toString());
        log.info("HTTP info {}", httpInfo.toString());
    }

    private UUID insert(String data){
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("data", data);
        return jdbcTemplate.queryForObject(INSERT_HTTP_REQUESTS, params, UUID.class);
    }
}
