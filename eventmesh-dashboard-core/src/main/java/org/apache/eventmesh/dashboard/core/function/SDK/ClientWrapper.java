package org.apache.eventmesh.dashboard.core.function.SDK;

import org.apache.eventmesh.dashboard.common.model.base.BaseSyncBase;
import org.apache.eventmesh.dashboard.core.function.SDK.config.CreateSDKConfig;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;

@Data
public class ClientWrapper {

    private Map<SDKTypeEnum, Object> clientMap = new HashMap<>();

    private CreateSDKConfig config;

    private BaseSyncBase baseSyncBase;


}
