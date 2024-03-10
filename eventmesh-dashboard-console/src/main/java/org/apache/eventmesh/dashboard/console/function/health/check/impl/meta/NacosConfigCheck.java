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

import static org.apache.eventmesh.dashboard.console.constant.health.HealthCheckTypeConstant.HEALTH_CHECK_SUBTYPE_NACOS_CONFIG;
import static org.apache.eventmesh.dashboard.console.constant.health.HealthCheckTypeConstant.HEALTH_CHECK_TYPE_META;
import static org.apache.eventmesh.dashboard.console.constant.health.HealthConstant.NACOS_CHECK_CONTENT;
import static org.apache.eventmesh.dashboard.console.constant.health.HealthConstant.NACOS_CHECK_DATA_ID;
import static org.apache.eventmesh.dashboard.console.constant.health.HealthConstant.NACOS_CHECK_GROUP;

import org.apache.eventmesh.dashboard.console.function.health.annotation.HealthCheckType;
import org.apache.eventmesh.dashboard.console.function.health.callback.HealthCheckCallback;
import org.apache.eventmesh.dashboard.console.function.health.check.AbstractHealthCheckService;
import org.apache.eventmesh.dashboard.console.function.health.check.config.HealthCheckObjectConfig;

import java.util.Properties;
import java.util.concurrent.CompletableFuture;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@HealthCheckType(type = HEALTH_CHECK_TYPE_META, subType = HEALTH_CHECK_SUBTYPE_NACOS_CONFIG)
public class NacosConfigCheck extends AbstractHealthCheckService {

    private ConfigService configService;

    public NacosConfigCheck(HealthCheckObjectConfig healthCheckObjectConfig) {
        super(healthCheckObjectConfig);
    }

    @Override
    public void doCheck(HealthCheckCallback callback) {
        CompletableFuture.runAsync(() -> {
            try {
                String content = configService.getConfig(NACOS_CHECK_DATA_ID, NACOS_CHECK_GROUP, getConfig().getRequestTimeoutMillis());
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

    @Override
    public void init() {
        //create a config
        try {
            Properties properties = new Properties();
            properties.put("serverAddr", getConfig().getConnectUrl());
            ConfigService configService = NacosFactory.createConfigService(properties);
            boolean isPublishOk = configService.publishConfig(NACOS_CHECK_DATA_ID, NACOS_CHECK_GROUP,
                NACOS_CHECK_CONTENT);
            if (!isPublishOk) {
                log.error("NacosCheck init failed caused by crate config failed");
            }
        } catch (NacosException e) {
            log.error("NacosCheck init failed caused by {}", e.getErrMsg());
        }

        try {
            Properties properties = new Properties();
            properties.put("serverAddr", getConfig().getConnectUrl());
            configService = NacosFactory.createConfigService(properties);
        } catch (NacosException e) {
            log.error("NacosCheck init failed caused by {}", e.getErrMsg());
        }
    }

    @Override
    public void destroy() {
        if (configService != null) {
            try {
                configService.removeConfig(NACOS_CHECK_DATA_ID, NACOS_CHECK_GROUP);
            } catch (NacosException e) {
                log.error("NacosCheck destroy failed caused by {}", e.getErrMsg());
            }

        }
    }
}
