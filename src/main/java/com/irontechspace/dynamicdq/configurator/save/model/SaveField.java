package com.irontechspace.dynamicdq.configurator.save.model;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
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
