package org.apache.eventmesh.dashboard.common.model.remoting;


import lombok.Data;

@Data
public abstract class AbstractGlobal2Request<T> extends Global2Request {

    private T metaData;

}
