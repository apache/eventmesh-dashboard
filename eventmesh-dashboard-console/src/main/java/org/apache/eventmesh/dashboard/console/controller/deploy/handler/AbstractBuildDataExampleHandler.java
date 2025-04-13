package org.apache.eventmesh.dashboard.console.controller.deploy.handler;


import org.apache.eventmesh.dashboard.console.entity.cases.DeployScriptEntity;
import org.apache.eventmesh.dashboard.console.entity.cases.ResourcesConfigEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.entity.function.ConfigEntity;

import java.util.List;

public abstract class AbstractBuildDataExampleHandler<T> extends AbstractMetadataExampleHandler<T> {

    protected ResourcesConfigEntity resourcesConfigEntity;

    protected DeployScriptEntity deployScriptEntity;

    protected List<ConfigEntity> configEntityList;

    protected ClusterEntity clusterEntity;

    protected ClusterEntity kubeClusterEntity;



}
