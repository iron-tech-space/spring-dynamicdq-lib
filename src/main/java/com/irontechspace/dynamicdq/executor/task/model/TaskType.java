package com.irontechspace.dynamicdq.executor.task.model;

// Перечисление типы задачи
public enum TaskType {
    flat,
    hierarchical,
    count,
    object,
    sql,
    sqlCount,
    save,
    queue,
    equal,
    notEqual,
    greaterEqual,
    lessEqual,
    greater,
    less,
    log,
    branch,
    output,
    event
}