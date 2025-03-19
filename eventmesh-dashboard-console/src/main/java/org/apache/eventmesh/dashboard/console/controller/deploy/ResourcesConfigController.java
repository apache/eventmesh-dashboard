package org.apache.eventmesh.dashboard.console.controller.deploy;

import org.apache.eventmesh.dashboard.console.entity.cases.ResourcesConfigEntity;
import org.apache.eventmesh.dashboard.console.mapstruct.deploy.ResourceConfigControllerMapper;
import org.apache.eventmesh.dashboard.console.modle.IdDTO;
import org.apache.eventmesh.dashboard.console.modle.deploy.resouce.QueryResourceByObjectTypeDTO;

import java.util.List;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("resourcesConfig")
public class ResourcesConfigController {



    public List<ResourcesConfigEntity> queryResourcesConfigByObjectType(@RequestBody QueryResourceByObjectTypeDTO queryResourcesConfiByObjectTypeDTO) {
        ResourceConfigControllerMapper.INSTANCE.queryResourcesConfigByObjectType(queryResourcesConfiByObjectTypeDTO);
        return null;
    }

    public Integer deleteResourcesConfig(@RequestBody IdDTO idDTO) {
        // 作废之前检查，是否使用过
        return 0;
    }

    public List<ResourcesConfigEntity> queryResourcesConfigByOrganization() {
        return null;
    }
}
