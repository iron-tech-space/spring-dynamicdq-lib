package com.irontechspace.dynamicdq.executor.file.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class File  {
    private String name;
    private String absolutePath;
    private byte[] content;
}
