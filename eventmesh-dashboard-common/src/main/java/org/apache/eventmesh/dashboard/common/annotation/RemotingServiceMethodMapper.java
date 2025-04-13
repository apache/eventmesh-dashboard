package org.apache.eventmesh.dashboard.common.annotation;

import org.apache.eventmesh.dashboard.common.model.remoting.RemotingActionType;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author jie
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RemotingServiceMethodMapper {

    RemotingActionType[] value();
}
