package com.irontechspace.dynamicdq.executor.events;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class SystemEventType {
    private UUID id;
    private String name;
    private String template;
}
