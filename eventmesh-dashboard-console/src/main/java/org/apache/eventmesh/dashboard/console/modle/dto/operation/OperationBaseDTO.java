package org.apache.eventmesh.dashboard.console.modle.dto.operation;

import org.apache.eventmesh.dashboard.common.enums.MetadataOperationType;
import org.apache.eventmesh.dashboard.common.enums.OperationRange.OperationRangeType;
import org.apache.eventmesh.dashboard.console.modle.ClusterIdDTO;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 问题：数据关系。批量修改行为如何处理 比如：修改 runtime 的配置。用户行为如何。 具体数据由子类决定。 视图称提供多维度数据视图与操作
 * 1. 添加
 * 2. 删除
 * 3. 修改
 * 4. queue
 */
@Data
@EqualsAndHashCode(callSuper = true)
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
     * 操作目标 id ， 修噶配置，重置offset，拷贝 metadata
     */
    private Long operationDataTypeId;

    private Long operationDataId;
}
