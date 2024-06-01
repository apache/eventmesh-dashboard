package org.apache.eventmesh.dashboard.core.remoting;

import lombok.Data;
import org.apache.eventmesh.dashboard.core.function.SDK.config.CreateSDKConfig;
import org.apache.eventmesh.dashboard.service.remoting.RemotingServiceType;

import java.util.HashMap;
import java.util.Map;

@Data
public class RemotingServiceRuntimeConfig {



    private CreateSDKConfig runtimeConfig;

    private CreateSDKConfig storageConfig;

    private Map<RemotingServiceType,String>  remotingServiceTypeStringMap = new HashMap<>();
}
