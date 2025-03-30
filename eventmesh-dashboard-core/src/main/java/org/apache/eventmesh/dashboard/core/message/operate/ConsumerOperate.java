package org.apache.eventmesh.dashboard.core.message.operate;

import java.util.List;

public interface ConsumerOperate {


    void start();

    void stop();

    List<Object> pull();
}
