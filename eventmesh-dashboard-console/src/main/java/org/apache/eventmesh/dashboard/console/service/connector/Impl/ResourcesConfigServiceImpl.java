package org.apache.eventmesh.dashboard.console.service.connector.Impl;


import jdk.jfr.Threshold;

import org.apache.eventmesh.dashboard.console.entity.cases.ResourcesConfigEntity;
import org.apache.eventmesh.dashboard.console.service.connector.ResourcesConfigService;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
@Threshold
public class ResourcesConfigServiceImpl implements ResourcesConfigService {



    @Override
    public void insertResources(ResourcesConfigEntity resourcesConfigEntity) {

    }

    @Override
    public ResourcesConfigEntity queryResourcesById(ResourcesConfigEntity resourcesConfigEntity) {
        return null;
    }

    @Override
    public List<ResourcesConfigEntity> queryResourcesByOrganizationId(ResourcesConfigEntity resourcesConfigEntity) {
        return List.of();
    }
}
