package org.apache.eventmesh.dashboard.core.function.SDK.operation.kafka;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.enums.RemotingType;
import org.apache.eventmesh.dashboard.core.function.SDK.AbstractSDKOperation;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKMetadata;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKTypeEnum;
import org.apache.eventmesh.dashboard.core.function.SDK.config.CreateKakfaConfig;

import org.apache.kafka.clients.admin.AdminClient;

import java.util.Properties;

@SDKMetadata(clusterType = {ClusterType.STORAGE_KAFKA_BROKER, ClusterType.STORAGE_KAFKA_RAFT}, remotingType = RemotingType.KAFKA, sdkTypeEnum = {
    SDKTypeEnum.ADMIN, SDKTypeEnum.PING})
public class KafkaAdminOperation extends AbstractSDKOperation<AdminClient, CreateKakfaConfig> {

    @Override
    public AdminClient createClient(CreateKakfaConfig clientConfig) throws Exception {
        Properties props = new Properties();
        AdminClient adminClient = AdminClient.create(props);
        return adminClient;
    }

    @Override
    public void close(AdminClient client) throws Exception {
        client.close();
    }
}
