package org.apache.eventmesh.dashboard.common.model.remoting;

import org.apache.eventmesh.dashboard.common.enums.RemotingType;

public @interface RemotingAction {

   boolean support();

   RemotingType substitution();

   RemotingType retrySubstitution() default  RemotingType.STORAGE;
}
