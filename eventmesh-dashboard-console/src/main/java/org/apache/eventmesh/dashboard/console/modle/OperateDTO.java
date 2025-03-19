package org.apache.eventmesh.dashboard.console.modle;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.enums.OperationRange.OperationRangeType;

import lombok.Data;

/**
 *
 */
@Data
public class OperateDTO extends IdDTO {


    private OperationRangeType operationRangeType;

    private ClusterType clusterType;

}
