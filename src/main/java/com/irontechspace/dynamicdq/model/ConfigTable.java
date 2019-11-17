package com.irontechspace.dynamicdq.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConfigTable {
    private UUID id;
    private Long userId;
    private String configName;
    private String tableName;
    private String description;
    private Long position;
    private Boolean hierarchical;
    private String hierarchyField;
    private String hierarchyView;
    private Boolean hierarchyLazyLoad;
    private String customSql;
    private String sharedForUsers;
    private List<ConfigField> fields;
}
