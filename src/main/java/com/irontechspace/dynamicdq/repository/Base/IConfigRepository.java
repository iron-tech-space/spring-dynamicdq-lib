package com.irontechspace.dynamicdq.repository.Base;

import java.util.List;
import java.util.UUID;

public interface IConfigRepository<T> {

    List<T> getAll();

    T save(T config);

    void delete(UUID configId);
}
