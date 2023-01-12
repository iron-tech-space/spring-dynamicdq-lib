package com.irontechspace.dynamicdq.executor.export.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExcelStyle {
    private ExcelBorder border;
    private ExcelFont font;

    @JsonProperty("hAlign")
    private String hAlign = null;

    @JsonProperty("vAlign")
    private String vAlign = null;

    private Boolean wrapped = false;
}
