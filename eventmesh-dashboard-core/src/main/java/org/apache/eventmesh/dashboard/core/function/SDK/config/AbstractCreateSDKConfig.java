package org.apache.eventmesh.dashboard.core.function.SDK.config;


import lombok.Data;

@Data
public abstract class AbstractCreateSDKConfig implements CreateSDKConfig {


    /**
     * true is  key
     * <p>
     * false if  address + ":" + port
     */
    private boolean keyMode = true;

    /**
     * cluster id  or runtime id
     */
    private String key;

    private String namespace;

    private String username;

    private String password;

    private String accessKey;

    private String secretKey;


    public String getUniqueKey() {
        return this.keyMode ? this.key : this.doUniqueKey();
    }

    abstract String doUniqueKey();

}
