package com.irontechspace.dynamicdq.model.Query;

import com.irontechspace.dynamicdq.model.Config;
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
public class QueryConfig extends Config {
    private String tableName;
    private Boolean hierarchical;
    private String hierarchyField;
    private String hierarchyView;
    private Boolean hierarchyLazyLoad;
    private String customSql;
    private List<QueryField> fields;
}
