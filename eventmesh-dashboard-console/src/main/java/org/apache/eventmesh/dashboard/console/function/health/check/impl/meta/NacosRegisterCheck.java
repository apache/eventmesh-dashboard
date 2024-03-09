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

import static org.apache.eventmesh.dashboard.console.constant.health.HealthCheckTypeConstant.HEALTH_CHECK_SUBTYPE_NACOS_REGISTER;
import static org.apache.eventmesh.dashboard.console.constant.health.HealthCheckTypeConstant.HEALTH_CHECK_TYPE_META;
import static org.apache.eventmesh.dashboard.console.constant.health.HealthConstant.NACOS_CHECK_SERVICE_CLUSTER_NAME;
import static org.apache.eventmesh.dashboard.console.constant.health.HealthConstant.NACOS_CHECK_SERVICE_NAME;

import org.apache.eventmesh.dashboard.console.function.health.annotation.HealthCheckType;
import org.apache.eventmesh.dashboard.console.function.health.callback.HealthCheckCallback;
import org.apache.eventmesh.dashboard.console.function.health.check.AbstractHealthCheckService;
import org.apache.eventmesh.dashboard.console.function.health.check.config.HealthCheckObjectConfig;

import java.util.Properties;
import java.util.concurrent.CompletableFuture;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;

import lombok.extern.slf4j.Slf4j;


@Slf4j
@HealthCheckType(type = HEALTH_CHECK_TYPE_META, subType = HEALTH_CHECK_SUBTYPE_NACOS_REGISTER)
public class NacosRegisterCheck extends AbstractHealthCheckService {

    private NamingService namingService;

    public NacosRegisterCheck(HealthCheckObjectConfig healthCheckObjectConfig) {
        super(healthCheckObjectConfig);
    }

    @Override
    public void doCheck(HealthCheckCallback callback) {
        CompletableFuture.runAsync(() -> {
            try {
                Instance result = namingService.selectOneHealthyInstance(NACOS_CHECK_SERVICE_NAME);
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

    @Override
    public void init() {
        try {
            Properties properties = new Properties();
            properties.put("serverAddr", getConfig().getConnectUrl());
            namingService = NamingFactory.createNamingService(properties);
            namingService.registerInstance(NACOS_CHECK_SERVICE_NAME, "11.11.11.11", 8888, NACOS_CHECK_SERVICE_CLUSTER_NAME);
        } catch (NacosException e) {
            log.error("NacosRegisterCheck init failed", e);
        }
    }

    @Override
    public void destroy() {
        try {
            namingService.deregisterInstance(NACOS_CHECK_SERVICE_NAME, "11.11.11.11", 8888, NACOS_CHECK_SERVICE_CLUSTER_NAME);
        } catch (NacosException e) {
            log.error("NacosRegisterCheck destroy failed", e);
        }
    }
}
