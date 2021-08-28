package com.irontechspace.dynamicdq.configurator.core.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Config {
    private UUID id;
    private UUID parentId;
    private Boolean isGroup;
    private UUID userId;
    private String configName;
    private String description;
    private Long position;
    private String sharedForUsers;
    private String sharedForRoles;
    private Boolean loggingQueries;
}
