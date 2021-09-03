package com.irontechspace.dynamicdq.configurator.query.model;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.irontechspace.dynamicdq.configurator.core.model.Config;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QueryConfig extends Config {
    private String tableName;
    private Boolean hierarchical;
    private String hierarchyField;
    private String hierarchyView;
    private Boolean hierarchyLazyLoad;
    private String customSql;
    private List<QueryField> fields;
    private ObjectNode userSettings;
}