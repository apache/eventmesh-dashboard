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

import org.apache.eventmesh.dashboard.common.model.metadata.ConfigMetadata;
import org.apache.eventmesh.dashboard.common.model.remoting.RemotingOperate;
import org.apache.eventmesh.dashboard.common.model.remoting.config.AddConfigRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.config.ConfigType;
import org.apache.eventmesh.dashboard.common.model.remoting.config.DeleteConfigRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.config.GetConfigRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.config.GetConfigResult;
import org.apache.eventmesh.dashboard.common.model.remoting.config.UpdateConfigRequest;
import org.apache.eventmesh.dashboard.core.function.SDK.ClientWrapper;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKTypeEnum;
import org.apache.eventmesh.dashboard.core.function.SDK.operation.rocketmq.RocketMQRemotingSDKOperation.DefaultRemotingClient;

import org.apache.rocketmq.remoting.exception.RemotingTimeoutException;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;
import org.apache.rocketmq.remoting.protocol.RequestCode;
import org.apache.rocketmq.remoting.protocol.ResponseCode;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class RocketMQConfigRemotingServiceTest {

    private DefaultRemotingClient client;
    private RocketMQConfigRemotingService service;

    @BeforeEach
    void setUp() throws Exception {
        this.client = Mockito.mock(DefaultRemotingClient.class);
        ClientWrapper wrapper = new ClientWrapper();
        wrapper.getClientMap().put(SDKTypeEnum.ADMIN, this.client);
        this.service = new RocketMQConfigRemotingService();
        this.service.setClientWrapper(wrapper);
        this.respond(ResponseCode.SUCCESS, null);
    }

    private void respond(int code, String body) throws Exception {
        RemotingCommand response = RemotingCommand.createResponseCommand(code, "test-result");
        response.setBody(body == null ? null : body.getBytes(StandardCharsets.UTF_8));
        Mockito.when(this.client.invokeSync(ArgumentMatchers.any(), ArgumentMatchers.eq(3000L))).thenReturn(response);
    }

    private RemotingCommand captured() throws Exception {
        ArgumentCaptor<RemotingCommand> captor = ArgumentCaptor.forClass(RemotingCommand.class);
        Mockito.verify(this.client).invokeSync(captor.capture(), ArgumentMatchers.eq(3000L));
        return captor.getValue();
    }

    @Test
    void writesEscapedIncrementalProperties() throws Exception {
        log.info("【配置模拟测试】更新指定键值，验证转义和请求码");
        UpdateConfigRequest request = new UpdateConfigRequest();
        request.setMetaData(this.config("testKey", "中文\nother=value\\suffix"));
        Assertions.assertEquals(200, this.service.updateConfig(request).getCode());
        RemotingCommand command = this.captured();
        Assertions.assertEquals(RequestCode.UPDATE_BROKER_CONFIG, command.getCode());
        Properties properties = new Properties();
        properties.load(new StringReader(new String(command.getBody(), StandardCharsets.UTF_8)));
        Assertions.assertEquals(1, properties.size());
        Assertions.assertEquals(request.getMetaData().getConfigValue(), properties.getProperty("testKey"));
    }

    @Test
    void supportsTypedBatchPatches() throws Exception {
        log.info("【配置模拟测试】批量写入两个配置项");
        AddConfigRequest request = new AddConfigRequest();
        request.setIncrementConfig(List.of(this.config("a", "1"), this.config("b", "2")));
        Assertions.assertEquals(200, this.service.addConfig(request).getCode());
        Properties properties = new Properties();
        properties.load(new StringReader(new String(this.captured().getBody(), StandardCharsets.UTF_8)));
        Assertions.assertEquals(2, properties.size());
    }

    @Test
    void queriesConfigurationInsteadOfTopics() throws Exception {
        log.info("【配置模拟测试】查询全部配置并验证排序及 UTF-8");
        this.respond(ResponseCode.SUCCESS, "z=末尾\na=first\n");
        GetConfigResult result = this.service.getAllConfigs(new GetConfigRequest());
        Assertions.assertEquals(List.of("a", "z"), result.getData().stream().map(ConfigMetadata::getConfigName).toList());
        Assertions.assertEquals("末尾", result.getData().get(1).getConfigValue());
        Assertions.assertEquals(RequestCode.GET_BROKER_CONFIG, this.captured().getCode());
    }

    @Test
    void filtersOneKeyAndReturnsEmptyForMissingKey() throws Exception {
        log.info("【配置模拟测试】查询指定配置键及不存在的键");
        this.respond(ResponseCode.SUCCESS, "a=1\nb=2\n");
        GetConfigRequest request = new GetConfigRequest(ConfigType.NODE, null, "a");
        Assertions.assertEquals("1", this.service.getConfig(request).getData().get(0).getConfigValue());
        request.setConfigObjectName("missing");
        Assertions.assertTrue(this.service.getConfig(request).getData().isEmpty());
    }

    @Test
    void rejectsDeleteAndTopicScopeWithoutSending() {
        log.info("【配置模拟测试】禁止伪造配置删除及 Topic 操作");
        Assertions.assertThrows(UnsupportedOperationException.class, () -> this.service.deleteConfig(new DeleteConfigRequest()));
        AddConfigRequest request = new AddConfigRequest();
        request.setConfigType(ConfigType.TOPIC);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> this.service.addConfig(request));
        DeleteConfigRequest delete = new DeleteConfigRequest();
        delete.setRemotingOperate(RemotingOperate.ADD);
        delete.setMetaData(this.config("a", "1"));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> this.service.deleteConfig(delete));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> this.service.addConfig(delete));
        Mockito.verifyNoInteractions(this.client);
    }

    @Test
    void rejectsAmbiguousOrInvalidPatches() {
        log.info("【配置模拟测试】拒绝空更新、重复键和全量替换");
        AddConfigRequest request = new AddConfigRequest();
        Assertions.assertThrows(IllegalArgumentException.class, () -> this.service.addConfig(request));
        request.setIncrementConfig(List.of(this.config("a", "1"), this.config("a", "2")));
        Assertions.assertThrows(IllegalArgumentException.class, () -> this.service.addConfig(request));
        request.setFullConfig(List.of());
        Assertions.assertThrows(IllegalArgumentException.class, () -> this.service.addConfig(request));
        Mockito.verifyNoInteractions(this.client);
    }

    @Test
    void propagatesBrokerFailureAndTimeout() throws Exception {
        log.info("【配置模拟测试】保留 Broker 拒绝和网络超时");
        this.respond(ResponseCode.NO_PERMISSION, null);
        GetConfigResult result = this.service.getAllConfigs(null);
        Assertions.assertEquals(10000 + ResponseCode.NO_PERMISSION, result.getCode());
        Assertions.assertNull(result.getData());
        Mockito.when(this.client.invokeSync(ArgumentMatchers.any(), ArgumentMatchers.eq(3000L)))
            .thenThrow(new RemotingTimeoutException("test-broker", 3000));
        Assertions.assertThrows(RemotingTimeoutException.class, () -> this.service.getAllConfigs(null));
    }

    @Test
    void rejectsMissingBodyAndAddressOverride() {
        log.info("【配置模拟测试】空响应不能当作完整配置，地址由托管客户端决定");
        Assertions.assertThrows(IllegalStateException.class, () -> this.service.getAllConfigs(null));
        GetConfigRequest request = new GetConfigRequest(ConfigType.NODE, "other:10911", null);
        Assertions.assertThrows(IllegalArgumentException.class, () -> this.service.getAllConfigs(request));
    }

    private ConfigMetadata config(String key, String value) {
        ConfigMetadata config = new ConfigMetadata();
        config.setConfigName(key);
        config.setConfigValue(value);
        return config;
    }

}
