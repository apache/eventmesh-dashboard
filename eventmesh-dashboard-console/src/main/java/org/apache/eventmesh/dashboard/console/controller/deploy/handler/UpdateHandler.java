package org.apache.eventmesh.dashboard.console.controller.deploy.handler;

public interface UpdateHandler<T> {

    void init();

    void handler(T t);

}
