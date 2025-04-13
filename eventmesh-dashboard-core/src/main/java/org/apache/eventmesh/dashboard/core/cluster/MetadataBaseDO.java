package org.apache.eventmesh.dashboard.core.cluster;

import org.apache.eventmesh.dashboard.common.model.metadata.ConfigMetadata;

import lombok.Data;
import lombok.EqualsAndHashCode;


@Data
@EqualsAndHashCode(callSuper = true)
public class MetadataBaseDO<C, R extends RuntimeBaseDO> extends ClusterBaseDO<C, R, Object, ConfigMetadata> {


}
