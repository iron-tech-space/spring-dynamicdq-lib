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
public class Field {

    // Описание полей конфигурации
    private String name;
    private String type;
    private String validators;
    private List<Field> fields;

    public Field(String name, String type) {
        this.name = name;
        this.type = type;
    }
}
