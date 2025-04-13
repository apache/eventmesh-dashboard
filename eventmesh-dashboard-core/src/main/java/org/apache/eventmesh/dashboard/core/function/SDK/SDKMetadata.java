package org.apache.eventmesh.dashboard.core.function.SDK;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.enums.RemotingType;
import org.apache.eventmesh.dashboard.core.function.SDK.config.NullCreateSDKConfig;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * @author hahaha
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SDKMetadata {

    ClusterType[] clusterType();


    RemotingType remotingType();


    SDKTypeEnum[] sdkTypeEnum();

    Class<?> config() default NullCreateSDKConfig.class;

    /**
     * @return
     */
    ProtocolType protocol() default ProtocolType.PRIVATE;

    /**
     * value is {@link ClusterType.RUNTIME } or {@link ClusterType.CLUSTER }
     *
     * @return
     */
    ClusterType dimension() default ClusterType.RUNTIME;

}
