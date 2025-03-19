package org.apache.eventmesh.dashboard.console.modle.dto.operation;

import org.apache.eventmesh.dashboard.common.enums.MetadataOperationType;
import org.apache.eventmesh.dashboard.common.enums.OperationRange.OperationRangeType;
import org.apache.eventmesh.dashboard.console.modle.ClusterIdDTO;

import lombok.Data;

/**
 * 问题：数据关系。批量修改行为如何处理 比如：修改 runtime 的配置。用户行为如何。
 * 具体数据由子类决定。
 * 视图称提供多维度数据视图与操作
 *
 */
@Data
public class OperationBaseDTO extends ClusterIdDTO {

    /**
     * 接口传入
     */
    private MetadataOperationType metadataOperationType;





    /**
     * 页面传递
     */
    private OperationRangeType operationRangeType;

    /**
     * 页面传递
     */
    private Long operationRangeId;

    /**
     * 操作目标 id
     */
    private Long operationDataTypeId;

    private Long operationDataId;
}
