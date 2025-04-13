package org.apache.eventmesh.dashboard.console.function.health.check.impl.jvm;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.enums.health.HealthCheckTypeEnum;
import org.apache.eventmesh.dashboard.console.function.health.annotation.HealthCheckType;
import org.apache.eventmesh.dashboard.console.function.health.callback.HealthCheckCallback;
import org.apache.eventmesh.dashboard.console.function.health.check.AbstractHealthCheckService;
import org.apache.eventmesh.dashboard.core.function.SDK.wrapper.NacosSDKWrapper;

@HealthCheckType(clusterType = {ClusterType.STORAGE_JVM_CAP_BROKER}, healthType = HealthCheckTypeEnum.PING)
public class JVMCapCheck extends AbstractHealthCheckService<NacosSDKWrapper> {

    @Override
    public void doCheck(HealthCheckCallback callback) throws Exception {

    }
}
