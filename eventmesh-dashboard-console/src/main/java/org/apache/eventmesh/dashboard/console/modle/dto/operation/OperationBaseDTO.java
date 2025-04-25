/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.apache.eventmesh.dashboard.console.modle.dto.operation;

import org.apache.eventmesh.dashboard.common.enums.MetadataType;
import org.apache.eventmesh.dashboard.common.enums.OperationRange.OperationRangeType;
import org.apache.eventmesh.dashboard.console.modle.ClusterIdDTO;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 问题：数据关系。批量修改行为如何处理 比如：修改 runtime 的配置。用户行为如何。 具体数据由子类决定。 视图称提供多维度数据视图与操作 1. 添加 2. 删除 3. 修改 4. queue
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OperationBaseDTO extends ClusterIdDTO {

    private MetadataType rangeType;


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
