package org.apache.eventmesh.dashboard.core.function.SDK.operation.kubernetes;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.enums.RemotingType;
import org.apache.eventmesh.dashboard.core.function.SDK.AbstractSDKOperation;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKMetadata;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKTypeEnum;
import org.apache.eventmesh.dashboard.core.function.SDK.config.CreateKubernetesConfig;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;


@SDKMetadata(clusterType = ClusterType.KUBERNETES_RUNTIME, remotingType = RemotingType.KUBERNETES, sdkTypeEnum = {
    SDKTypeEnum.ADMIN, SDKTypeEnum.PING})
public class KubernetesClientOperation extends AbstractSDKOperation<KubernetesClient, CreateKubernetesConfig> {

    @Override
    public KubernetesClient createClient(CreateKubernetesConfig clientConfig) throws Exception {
        KubernetesClientBuilder kubernetesClientBuilder = new KubernetesClientBuilder();
        kubernetesClientBuilder.withConfig(this.getBaseSyncBase().getConfig());
        return kubernetesClientBuilder.build();
    }

    @Override
    public void close(KubernetesClient client) throws Exception {

    }
}
