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


package org.apache.eventmesh.dashboard.core.remoting.kafka;

import org.apache.eventmesh.dashboard.common.enums.MetadataType;
import org.apache.eventmesh.dashboard.common.model.metadata.ConfigMetadata;
import org.apache.eventmesh.dashboard.common.model.remoting.BaseGlobalResult;
import org.apache.eventmesh.dashboard.common.model.remoting.config.AddConfigRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.config.ConfigType;
import org.apache.eventmesh.dashboard.common.model.remoting.config.DeleteConfigRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.config.GetConfigRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.config.GetConfigResult;
import org.apache.eventmesh.dashboard.common.model.remoting.config.UpdateConfigRequest;
import org.apache.eventmesh.dashboard.service.remoting.kafka.ConfigRemotingService;

import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.AlterConfigsOptions;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.DescribeClusterOptions;
import org.apache.kafka.clients.admin.DescribeConfigsOptions;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.common.config.ConfigResource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Kafka configuration actions registered through the independent Kafka service contract. */
public class KafkaConfigRemotingService extends AbstractKafkaRemotingService implements ConfigRemotingService {

    @Override
    public GetConfigResult getConfigs(GetConfigRequest request) throws Exception {
        if (request == null) {
            throw new IllegalArgumentException("Config request is required");
        }
        if (request.getMetaData() != null || request.getNode() != null
            || request.getConfigObjectName() != null || request.getConfigType() != ConfigType.NODE) {
            ConfigResource target = this.target(request.getConfigType(), request.getNode(), request.getConfigObjectName(), request.getMetaData());
            return this.describe(List.of(target));
        }
        // QUEUE_ALL supplies no per-resource metadata. Discover targets through the managed cluster client.
        List<ConfigResource> resources = new ArrayList<>();
        resources.add(new ConfigResource(ConfigResource.Type.BROKER, ""));
        var nodes = this.awaitResult(this.getClient().describeCluster(new DescribeClusterOptions().timeoutMs(ADMIN_TIMEOUT_MS)).nodes());
        var topics = this.awaitResult(this.getClient().listTopics(new ListTopicsOptions().listInternal(false).timeoutMs(ADMIN_TIMEOUT_MS)).names());
        if (nodes == null || topics == null) {
            throw new IllegalStateException("Kafka configuration targets are missing");
        }
        nodes.forEach(node -> resources.add(new ConfigResource(ConfigResource.Type.BROKER, Integer.toString(node.id()))));
        topics.forEach(topic -> resources.add(new ConfigResource(ConfigResource.Type.TOPIC, topic)));
        return this.describe(resources);
    }

    private GetConfigResult describe(Collection<ConfigResource> resources) throws Exception {
        Map<ConfigResource, Config> configs = this.awaitResult(this.getClient()
            .describeConfigs(resources, new DescribeConfigsOptions().timeoutMs(ADMIN_TIMEOUT_MS)).all());
        List<ConfigMetadata> entries = new ArrayList<>();
        for (ConfigResource resource : resources) {
            Config config = configs == null ? null : configs.get(resource);
            if (config == null) {
                throw new IllegalStateException("Kafka configuration response is incomplete: " + resource);
            }
            for (ConfigEntry entry : config.entries()) {
                ConfigMetadata converted = new ConfigMetadata();
                converted.setInstanceType(resource.type() == ConfigResource.Type.TOPIC ? MetadataType.TOPIC
                    : resource.name().isEmpty() ? MetadataType.CLUSTER : MetadataType.RUNTIME);
                converted.setInstanceName(resource.name());
                converted.setConfigName(entry.name());
                converted.setConfigValue(entry.isSensitive() ? null : entry.value());
                converted.setSensitive(entry.isSensitive());
                converted.setReadOnly(entry.isReadOnly());
                converted.setSource(entry.source().name());
                entries.add(converted);
            }
        }
        entries.sort(Comparator.comparing(ConfigMetadata::nodeUnique));
        GetConfigResult result = new GetConfigResult();
        result.setCode(200);
        result.setData(entries);
        return result;
    }

    @Override
    public BaseGlobalResult updateConfigs(UpdateConfigRequest request) throws Exception {
        return this.alterConfigs(request, AlterConfigOp.OpType.SET);
    }

    @Override
    public BaseGlobalResult deleteConfigs(DeleteConfigRequest request) throws Exception {
        return this.alterConfigs(request, AlterConfigOp.OpType.DELETE);
    }

    private BaseGlobalResult alterConfigs(AddConfigRequest request, AlterConfigOp.OpType operation) throws Exception {
        if (request == null) {
            throw new IllegalArgumentException("Config request is required");
        }
        final ConfigResource resource = this.target(request.getConfigType(), request.getNode(), request.getConfigObjectName(), request.getMetaData());
        if (request.getFullConfig() != null) {
            throw new IllegalArgumentException("Full configuration replacement is not supported");
        }
        List<Object> entries = request.getIncrementConfig();
        if (request.getMetaData() != null) {
            if (entries != null) {
                throw new IllegalArgumentException("Specify metaData or incrementConfig, not both");
            }
            entries = List.of(request.getMetaData());
        }
        if (entries == null || entries.isEmpty()) {
            throw new IllegalArgumentException("At least one ConfigMetadata is required");
        }
        Collection<AlterConfigOp> operations = new ArrayList<>();
        for (Object entry : entries) {
            if (!(entry instanceof ConfigMetadata)) {
                throw new IllegalArgumentException("incrementConfig must contain ConfigMetadata");
            }
            ConfigMetadata config = (ConfigMetadata) entry;
            String key = config.getConfigName();
            if (key == null || key.isBlank() || !key.equals(key.trim())
                || operation == AlterConfigOp.OpType.SET && config.getConfigValue() == null) {
                throw new IllegalArgumentException("Config keys must be nonblank and SET values must be non-null");
            }
            String value = operation == AlterConfigOp.OpType.DELETE ? null : config.getConfigValue();
            operations.add(new AlterConfigOp(new ConfigEntry(key, value), operation));
        }
        this.awaitResult(this.getClient().incrementalAlterConfigs(Map.of(resource, operations),
            new AlterConfigsOptions().timeoutMs(ADMIN_TIMEOUT_MS)).all());
        return this.successfulResult();
    }

    private ConfigResource target(ConfigType type, String node, String objectName, ConfigMetadata metadata) {
        ConfigResource explicit = node != null || objectName != null || type != ConfigType.NODE
            ? this.resource(type, node, objectName) : null;
        ConfigResource embedded = null;
        if (metadata != null && (metadata.getInstanceType() != null || metadata.getInstanceName() != null)) {
            MetadataType instanceType = metadata.getInstanceType();
            String name = metadata.getInstanceName();
            if (instanceType == MetadataType.TOPIC) {
                embedded = this.resource(ConfigType.TOPIC, null, name);
            } else if (instanceType == MetadataType.RUNTIME) {
                embedded = this.resource(ConfigType.NODE, this.requireName(name, "instanceName (broker ID)"), null);
            } else if (instanceType == MetadataType.CLUSTER && "".equals(name)) {
                embedded = this.resource(ConfigType.NODE, "", null);
            } else {
                throw new IllegalArgumentException("Config metadata requires a resolved TOPIC/RUNTIME/CLUSTER target name");
            }
        }
        if (explicit != null && embedded != null && !explicit.equals(embedded)) {
            throw new IllegalArgumentException("Request target conflicts with ConfigMetadata target");
        }
        if (explicit == null && embedded == null) {
            throw new IllegalArgumentException("Config target is required; instanceId is a database ID, not a Kafka resource name");
        }
        return embedded == null ? explicit : embedded;
    }

    private ConfigResource resource(ConfigType type, String node, String objectName) {
        if (type == ConfigType.TOPIC) {
            if (node != null) {
                throw new IllegalArgumentException("Topic configuration must not specify node");
            }
            return new ConfigResource(ConfigResource.Type.TOPIC, this.requireName(objectName, "configObjectName"));
        }
        if (type != ConfigType.NODE || node == null || objectName != null) {
            throw new IllegalArgumentException("NODE configuration requires node; an explicit empty node selects broker defaults");
        }
        if (!node.isEmpty()) {
            if (!node.matches("0|[1-9][0-9]*")) {
                throw new IllegalArgumentException("node must be a nonnegative broker ID");
            }
            Integer.parseInt(node);
        }
        return new ConfigResource(ConfigResource.Type.BROKER, node);
    }
}
