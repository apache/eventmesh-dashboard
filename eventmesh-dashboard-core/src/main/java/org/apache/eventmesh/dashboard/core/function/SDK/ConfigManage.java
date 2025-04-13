package org.apache.eventmesh.dashboard.core.function.SDK;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.core.function.SDK.config.AbstractCreateSDKConfig;
import org.apache.eventmesh.dashboard.core.function.SDK.config.AbstractMultiCreateSDKConfig;
import org.apache.eventmesh.dashboard.core.function.SDK.config.AbstractSimpleCreateSDKConfig;

public class ConfigManage {

    private static final ConfigManage configManage = new ConfigManage();

    public static ConfigManage getInstance() {
        return configManage;
    }

    private ConfigManage() {
    }


    public AbstractMultiCreateSDKConfig getMultiCreateSDKConfig(ClusterType clusterType, SDKTypeEnum sdkTypeEnum) {
        return (AbstractMultiCreateSDKConfig) getCreateSDKConfig(clusterType, sdkTypeEnum);
    }

    public AbstractSimpleCreateSDKConfig getSimpleCreateSDKConfig(ClusterType clusterType, SDKTypeEnum sdkTypeEnum) {
        return (AbstractSimpleCreateSDKConfig) getCreateSDKConfig(clusterType, sdkTypeEnum);
    }

    private AbstractCreateSDKConfig getCreateSDKConfig(ClusterType clusterType, SDKTypeEnum sdkTypeEnum) {

        try {
            Class<?> clazz = SDKManage.getInstance().getConfig(clusterType, sdkTypeEnum);
            return (AbstractCreateSDKConfig) clazz.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
