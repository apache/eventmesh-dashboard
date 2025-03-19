package org.apache.eventmesh.dashboard.core.metadata;

import org.apache.eventmesh.dashboard.common.model.remoting.Global2Request;

import java.util.List;

/**
 * @param <T>
 */
public interface GetMetadataHandler<T> {

    List<T> getData();

    default List<T> getData(Global2Request global2Request) {
        return getData();
    }
}
