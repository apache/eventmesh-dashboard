package org.apache.eventmesh.dashboard.core.cluster;

import org.apache.eventmesh.dashboard.common.model.metadata.ConfigMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;

import lombok.Data;
import lombok.EqualsAndHashCode;


@Data
@EqualsAndHashCode(callSuper = true)
public class RuntimeDO extends RuntimeBaseDO<RuntimeMetadata, Object, ConfigMetadata> {



}
