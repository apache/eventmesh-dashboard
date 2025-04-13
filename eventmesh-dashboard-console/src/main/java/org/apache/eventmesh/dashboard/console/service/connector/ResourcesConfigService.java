package org.apache.eventmesh.dashboard.console.service.connector;

import org.apache.eventmesh.dashboard.console.entity.cases.ResourcesConfigEntity;

import java.util.List;

public interface ResourcesConfigService {



    void insertResources(ResourcesConfigEntity resourcesConfigEntity);


    ResourcesConfigEntity queryResourcesById(ResourcesConfigEntity resourcesConfigEntity);


    List<ResourcesConfigEntity> queryResourcesByOrganizationId(ResourcesConfigEntity resourcesConfigEntity);

    void copyResources(ResourcesConfigEntity resourcesConfigEntity);
}
