package com.irontechspace.dynamicdq.exceptions.handlers;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * ExceptionResponseObject.java
 * Description: Класс для передачи данных исключении
 */

@Getter
@Setter
@Builder
public class ExceptionResponseObject
{
    /**
     * Http статус ошибки
     */
    private Integer status;

    /**
     * Код ошибки
     */
    private String error;

    /**
     * Описание ошибки
     */
    private String error_description;
}

