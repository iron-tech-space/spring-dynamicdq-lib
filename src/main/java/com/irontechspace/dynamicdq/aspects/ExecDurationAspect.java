package com.irontechspace.dynamicdq.aspects;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

@Aspect
@Component
@EnableAsync
public class ExecDurationAspect {

    @Autowired
    ExecDurationRepository execDurationRepository;

    @Around("@annotation(com.irontechspace.dynamicdq.annotations.ExecDuration)")
    public Object handle(ProceedingJoinPoint joinPoint) throws Throwable {
        // Время начала выполнения
        long startNanos = System.nanoTime();

        // Выполнение
        Object result = joinPoint.proceed();
        // Продолжительность выполнения
        long durationNanos = System.nanoTime() - startNanos;
        long[] duration = {millis(durationNanos), nanos(durationNanos)};

        //Get the HttpServletRequest currently bound to the thread.
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        execDurationRepository.logExecDuration(joinPoint, request, duration);
        return result;
    }

    private long millis(long nanos) {
        return nanos / 1000000L;
    }

    private long nanos(long nanos) {
        return nanos % 1000000L;
    }
}
