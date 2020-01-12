package com.irontechspace.dynamicdq.model.SaveData;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SaveConfig {
    private UUID id;
    private UUID userId;
    private String configName;
    private String description;
    private Long position;
    private String sharedForUsers;
    private String sharedForRoles;
    private String logic;
}
