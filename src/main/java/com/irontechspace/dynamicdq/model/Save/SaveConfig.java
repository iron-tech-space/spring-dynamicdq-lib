package com.irontechspace.dynamicdq.model.Save;

import com.irontechspace.dynamicdq.model.Config;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SaveConfig extends Config  {
    private String logic;
}
