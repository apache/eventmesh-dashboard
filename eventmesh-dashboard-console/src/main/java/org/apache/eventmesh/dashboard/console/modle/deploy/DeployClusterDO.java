package org.apache.eventmesh.dashboard.console.modle.deploy;

import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;

import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DeployClusterDO extends BaseDeployDO {

    private ClusterEntity cluster;

    private List<DeployRuntimeDO> deployRuntimeDOList;


}
