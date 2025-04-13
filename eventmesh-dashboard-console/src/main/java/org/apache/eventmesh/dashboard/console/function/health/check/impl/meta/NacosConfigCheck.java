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

import static org.apache.eventmesh.dashboard.common.constant.health.HealthConstant.NACOS_CHECK_CONTENT;
import static org.apache.eventmesh.dashboard.common.constant.health.HealthConstant.NACOS_CHECK_DATA_ID;
import static org.apache.eventmesh.dashboard.common.constant.health.HealthConstant.NACOS_CHECK_GROUP;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.console.function.health.annotation.HealthCheckType;
import org.apache.eventmesh.dashboard.console.function.health.callback.HealthCheckCallback;
import org.apache.eventmesh.dashboard.console.function.health.check.AbstractHealthCheckService;
import org.apache.eventmesh.dashboard.core.function.SDK.wrapper.NacosSDKWrapper;

import java.util.concurrent.CompletableFuture;

import com.alibaba.nacos.api.exception.NacosException;

import lombok.extern.slf4j.Slf4j;

/**
 * Interface to check the state of nacos
 */
@Slf4j
@HealthCheckType(clusterType = {ClusterType.EVENTMESH_META_NACOS})
public class NacosConfigCheck extends AbstractHealthCheckService<NacosSDKWrapper> {


    @Override
    public void doCheck(HealthCheckCallback callback) {
        CompletableFuture.runAsync(() -> {
            try {
                String content = this.getClient().getConfigService().getConfig(NACOS_CHECK_DATA_ID, NACOS_CHECK_GROUP, 3000);
                if (NACOS_CHECK_CONTENT.equals(content)) {
                    callback.onSuccess();
                } else {
                    callback.onFail(new RuntimeException("NacosCheck failed. Content is wrong."));
                }
            } catch (NacosException e) {
                callback.onFail(e);
            }
        });
    }

}
