package com.irontechspace.dynamicdq.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConfigField {
    private UUID id;
    private UUID idConfig;
    private String name;
    private String alias;
    private String header;
    private String description;
    private String type;
    private String linkPath;
    private String linkView;
    private Boolean visible;
    private Long position;
    private Boolean resizable;
    private String filterInside;
    private String orderByInside;
    private Boolean sortable;
    private String align;
    private String defaultSort;
    private String defaultFilter;
    private Long width;
    private String filterFields;
    private String filterSigns;

    public String getAliasOrName(){
        return alias != null ? alias : name;
    }
}


