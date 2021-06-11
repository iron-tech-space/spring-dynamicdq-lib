package com.irontechspace.dynamicdq.DebugLog;

import lombok.extern.log4j.Log4j2;


import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.CodeSignature;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Aspect
@Component
public class DebugLogAspect {

    @Around("@annotation(com.irontechspace.dynamicdq.DebugLog.DebugLog)")
    public Object handle(ProceedingJoinPoint joinPoint) throws Throwable {
        // Время начала выполнения
        long startNanos = System.nanoTime();
        // Выполнение
        Object result = joinPoint.proceed();
        // Продолжительность выполнения
        long durationNanos = System.nanoTime() - startNanos;
        long[] duration = {millis(durationNanos), nanos(durationNanos)};

        // Получить структуру метода
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        // Получить класс
        Class<?> cls = signature.getDeclaringType();
        // Получить имя метода
        String methodName = signature.getName();
        // Логгер
        org.apache.logging.log4j.Logger log = org.apache.logging.log4j.LogManager.getLogger(cls);
        // Заданная аннотация
        DebugLog dl = signature.getMethod().getAnnotation(DebugLog.class);

        if(!StringUtils.isEmpty(dl.param())) {
            Parameter[] params = signature.getMethod().getParameters();
            for (int i = 0; i < params.length; i++) {
                if (params[i].getName().equals(dl.param())) {
                    // Логирование времени выполнения параметра
                    log.info("[{}] Method [{}]. Duration [{} ms {} ns]",
                            joinPoint.getArgs()[i], methodName, duration[0], duration[1]);
                    return result;
                }
            }
        }
        // Логирование времени выполнения
        log.info("Method [{}] duration [{} ms {} ns]", methodName, duration[0], duration[1]);
        return result;
    }

    private long millis(long nanos) {
        return nanos / 1000000L;
    }

    private long nanos(long nanos) {
        return nanos % 1000000L;
    }
}
