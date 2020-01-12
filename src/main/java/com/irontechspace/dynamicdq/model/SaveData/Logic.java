package com.irontechspace.dynamicdq.model.SaveData;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Logic {

    // Какое поле обрабатываем
    private String fieldType;
    private String fieldName;

    // Параметры сохранения
    private String primaryKey;
    private String tableName;
    private Boolean autoGenerateCode;
    private Boolean excludePrimaryKey;

    // Список полей которые надо сохранить
    private List<Field> fields;

    // Вложенные объекты логики сохранения
    private List<Logic> children;
}
