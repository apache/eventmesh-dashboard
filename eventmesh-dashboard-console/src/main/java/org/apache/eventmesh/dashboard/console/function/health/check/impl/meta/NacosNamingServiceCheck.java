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


package org.apache.eventmesh.dashboard.console.function.health.check.impl.meta;

import static org.apache.eventmesh.dashboard.common.constant.health.HealthConstant.NACOS_CHECK_SERVICE_NAME;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.enums.health.HealthCheckTypeEnum;
import org.apache.eventmesh.dashboard.console.function.health.annotation.HealthCheckType;
import org.apache.eventmesh.dashboard.console.function.health.callback.HealthCheckCallback;
import org.apache.eventmesh.dashboard.console.function.health.check.AbstractHealthCheckService;
import org.apache.eventmesh.dashboard.core.function.SDK.wrapper.NacosSDKWrapper;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.pojo.Instance;

import lombok.extern.slf4j.Slf4j;

/**
 * not used for now, recommend to use NacosConfigCheck to check health status of nacos
 */

@Slf4j
@HealthCheckType(clusterType = {ClusterType.EVENTMESH_META_NACOS}, healthType = HealthCheckTypeEnum.PING)
public class NacosNamingServiceCheck extends AbstractHealthCheckService<NacosSDKWrapper> {


    @Override
    public void doCheck(HealthCheckCallback callback) {
        this.completableFuture(() -> {
            try {
                Instance result = this.getClient().getNamingService().selectOneHealthyInstance(NACOS_CHECK_SERVICE_NAME);
                if (result.isHealthy()) {
                    callback.onSuccess();
                } else {
                    callback.onFail(new RuntimeException("NacosCheck failed. Service is not healthy."));
                }
            } catch (NacosException e) {
                callback.onFail(e);
            }
        });
    }
}
