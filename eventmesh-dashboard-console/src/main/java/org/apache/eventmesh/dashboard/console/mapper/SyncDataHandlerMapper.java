package org.apache.eventmesh.dashboard.console.mapper;

import java.util.List;

public interface SyncDataHandlerMapper<T> {

    void syncInsert(List<T> entityList);

    void syncUpdate(List<T> entityList);

    void syncDelete(List<T> entityList);

    List<T> syncGet(T topicEntity);

}
