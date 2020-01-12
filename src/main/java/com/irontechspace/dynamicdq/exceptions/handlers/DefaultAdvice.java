package com.irontechspace.dynamicdq.exceptions.handlers;


import com.irontechspace.dynamicdq.exceptions.ForbiddenException;
import com.irontechspace.dynamicdq.exceptions.NotFoundException;
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
import org.springframework.web.servlet.NoHandlerFoundException;

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

            /** DYNAMICDQ EXCEPTION */
            IllegalArgumentException.class,
            NullPointerException.class,
            NotFoundException.class,
            ForbiddenException.class,

            /** DB EXCEPTION */
            EmptyResultDataAccessException.class,
            PSQLException.class
    })
    public ResponseEntity<Object> handleException(Exception ex) {
        ex.printStackTrace();

        /** HTTP EXCEPTION */
        if (ex instanceof HttpRequestMethodNotSupportedException)
            return getEntity(HttpStatus.METHOD_NOT_ALLOWED, ex.getMessage(), ex.getStackTrace());
        else if (ex instanceof HttpMediaTypeNotSupportedException)
            return getEntity(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ex.getMessage(), ex.getStackTrace());
        else if (ex instanceof HttpMediaTypeNotAcceptableException)
            return getEntity(HttpStatus.NOT_ACCEPTABLE, ex.getMessage(), ex.getStackTrace());
        else if (ex instanceof MissingPathVariableException)
            return getEntity(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), ex.getStackTrace());
        else if (ex instanceof MissingServletRequestParameterException)
            return getEntity(HttpStatus.BAD_REQUEST, ex.getMessage(), ex.getStackTrace());
        else if (ex instanceof ServletRequestBindingException)
            return getEntity(HttpStatus.BAD_REQUEST, ex.getMessage(), ex.getStackTrace());
        else if (ex instanceof ConversionNotSupportedException)
            return getEntity(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), ex.getStackTrace());
        else if (ex instanceof TypeMismatchException)
            return getEntity(HttpStatus.BAD_REQUEST, ex.getMessage(), ex.getStackTrace());
        else if (ex instanceof HttpMessageNotReadableException)
            return getEntity(HttpStatus.BAD_REQUEST, ex.getMessage(), ex.getStackTrace());
        else if (ex instanceof HttpMessageNotWritableException)
            return getEntity(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), ex.getStackTrace());
        else if (ex instanceof MethodArgumentNotValidException)
            return getEntity(HttpStatus.BAD_REQUEST, ex.getMessage(), ex.getStackTrace());
        else if (ex instanceof MissingServletRequestPartException)
            return getEntity(HttpStatus.BAD_REQUEST, ex.getMessage(), ex.getStackTrace());
        else if (ex instanceof BindException)
            return getEntity(HttpStatus.BAD_REQUEST, ex.getMessage(), ex.getStackTrace());
        else if (ex instanceof NoHandlerFoundException)
            return getEntity(HttpStatus.NOT_FOUND, ex.getMessage(), ex.getStackTrace());
        else if (ex instanceof AsyncRequestTimeoutException)
            return getEntity(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), ex.getStackTrace());

        /** DYNAMICDQ EXCEPTION */
        else if (ex instanceof IllegalArgumentException)
            return getEntity(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), ex.getStackTrace());
        else if (ex instanceof NullPointerException)
            return getEntity(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), ex.getStackTrace());
        else if (ex instanceof NotFoundException)
            return getEntity(HttpStatus.NOT_FOUND, ex.getMessage(), ex.getStackTrace());
        else if (ex instanceof ForbiddenException)
            return getEntity(HttpStatus.FORBIDDEN, ex.getMessage(), ex.getStackTrace());

        /** DB EXCEPTION */
        else if (ex instanceof EmptyResultDataAccessException)
            return getEntity(HttpStatus.NOT_FOUND, ex.getMessage(), ex.getStackTrace());
        else if (ex instanceof PSQLException)
            return getEntity(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), ex.getStackTrace());
        else
            return getEntity(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), ex.getStackTrace());
    }

    private ResponseEntity<Object> getEntity (HttpStatus status, String message, StackTraceElement[] error) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ExceptionResponseObject.builder().status(status.value()).error(message).error_description(error).build());
    }
}
