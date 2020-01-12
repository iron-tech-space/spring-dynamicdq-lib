package com.irontechspace.dynamicdq.model.Db;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Field {
    private String columnName;
    private String rawColumnName;
    private String dataType;
}
