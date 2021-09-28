package com.irontechspace.dynamicdq.exceptions.handlers;
import com.irontechspace.dynamicdq.exceptions.ForbiddenException;
import com.irontechspace.dynamicdq.exceptions.NotFoundException;
import lombok.extern.log4j.Log4j2;
import org.postgresql.util.PSQLException;
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
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

import static com.irontechspace.dynamicdq.exceptions.ExceptionUtils.getExceptionEntity;
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
            return getExceptionEntity(HttpStatus.METHOD_NOT_ALLOWED, e);
        else if (e instanceof HttpMediaTypeNotSupportedException)
            return getExceptionEntity(HttpStatus.UNSUPPORTED_MEDIA_TYPE, e);
        else if (e instanceof HttpMediaTypeNotAcceptableException)
            return getExceptionEntity(HttpStatus.NOT_ACCEPTABLE, e);
        else if (e instanceof MissingPathVariableException)
            return getExceptionEntity(HttpStatus.INTERNAL_SERVER_ERROR, e);
        else if (e instanceof MissingServletRequestParameterException)
            return getExceptionEntity(HttpStatus.BAD_REQUEST, e);
        else if (e instanceof ServletRequestBindingException)
            return getExceptionEntity(HttpStatus.BAD_REQUEST, e);
        else if (e instanceof ConversionNotSupportedException)
            return getExceptionEntity(HttpStatus.INTERNAL_SERVER_ERROR, e);
        else if (e instanceof TypeMismatchException)
            return getExceptionEntity(HttpStatus.BAD_REQUEST, e);
        else if (e instanceof HttpMessageNotReadableException)
            return getExceptionEntity(HttpStatus.BAD_REQUEST, e);
        else if (e instanceof HttpMessageNotWritableException)
            return getExceptionEntity(HttpStatus.INTERNAL_SERVER_ERROR, e);
        else if (e instanceof MethodArgumentNotValidException)
            return getExceptionEntity(HttpStatus.BAD_REQUEST, e);
        else if (e instanceof MissingServletRequestPartException)
            return getExceptionEntity(HttpStatus.BAD_REQUEST, e);
        else if (e instanceof BindException)
            return getExceptionEntity(HttpStatus.BAD_REQUEST, e);
        else if (e instanceof NoHandlerFoundException)
            return getExceptionEntity(HttpStatus.NOT_FOUND, e);
        else if (e instanceof AsyncRequestTimeoutException)
            return getExceptionEntity(HttpStatus.SERVICE_UNAVAILABLE, e);
        else if (e instanceof ResponseStatusException)
//            return getExceptionEntity(((ResponseStatusException) e).getStatus(), ((ResponseStatusException) e).getReason(), e.getStackTrace());
            return getExceptionEntity(((ResponseStatusException) e).getStatus(), e);


        /** DYNAMICDQ EXCEPTION */
        else if (e instanceof IllegalArgumentException)
            return getExceptionEntity(HttpStatus.INTERNAL_SERVER_ERROR, e);
        else if (e instanceof NullPointerException)
            return getExceptionEntity(HttpStatus.INTERNAL_SERVER_ERROR, e);
        else if (e instanceof NotFoundException)
            return getExceptionEntity(HttpStatus.NOT_FOUND, e);
        else if (e instanceof ForbiddenException)
            return getExceptionEntity(HttpStatus.FORBIDDEN, e);

        /** DB EXCEPTION */
        else if (e instanceof EmptyResultDataAccessException)
            return getExceptionEntity(HttpStatus.NOT_FOUND, e);
        else if (e instanceof PSQLException)
            return getExceptionEntity(HttpStatus.INTERNAL_SERVER_ERROR, e);
        else
            return getExceptionEntity(HttpStatus.INTERNAL_SERVER_ERROR, e);
    }
}
