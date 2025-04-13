package org.apache.eventmesh.dashboard.core.function.SDK;

import org.apache.eventmesh.dashboard.common.model.base.BaseSyncBase;
import org.apache.eventmesh.dashboard.core.function.SDK.config.CreateSDKConfig;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import lombok.Setter;

public abstract class AbstractClientInfo<T> {

    @Setter
    private ClientWrapper clientWrapper;

    @Setter
    private Executor executor;

    public T getClient() {
        return (T) this.clientWrapper.getClientMap().get(this.getSDKTypeEnum());
    }

    protected CompletableFuture<Void> completableFuture(Runnable runnable) {
        return Objects.nonNull(this.executor) ? CompletableFuture.runAsync(runnable, executor) : CompletableFuture.runAsync(runnable);
    }

    public CreateSDKConfig getCreateSDKConfig() {
        return this.getCreateSDKConfig();
    }

    public BaseSyncBase getBaseSyncBase() {
        return this.getBaseSyncBase();
    }


    protected abstract SDKTypeEnum getSDKTypeEnum();
}
