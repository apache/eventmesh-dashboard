package org.apache.eventmesh.dashboard.common.annotation;

public @interface RemotingServiceMethodMapper {

    Class<?> mapperClass();

    String methodName();
}
