package org.apache.eventmesh.dashboard.core.function.SDK;

import org.apache.eventmesh.dashboard.common.model.base.BaseSyncBase;
import org.apache.eventmesh.dashboard.core.function.SDK.config.CreateSDKConfig;

import lombok.Setter;

public abstract class AbstractClientInfo<T> {

    @Setter
    private ClientWrapper clientWrapper;


    public T getClient() {
        return (T) this.clientWrapper.getClientMap().get(this.getSDKTypeEnum());
    }

    public CreateSDKConfig getCreateSDKConfig() {
        return this.getCreateSDKConfig();
    }

    public BaseSyncBase getBaseSyncBase() {
        return this.getBaseSyncBase();
    }


    abstract SDKTypeEnum getSDKTypeEnum();
}
