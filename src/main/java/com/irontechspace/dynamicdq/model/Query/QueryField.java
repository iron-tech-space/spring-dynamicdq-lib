package com.irontechspace.dynamicdq.model.Query;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class QueryField {
    // private UUID id;
    // private UUID configId;
    // private String description;

    // Back params
    private String name;
    private String alias;
    private String typeData;
    private String typeField;
    private String linkPath;
    private String linkView;
    private String filterInside;
    private String orderByInside;
    private String defaultSort;
    private String defaultFilter;
    private String filterFields;
    private String filterSigns;


    // UI params
    private Long position;
    private String header;
    private Boolean visible;
    private Boolean resizable;
    private Boolean sortable;
    private String align;
    private Long width;

    public String getAliasOrName(){
        return alias != null ? alias : name;
    }
}


