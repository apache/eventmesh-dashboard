package org.apache.eventmesh.dashboard.core.function.SDK.config;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AbstractSimpleCreateSDKConfig extends AbstractCreateSDKConfig {


    private NetAddress netAddress;


    @Override
    protected String uniqueKey() {
        return "s_";
    }

    public String doUniqueKey() {
        return netAddress.doUniqueKey();
    }
}
