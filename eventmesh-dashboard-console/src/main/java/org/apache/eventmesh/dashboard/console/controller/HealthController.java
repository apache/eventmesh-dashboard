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

package org.apache.eventmesh.dashboard.console.controller;

import org.apache.eventmesh.dashboard.console.entity.health.HealthCheckResultEntity;
import org.apache.eventmesh.dashboard.console.modle.vo.health.InstanceLiveProportionVo;
import org.apache.eventmesh.dashboard.console.service.health.HealthDataService;

import java.sql.Timestamp;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @Autowired
    HealthDataService healthDataService;

    @GetMapping("/cluster/health/getHistoryLiveStatus")
    public List<HealthCheckResultEntity> getHistoryLiveStatusById(Integer type, Long instanceId, String startTime) {
        Timestamp timestamp = Timestamp.valueOf(startTime);
        return healthDataService.getInstanceLiveStatusHistory(type, instanceId, timestamp);
    }

    @GetMapping("/cluster/health/getInstanceLiveProportion")
    public InstanceLiveProportionVo getInstanceLiveProportion(Integer instanceType, Long theClusterId) {
        return healthDataService.getInstanceLiveProportion(theClusterId, instanceType);
    }
}
