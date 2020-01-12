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
public class Dependence {

    // Описание зависимости удаления
    private String tableName;
    private String whereField;
    private List<Dependence> dependencies;
}
