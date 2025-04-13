package org.apache.eventmesh.dashboard.common.model.remoting;


import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public abstract class AbstractGlobal2Request<T> extends Global2Request {

    private T metaData;

}
