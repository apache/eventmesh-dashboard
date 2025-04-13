package org.apache.eventmesh.dashboard.core.function.SDK.operation.kafka;

import org.apache.eventmesh.dashboard.core.function.SDK.AbstractSDKOperation;
import org.apache.eventmesh.dashboard.core.function.SDK.config.CreateKafkaZkConfig;

import org.apache.zookeeper.ZooKeeper;

/**
 * user ZooKeeper or KafkaZkClient or  ZookeeperAdmin
 *
 * @see org.apache.eventmesh.dashboard.core.function.SDK.operation.zookeeper.ZookeeperAdmin
 */
public class KafkaZkAdminOperation extends AbstractSDKOperation<ZooKeeper, CreateKafkaZkConfig> {

    @Override
    public ZooKeeper createClient(CreateKafkaZkConfig clientConfig) throws Exception {
        return null;
    }

    @Override
    public void close(ZooKeeper client) throws Exception {
        client.close();
    }
}
