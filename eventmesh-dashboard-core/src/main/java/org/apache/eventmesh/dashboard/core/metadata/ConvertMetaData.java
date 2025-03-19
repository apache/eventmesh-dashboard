package org.apache.eventmesh.dashboard.core.metadata;

public interface ConvertMetaData<E, M> {


    E toEntity(M meta);

    M toMetaData(E entity);

}
