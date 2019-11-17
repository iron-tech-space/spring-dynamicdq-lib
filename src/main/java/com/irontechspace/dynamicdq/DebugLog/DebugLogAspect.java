package com.irontechspace.dynamicdq.DebugLog;

import lombok.extern.log4j.Log4j2;


import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.CodeSignature;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Log4j2
@Aspect
@Component
public class DebugLogAspect {

    @Around("@annotation(com.irontechspace.dynamicdq.DebugLog.DebugLog)")
    public Object handle(ProceedingJoinPoint joinPoint) throws Throwable {

        long startNanos = System.nanoTime();
        Object result = joinPoint.proceed();
        long stopNanos = System.nanoTime();
        long lengthMillis = TimeUnit.NANOSECONDS.toMillis(stopNanos - startNanos);


        CodeSignature codeSignature = (CodeSignature) joinPoint.getSignature();

        Class<?> cls = codeSignature.getDeclaringType();
        String methodName = codeSignature.getName();

        org.apache.logging.log4j.LogManager.getLogger(cls).info("Method [{}] duration [{} ms]", methodName, lengthMillis);

        return result;
    }
}
