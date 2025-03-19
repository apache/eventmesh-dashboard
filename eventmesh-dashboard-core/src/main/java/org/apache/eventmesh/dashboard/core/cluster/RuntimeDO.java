package org.apache.eventmesh.dashboard.core.cluster;

import org.apache.eventmesh.dashboard.common.model.metadata.ConfigMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;

import lombok.Data;


@Data
public class RuntimeDO extends RuntimeBaseDO<RuntimeMetadata, Object, ConfigMetadata> {


    private RuntimeMetadata runtimeMetadata;


}
