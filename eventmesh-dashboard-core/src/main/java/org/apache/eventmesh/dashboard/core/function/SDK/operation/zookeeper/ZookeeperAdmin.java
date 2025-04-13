package org.apache.eventmesh.dashboard.core.function.SDK.operation.zookeeper;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.enums.RemotingType;
import org.apache.eventmesh.dashboard.core.function.SDK.AbstractSDKOperation;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKMetadata;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKTypeEnum;
import org.apache.eventmesh.dashboard.core.function.SDK.config.CreateZookeeperConfig;

import org.apache.zookeeper.ZooKeeper;

@SDKMetadata(clusterType = {ClusterType.STORAGE_KAFKA_ZK}, remotingType = RemotingType.KAFKA, sdkTypeEnum = {
    SDKTypeEnum.ADMIN, SDKTypeEnum.PING})
public class ZookeeperAdmin extends AbstractSDKOperation<ZooKeeper, CreateZookeeperConfig> {

    @Override
    public ZooKeeper createClient(CreateZookeeperConfig clientConfig) throws Exception {

        return null;
    }

    @Override
    public void close(ZooKeeper client) throws Exception {
        client.close();
    }
}
