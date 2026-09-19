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


package org.apache.eventmesh.dashboard.console.function.report.collect;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.model.metadata.ClusterMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;
import org.apache.eventmesh.dashboard.console.function.report.ReportConfig.ReportEngineConfig;
import org.apache.eventmesh.dashboard.console.function.report.iotdb.RocketMQIotdbReportEngine;
import org.apache.eventmesh.dashboard.core.function.SDK.ConfigManage;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKManage;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKTypeEnum;
import org.apache.eventmesh.dashboard.core.function.SDK.config.AbstractSimpleCreateSDKConfig;
import org.apache.eventmesh.dashboard.core.function.SDK.config.NetAddress;
import org.apache.eventmesh.dashboard.core.function.SDK.operation.rocketmq.RocketMQRemotingSDKOperation.DefaultRemotingClient;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Explicit standalone registration; no changes to existing Spring wiring, SDK lifecycle or MySQL metadata. */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "rocketmq.metrics.enabled", havingValue = "true")
public class RocketMQMetricsConfiguration {
    @Bean(destroyMethod = "close")
    public RocketMQIotdbReportEngine rocketMQMetricsEngine(Environment environment) {
        ReportEngineConfig config = new ReportEngineConfig();
        config.setEngineAddress(environment.getRequiredProperty("rocketmq.metrics.iotdb-address"));
        RocketMQIotdbReportEngine engine = new RocketMQIotdbReportEngine();
        engine.setReportEngineConfig(config);
        engine.init();
        engine.createReport("*");
        return engine;
    }

    @Bean
    public RuntimeMetadata rocketMQMetricsRuntime(Environment environment) {
        RuntimeMetadata runtime = new RuntimeMetadata();
        runtime.setId(environment.getRequiredProperty("rocketmq.metrics.runtime-id", Long.class));
        runtime.setClusterId(environment.getRequiredProperty("rocketmq.metrics.cluster-id", Long.class));
        runtime.setOrganizationId(environment.getRequiredProperty("rocketmq.metrics.organization-id", Long.class));
        runtime.setHost(environment.getRequiredProperty("rocketmq.metrics.broker-host"));
        runtime.setPort(environment.getRequiredProperty("rocketmq.metrics.broker-port", Integer.class));
        runtime.setName(runtime.getHost() + ":" + runtime.getPort());
        runtime.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE);
        runtime.setStatus(1L);
        return runtime;
    }

    @Bean(destroyMethod = "shutdown")
    public DefaultRemotingClient rocketMQMetricsClient(RuntimeMetadata rocketMQMetricsRuntime) {
        AbstractSimpleCreateSDKConfig config = ConfigManage.getInstance().getSimpleCreateSdkConfig(
            rocketMQMetricsRuntime.getClusterType(), SDKTypeEnum.ADMIN);
        NetAddress address = new NetAddress();
        address.setAddress(rocketMQMetricsRuntime.getHost());
        address.setPort(rocketMQMetricsRuntime.getPort());
        config.setNetAddress(address);
        SDKManage.getInstance().createClient(SDKTypeEnum.ADMIN, rocketMQMetricsRuntime, config, rocketMQMetricsRuntime.getClusterType());
        return SDKManage.getInstance().getClient(SDKTypeEnum.ADMIN, rocketMQMetricsRuntime.getUnique());
    }

    @Bean(destroyMethod = "")
    public RocketMQDataSyncHandler rocketMQMetricsDataSyncHandler() {
        return new RocketMQDataSyncHandler();
    }

    @Bean(destroyMethod = "close")
    public RocketMQCollectManage rocketMQMetricsManage(RuntimeMetadata rocketMQMetricsRuntime,
        DefaultRemotingClient rocketMQMetricsClient) {
        ClusterMetadata cluster = new ClusterMetadata();
        cluster.setId(rocketMQMetricsRuntime.getClusterId());
        cluster.setOrganizationId(rocketMQMetricsRuntime.getOrganizationId());
        cluster.setName("rocketmq-" + cluster.getId());
        cluster.setClusterType(rocketMQMetricsRuntime.getClusterType());
        cluster.setStatus(1L);
        RocketMQCollectManage manager = new RocketMQCollectManage();
        manager.register(cluster, rocketMQMetricsRuntime);
        return manager;
    }

    @Bean
    public RocketMQMetricsCollectTask rocketMQMetricsCollectTask() {
        return new RocketMQMetricsCollectTask();
    }
}
