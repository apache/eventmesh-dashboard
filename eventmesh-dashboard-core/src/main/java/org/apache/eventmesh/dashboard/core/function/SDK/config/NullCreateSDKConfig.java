package org.apache.eventmesh.dashboard.core.function.SDK.config;

public class NullCreateSDKConfig extends AbstractCreateSDKConfig {

    @Override
    protected String uniqueKey() {
        return "null_";
    }

    @Override
    String doUniqueKey() {
        return "";
    }
}
