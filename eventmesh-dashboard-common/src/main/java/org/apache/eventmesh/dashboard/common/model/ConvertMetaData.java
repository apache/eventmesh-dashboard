package org.apache.eventmesh.dashboard.common.model;

public interface ConvertMetaData<E, M> {


    E toEntity(M meta);

    M toMetaData(E entity);

}
