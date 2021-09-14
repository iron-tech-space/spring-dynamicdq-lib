package com.irontechspace.dynamicdq.exceptions.handlers;
import com.irontechspace.dynamicdq.exceptions.ForbiddenException;
import com.irontechspace.dynamicdq.exceptions.NotFoundException;
import lombok.extern.log4j.Log4j2;
import org.postgresql.util.PSQLException;
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;

import static com.irontechspace.dynamicdq.exceptions.ExceptionUtils.logException;

@Log4j2
@ControllerAdvice
public class DefaultAdvice
{
    @ExceptionHandler({
            /** HTTP EXCEPTION */
            HttpRequestMethodNotSupportedException.class,
            HttpMediaTypeNotSupportedException.class,
            HttpMediaTypeNotAcceptableException.class,
            MissingPathVariableException.class,
            MissingServletRequestParameterException.class,
            ServletRequestBindingException.class,
            ConversionNotSupportedException.class,
            TypeMismatchException.class,
            HttpMessageNotReadableException.class,
            HttpMessageNotWritableException.class,
            MethodArgumentNotValidException.class,
            MissingServletRequestPartException.class,
            BindException.class,
            NoHandlerFoundException.class,
            AsyncRequestTimeoutException.class,
            ResponseStatusException.class,

            /** DYNAMICDQ EXCEPTION */
            IllegalArgumentException.class,
            NullPointerException.class,
            NotFoundException.class,
            ForbiddenException.class,

            /** DB EXCEPTION */
            EmptyResultDataAccessException.class,
            PSQLException.class
    })
    public ResponseEntity<Object> handleException(Exception e) {
//        ex.printStackTrace();
        logException(log, e);

        /** HTTP EXCEPTION */
        if (e instanceof HttpRequestMethodNotSupportedException)
            return getEntity(HttpStatus.METHOD_NOT_ALLOWED, e.getMessage(), e.getStackTrace());
        else if (e instanceof HttpMediaTypeNotSupportedException)
            return getEntity(HttpStatus.UNSUPPORTED_MEDIA_TYPE, e.getMessage(), e.getStackTrace());
        else if (e instanceof HttpMediaTypeNotAcceptableException)
            return getEntity(HttpStatus.NOT_ACCEPTABLE, e.getMessage(), e.getStackTrace());
        else if (e instanceof MissingPathVariableException)
            return getEntity(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e.getStackTrace());
        else if (e instanceof MissingServletRequestParameterException)
            return getEntity(HttpStatus.BAD_REQUEST, e.getMessage(), e.getStackTrace());
        else if (e instanceof ServletRequestBindingException)
            return getEntity(HttpStatus.BAD_REQUEST, e.getMessage(), e.getStackTrace());
        else if (e instanceof ConversionNotSupportedException)
            return getEntity(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e.getStackTrace());
        else if (e instanceof TypeMismatchException)
            return getEntity(HttpStatus.BAD_REQUEST, e.getMessage(), e.getStackTrace());
        else if (e instanceof HttpMessageNotReadableException)
            return getEntity(HttpStatus.BAD_REQUEST, e.getMessage(), e.getStackTrace());
        else if (e instanceof HttpMessageNotWritableException)
            return getEntity(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e.getStackTrace());
        else if (e instanceof MethodArgumentNotValidException)
            return getEntity(HttpStatus.BAD_REQUEST, e.getMessage(), e.getStackTrace());
        else if (e instanceof MissingServletRequestPartException)
            return getEntity(HttpStatus.BAD_REQUEST, e.getMessage(), e.getStackTrace());
        else if (e instanceof BindException)
            return getEntity(HttpStatus.BAD_REQUEST, e.getMessage(), e.getStackTrace());
        else if (e instanceof NoHandlerFoundException)
            return getEntity(HttpStatus.NOT_FOUND, e.getMessage(), e.getStackTrace());
        else if (e instanceof AsyncRequestTimeoutException)
            return getEntity(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage(), e.getStackTrace());
        else if (e instanceof ResponseStatusException)
            return getEntity(((ResponseStatusException) e).getStatus(), ((ResponseStatusException) e).getReason(), e.getStackTrace());


        /** DYNAMICDQ EXCEPTION */
        else if (e instanceof IllegalArgumentException)
            return getEntity(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e.getStackTrace());
        else if (e instanceof NullPointerException)
            return getEntity(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e.getStackTrace());
        else if (e instanceof NotFoundException)
            return getEntity(HttpStatus.NOT_FOUND, e.getMessage(), e.getStackTrace());
        else if (e instanceof ForbiddenException)
            return getEntity(HttpStatus.FORBIDDEN, e.getMessage(), e.getStackTrace());

        /** DB EXCEPTION */
        else if (e instanceof EmptyResultDataAccessException)
            return getEntity(HttpStatus.NOT_FOUND, e.getMessage(), e.getStackTrace());
        else if (e instanceof PSQLException)
            return getEntity(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e.getStackTrace());
        else
            return getEntity(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e.getStackTrace());
    }

    private ResponseEntity<Object> getEntity (HttpStatus status, String message, StackTraceElement[] error) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ExceptionResponseObject.builder().status(status.value()).error(message).build());
//                .body(ExceptionResponseObject.builder().status(status.value()).error(message).error_description(error).build());
    }
}
