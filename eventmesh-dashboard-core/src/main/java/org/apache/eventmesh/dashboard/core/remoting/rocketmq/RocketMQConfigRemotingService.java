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
import org.apache.eventmesh.dashboard.common.model.remoting.BaseGlobalResult;
import org.apache.eventmesh.dashboard.common.model.remoting.RemotingOperate;
import org.apache.eventmesh.dashboard.common.model.remoting.config.AddConfigRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.config.ConfigType;
import org.apache.eventmesh.dashboard.common.model.remoting.config.DeleteConfigRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.config.GetConfigRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.config.GetConfigResult;
import org.apache.eventmesh.dashboard.service.remoting.ConfigRemotingService;

import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;
import org.apache.rocketmq.remoting.protocol.RequestCode;
import org.apache.rocketmq.remoting.protocol.ResponseCode;

import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/** Broker configuration patches; Topic configuration belongs to TopicRemotingService. */
public class RocketMQConfigRemotingService extends AbstractRocketMQRemotingService implements ConfigRemotingService {

    @Override
    public BaseGlobalResult addConfig(AddConfigRequest request) throws Exception {
        if (request == null) {
            throw new IllegalArgumentException("Config request is required");
        }
        this.requireNode(request.getConfigType());
        if (request instanceof DeleteConfigRequest || request.getRemotingOperate() == RemotingOperate.DELETE) {
            throw new UnsupportedOperationException("RocketMQ cannot delete Broker configuration keys; update an explicit value instead");
        }
        if (request.getRemotingOperate() != RemotingOperate.ADD && request.getRemotingOperate() != RemotingOperate.UPDATE) {
            throw new IllegalArgumentException("Config operation must be ADD or UPDATE");
        }
        if (request.getFullConfig() != null) {
            throw new IllegalArgumentException("Broker config accepts incremental patches, not full replacement");
        }
        Properties properties = new Properties();
        if (request.getMetaData() != null) {
            if (request.getIncrementConfig() != null) {
                throw new IllegalArgumentException("Supply either metadata or incrementConfig, not both");
            }
            this.addProperty(properties, request.getMetaData());
        } else if (request.getIncrementConfig() != null) {
            for (Object value : request.getIncrementConfig()) {
                if (!(value instanceof ConfigMetadata)) {
                    throw new IllegalArgumentException("incrementConfig entries must be ConfigMetadata");
                }
                this.addProperty(properties, (ConfigMetadata) value);
            }
        }
        if (properties.isEmpty()) {
            throw new IllegalArgumentException("At least one configuration entry is required");
        }
        // Properties.store escapes newlines, separators and backslashes; values cannot inject another key.
        StringWriter writer = new StringWriter();
        properties.store(writer, null);
        RemotingCommand command = RemotingCommand.createRequestCommand(RequestCode.UPDATE_BROKER_CONFIG, null);
        command.setBody(writer.toString().getBytes(StandardCharsets.UTF_8));
        return this.buildResult(this.invokeSync(command), new BaseGlobalResult());
    }

    @Override
    public BaseGlobalResult deleteConfig(DeleteConfigRequest request) {
        throw new UnsupportedOperationException("RocketMQ cannot delete Broker configuration keys; update an explicit value instead");
    }

    @Override
    public GetConfigResult getConfig(GetConfigRequest request) throws Exception {
        if (request == null) {
            throw new IllegalArgumentException("Config request is required");
        }
        String name = request.getMetaData() == null ? request.getConfigObjectName() : request.getMetaData().getConfigName();
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("Configuration key is required");
        }
        GetConfigResult result = this.getAllConfigs(request);
        if (result.getCode() == 200) {
            result.getData().removeIf(config -> !name.equals(config.getConfigName()));
        }
        return result;
    }

    @Override
    public GetConfigResult getAllConfigs(GetConfigRequest request) throws Exception {
        if (request != null) {
            this.requireNode(request.getConfigType());
            if (StringUtils.isNotBlank(request.getNode())) {
                throw new IllegalArgumentException("Broker address comes from the managed ADMIN client, not request.node");
            }
        }
        RemotingCommand response = this.invokeSync(RemotingCommand.createRequestCommand(RequestCode.GET_BROKER_CONFIG, null));
        GetConfigResult result = this.buildResult(response, new GetConfigResult());
        if (response.getCode() != ResponseCode.SUCCESS) {
            return result;
        }
        if (response.getBody() == null || response.getBody().length == 0) {
            throw new IllegalStateException("RocketMQ returned no Broker configuration body");
        }
        Properties properties = new Properties();
        properties.load(new StringReader(new String(response.getBody(), StandardCharsets.UTF_8)));
        List<ConfigMetadata> configs = new ArrayList<>();
        properties.stringPropertyNames().stream().sorted().forEach(name -> {
            ConfigMetadata config = new ConfigMetadata();
            config.setConfigName(name);
            config.setConfigValue(properties.getProperty(name));
            configs.add(config);
        });
        result.setData(configs);
        return result;
    }

    private void requireNode(ConfigType type) {
        if (type != ConfigType.NODE) {
            throw new UnsupportedOperationException("This service manages NODE configuration; use TopicRemotingService for Topics");
        }
    }

    private void addProperty(Properties properties, ConfigMetadata config) {
        if (StringUtils.isBlank(config.getConfigName()) || config.getConfigValue() == null) {
            throw new IllegalArgumentException("Configuration key and value are required");
        }
        if (properties.containsKey(config.getConfigName())) {
            throw new IllegalArgumentException("Duplicate configuration keys are not allowed");
        }
        properties.setProperty(config.getConfigName(), config.getConfigValue());
    }
}
