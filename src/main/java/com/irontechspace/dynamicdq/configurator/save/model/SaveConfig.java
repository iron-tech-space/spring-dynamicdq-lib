package com.irontechspace.dynamicdq.configurator.save.model;

import com.irontechspace.dynamicdq.configurator.core.model.Config;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SaveConfig extends Config  {
    private String logic;
}
