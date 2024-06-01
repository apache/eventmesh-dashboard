package org.apache.eventmesh.dashboard.core.function.SDK.config;

import lombok.Data;

@Data
public class CreateRocketmqAdminSDKConfig implements CreateSDKConfig{

    private String nameServerUrl;

    private String clusterName;



    @Override
    public String getUniqueKey() {
        return nameServerUrl;
    }
}
