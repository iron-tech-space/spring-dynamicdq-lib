package com.irontechspace.dynamicdq.model.Save;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SaveField {

    // Описание полей конфигурации
    private String name;
    private String type;
    private String validators;
    private List<SaveField> fields;

    public SaveField(String name, String type) {
        this.name = name;
        this.type = type;
    }
}
