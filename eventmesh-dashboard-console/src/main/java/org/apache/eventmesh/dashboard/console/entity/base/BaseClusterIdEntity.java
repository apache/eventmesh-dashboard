package org.apache.eventmesh.dashboard.console.entity.base;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;

import lombok.Data;
import lombok.EqualsAndHashCode;


@Data
@EqualsAndHashCode(callSuper = true)
public abstract class BaseClusterIdEntity extends BaseOrganizationEntity {

    private Long clusterId;

    private ClusterType clusterType;

}
