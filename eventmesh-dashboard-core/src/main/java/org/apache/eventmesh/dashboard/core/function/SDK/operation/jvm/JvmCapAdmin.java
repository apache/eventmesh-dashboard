package org.apache.eventmesh.dashboard.core.function.SDK.operation.jvm;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.enums.RemotingType;
import org.apache.eventmesh.dashboard.core.function.SDK.AbstractSDKOperation;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKMetadata;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKTypeEnum;
import org.apache.eventmesh.dashboard.core.function.SDK.config.CreateJvmCapConfig;

@SDKMetadata(clusterType = ClusterType.STORAGE_JVM_CAP_BROKER, remotingType = RemotingType.JVM, sdkTypeEnum = SDKTypeEnum.ALL)
public class JvmCapAdmin extends AbstractSDKOperation<Object, CreateJvmCapConfig> {

    @Override
    public Object createClient(CreateJvmCapConfig clientConfig) throws Exception {
        return new Object();
    }

    @Override
    public void close(Object client) throws Exception {

    }
}
