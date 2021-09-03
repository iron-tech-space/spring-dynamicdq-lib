package com.irontechspace.dynamicdq.configurator.flow.model;

import com.irontechspace.dynamicdq.configurator.core.model.Config;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FlowConfig extends Config {
    private String execDiagram;
    private String uiDiagram;
}
