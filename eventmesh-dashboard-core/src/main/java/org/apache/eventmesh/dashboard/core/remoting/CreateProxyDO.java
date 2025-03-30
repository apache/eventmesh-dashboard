package org.apache.eventmesh.dashboard.core.remoting;

import lombok.Data;

@Data
public class CreateProxyDO {

    private Object proxy;

    private AbstractRemotingService<Object> remotingService;

}
