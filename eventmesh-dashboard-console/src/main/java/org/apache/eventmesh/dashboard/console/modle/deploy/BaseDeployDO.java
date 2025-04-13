package org.apache.eventmesh.dashboard.console.modle.deploy;

import org.apache.eventmesh.dashboard.console.entity.cases.ResouceEntity;

import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class BaseDeployDO extends BaseConifgDO {


    private List<DeployTopicDO> topicList;

    private ResouceEntity resource;


}
