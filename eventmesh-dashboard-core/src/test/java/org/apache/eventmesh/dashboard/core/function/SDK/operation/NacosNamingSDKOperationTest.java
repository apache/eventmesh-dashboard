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

package org.apache.eventmesh.dashboard.core.function.SDK.operation;

import org.apache.eventmesh.dashboard.core.function.SDK.SDKManager;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKTypeEnum;
import org.apache.eventmesh.dashboard.core.function.SDK.config.CreateNacosConfig;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Timeout(value = 5)
class NacosNamingSDKOperationTest {

    private NacosNamingSDKOperation nacosNamingSDKOperation = new NacosNamingSDKOperation();

    @Test
    public void testCreateNamingService() throws NacosException, InterruptedException {
        try {
            CreateNacosConfig createClientConfig = new CreateNacosConfig();
            createClientConfig.setServerAddress("127.0.0.1:8848");
            createClientConfig.setUsername("nacos");
            createClientConfig.setPassword("nacos");
            NamingService namingService = (NamingService) SDKManager.getInstance().createClient(SDKTypeEnum.META_NACOS_NAMING, createClientConfig)
                .getValue();
            namingService.registerInstance("eventmesh-dashboard-sdk-nacos-test", "192.168.11.11", 8888,
                "eventmesh-dashboard-sdk-nacos-test-cluster-name");
            namingService.deregisterInstance("eventmesh-dashboard-sdk-nacos-test", "192.168.11.11", 8888,
                "eventmesh-dashboard-sdk-nacos-test-cluster-name");
        } catch (Exception e) {
            log.error("create nacos naming service failed", e);
        }
    }
}