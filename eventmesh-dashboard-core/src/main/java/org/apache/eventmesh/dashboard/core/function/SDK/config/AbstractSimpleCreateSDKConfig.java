package org.apache.eventmesh.dashboard.core.function.SDK.config;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class AbstractSimpleCreateSDKConfig extends AbstractCreateSDKConfig {


    private NetAddress netAddress;


    public String doUniqueKey() {
        return netAddress.doUniqueKey();
    }
}
