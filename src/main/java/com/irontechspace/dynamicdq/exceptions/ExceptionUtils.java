package com.irontechspace.dynamicdq.exceptions;

import org.apache.logging.log4j.Logger;

public class ExceptionUtils {
    public static void logException(Logger log, Exception e){
        StringBuilder errorMsg = new StringBuilder("\n").append(e).append("\n");
        for (StackTraceElement traceElement : e.getStackTrace())
            errorMsg.append("\tat ").append(traceElement).append("\n");
        log.error(errorMsg.toString());
    }
}
