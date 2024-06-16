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

package org.apache.eventmesh.dashboard.console.service.function;

import org.apache.eventmesh.dashboard.console.entity.function.HealthCheckResultEntity;
import org.apache.eventmesh.dashboard.console.modle.vo.health.InstanceLiveProportionVo;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service providing data of HealthCheckResult.
 */
public interface HealthDataService {

    InstanceLiveProportionVo selectInstanceLiveProportion(Long clusterId, Integer instanceType);

    List<HealthCheckResultEntity> selectInstanceLiveStatusHistory(Integer type, Long clusterId, LocalDateTime startTime);

    HealthCheckResultEntity insertHealthCheckResult(HealthCheckResultEntity healthCheckResultEntity);

    Integer batchInsertHealthCheckResult(List<HealthCheckResultEntity> healthCheckResultEntityList);

    /**
     * New check results have state 4: SDK client not created or connected
     */
    Integer batchInsertNewCheckResult(List<HealthCheckResultEntity> healthCheckResultEntityList);

    List<HealthCheckResultEntity> selectAll();

    List<HealthCheckResultEntity> queryHealthCheckResultByClusterIdAndTypeAndTypeId(HealthCheckResultEntity entity);

    Integer batchUpdateCheckResult(List<HealthCheckResultEntity> healthCheckResultEntityList);

    void batchUpdateCheckResultByClusterIdAndTypeAndTypeId(List<HealthCheckResultEntity> healthCheckResultEntityList);

    List<HealthCheckResultEntity> queryHealthCheckResultByClusterIdAndTimeRange(Long clusterId, Timestamp startTime, Timestamp endTime);
}
