package com.irontechspace.dynamicdq.exceptions;

import com.irontechspace.dynamicdq.exceptions.handlers.ExceptionResponseObject;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

public class ExceptionUtils {

    public static void logException(Logger log, Exception e) {
        log.error(getStackTrace(e));
    }

//    public static ResponseEntity<Object> getExceptionEntity(HttpStatus status, String message, StackTraceElement[] error) {
    public static ResponseEntity<Object> getExceptionEntity(HttpStatus status, Exception e) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ExceptionResponseObject.builder().status(status.value()).error(e.getMessage()).error_description(getStackTrace(e)).build());
//                .body(ExceptionResponseObject.builder().status(status.value()).error(message).error_description(error).build());
    }

    public static String getStackTrace(Exception e){
        StringBuilder errorMsg = new StringBuilder("\n").append(e).append("\n");
        for (StackTraceElement traceElement : e.getStackTrace())
            errorMsg.append("\tat ").append(traceElement).append("\n");
        return errorMsg.toString();
    }
}
