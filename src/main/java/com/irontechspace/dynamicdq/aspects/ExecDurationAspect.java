package com.irontechspace.dynamicdq.aspects;

import com.irontechspace.dynamicdq.annotations.ExecDuration;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Parameter;

@Aspect
@Component
public class ExecDurationAspect {

    @Around("@annotation(com.irontechspace.dynamicdq.annotations.ExecDuration)")
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
        ExecDuration dl = signature.getMethod().getAnnotation(ExecDuration.class);

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
