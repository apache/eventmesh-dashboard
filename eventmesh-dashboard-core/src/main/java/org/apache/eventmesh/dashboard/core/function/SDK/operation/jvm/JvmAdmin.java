package org.apache.eventmesh.dashboard.core.function.SDK.operation.jvm;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.enums.RemotingType;
import org.apache.eventmesh.dashboard.core.function.SDK.AbstractSDKOperation;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKMetadata;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKTypeEnum;
import org.apache.eventmesh.dashboard.core.function.SDK.config.CreateJvmConfig;

@SDKMetadata(clusterType = ClusterType.STORAGE_JVM_BROKER, remotingType = RemotingType.JVM, sdkTypeEnum = SDKTypeEnum.ALL)
public class JvmAdmin extends AbstractSDKOperation<Object, CreateJvmConfig> {

    @Override
    public Object createClient(CreateJvmConfig clientConfig) throws Exception {
        return new Object();
    }

    @Override
    public void close(Object client) throws Exception {

    }
}
