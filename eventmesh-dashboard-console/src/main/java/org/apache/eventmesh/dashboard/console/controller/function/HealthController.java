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

package org.apache.eventmesh.dashboard.console.controller.function;

import org.apache.eventmesh.dashboard.console.entity.function.HealthCheckResultEntity;
import org.apache.eventmesh.dashboard.console.modle.vo.health.InstanceLiveProportionVo;
import org.apache.eventmesh.dashboard.console.service.function.HealthDataService;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cluster/health")
public class HealthController {

    @Autowired
    HealthDataService healthDataService;

    @GetMapping("/getHistoryLiveStatus")
    public List<HealthCheckResultEntity> getHistoryLiveStatusById(Integer type, Long instanceId, String startTime) {
        return healthDataService.selectInstanceLiveStatusHistory(type, instanceId, LocalDateTime.parse(startTime));
    }

    @GetMapping("/getInstanceLiveProportion")
    public InstanceLiveProportionVo getInstanceLiveProportion(Integer instanceType, Long theClusterId) {
        return healthDataService.selectInstanceLiveProportion(theClusterId, instanceType);
    }
}
