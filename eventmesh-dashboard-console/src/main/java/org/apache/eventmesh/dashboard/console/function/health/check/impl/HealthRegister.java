package org.apache.eventmesh.dashboard.console.function.health.check.impl;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.console.function.health.annotation.HealthCheckType;

public @interface HealthRegister {

    ClusterType clusterType();

    HealthCheckType healthCheckType();

}
