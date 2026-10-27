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


package org.apache.eventmesh.dashboard.core.remoting.rocketmq;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.model.metadata.ConfigMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;
import org.apache.eventmesh.dashboard.common.model.remoting.config.ConfigType;
import org.apache.eventmesh.dashboard.common.model.remoting.config.DeleteConfigRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.config.GetConfigRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.config.UpdateConfigRequest;
import org.apache.eventmesh.dashboard.core.function.SDK.ConfigManage;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKManage;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKTypeEnum;
import org.apache.eventmesh.dashboard.core.function.SDK.config.AbstractSimpleCreateSDKConfig;
import org.apache.eventmesh.dashboard.core.function.SDK.config.NetAddress;
import org.apache.eventmesh.dashboard.core.function.SDK.operation.rocketmq.RocketMQRemotingSDKOperation.DefaultRemotingClient;
import org.apache.eventmesh.dashboard.core.remoting.Remoting2Manage;
import org.apache.eventmesh.dashboard.service.remoting.ConfigRemotingService;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import lombok.extern.slf4j.Slf4j;

/** Requires the isolated RocketMQ 5.4.0 test Broker; no production resources are used. */
@Slf4j
class RocketMQConfigIntegrationTest {

    private RuntimeMetadata runtime;
    private DefaultRemotingClient client;
    private String name;
    private ConfigRemotingService service;

    private String originalValue;

    @BeforeEach
    void setUp() throws Exception {
        this.runtime = new RuntimeMetadata();
        this.runtime.setId(99502L);
        this.runtime.setClusterId(99500L);
        this.runtime.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE);
        AbstractSimpleCreateSDKConfig config = ConfigManage.getInstance().getSimpleCreateSdkConfig(this.runtime.getClusterType(), SDKTypeEnum.ADMIN);
        NetAddress address = new NetAddress();
        address.setAddress(System.getProperty("rocketmq.admin.broker.host", "127.0.0.1"));
        address.setPort(Integer.getInteger("rocketmq.admin.broker.port", 21911));
        config.setNetAddress(address);
        SDKManage.getInstance().createClient(SDKTypeEnum.ADMIN, this.runtime, config, this.runtime.getClusterType());
        this.client = SDKManage.getInstance().getClient(SDKTypeEnum.ADMIN, this.runtime.getUnique());
        this.name = "dashboard_admin_" + UUID.randomUUID().toString().replace("-", "");
        this.service = Remoting2Manage.getInstance().createRemotingService(ConfigRemotingService.class, this.runtime);
    }

    @AfterEach
    void tearDown() throws Exception {
        try {
            if (this.originalValue != null) {
                UpdateConfigRequest request = new UpdateConfigRequest();
                request.setMetaData(this.config(this.originalValue));
                Assertions.assertEquals(200, this.service.updateConfig(request).getCode());
                Assertions.assertEquals(this.originalValue, this.readValue());
                log.info("【真实配置测试】已恢复测试前的配置值");
            }
        } finally {
            try {
                if (this.client != null) {
                    this.client.shutdown();
                }
            } finally {
                if (this.runtime != null) {
                    SDKManage.getInstance().deleteClient(null, this.runtime.getUnique());
                }
            }
        }
    }

    @Test
    void queryBrokerConfigurationThroughFramework() {
        log.info("【真实配置测试】框架查询 Broker 配置，返回配置列表");
        List<?> configs = Remoting2Manage.getInstance().createDataMetadataHandler(ConfigRemotingService.class, this.runtime).getData();
        Assertions.assertTrue(configs.stream().map(ConfigMetadata.class::cast).anyMatch(c -> "brokerName".equals(c.getName())));
    }

    @Test
    void updateAndRestoreBrokerConfiguration() throws Exception {
        log.info("【真实配置测试】通过框架更新 commercialBaseCount 并查询验证，结束后恢复");
        this.originalValue = this.readValue();
        String updated = String.valueOf(Integer.parseInt(this.originalValue) + 1);
        Remoting2Manage.getInstance().createDataMetadataHandler(ConfigRemotingService.class, this.runtime)
            .handleAll(null, null, List.of(this.config(updated)), null);
        Assertions.assertEquals(updated, this.readValue());
    }

    @Test
    void deleteIsExplicitlyUnsupported() {
        log.info("【真实配置测试】Broker 配置删除明确报不支持");
        Assertions.assertThrows(UnsupportedOperationException.class, () -> this.service.deleteConfig(new DeleteConfigRequest()));
    }

    private String readValue() throws Exception {
        return this.service.getConfig(new GetConfigRequest(ConfigType.NODE, null, "commercialBaseCount"))
            .getData().get(0).getConfigValue();
    }

    private ConfigMetadata config(String value) {
        ConfigMetadata config = new ConfigMetadata();
        config.setId(99520L);
        config.setName("commercialBaseCount");
        config.setConfigValue(value);
        return config;
    }

}
