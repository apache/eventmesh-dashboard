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

package org.apache.eventmesh.dashboard.core.config;

import org.apache.eventmesh.dashboard.core.constant.ConfigConst;
import org.apache.eventmesh.dashboard.core.service.ConnectionService;
import org.apache.eventmesh.dashboard.core.service.SubscriptionService;
import org.apache.eventmesh.dashboard.core.service.TopicService;
import org.apache.eventmesh.dashboard.core.service.meta.EtcdConnectionService;
import org.apache.eventmesh.dashboard.core.service.meta.EtcdSubscriptionService;
import org.apache.eventmesh.dashboard.core.service.meta.NacosConnectionService;
import org.apache.eventmesh.dashboard.core.service.meta.NacosSubscriptionService;
import org.apache.eventmesh.dashboard.core.service.store.RocketmqTopicService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Use different registry SDK depending on the configured meta type;
 * Use different storage SDK depending on the configured storage type.
 */
@Configuration
public class BeanTypeConfig {

    private final AdminProperties adminProperties;

    public BeanTypeConfig(AdminProperties adminProperties) {
        this.adminProperties = adminProperties;
    }

    @Bean
    public ConnectionService connectionService() {
        switch (adminProperties.getMeta().getType()) {
            case ConfigConst.META_TYPE_NACOS:
                return new NacosConnectionService(adminProperties);
            case ConfigConst.META_TYPE_ETCD:
                return new EtcdConnectionService();
            default:
                throw new IllegalArgumentException("Unsupported EventMesh Meta type: " + adminProperties.getMeta().getType());
        }
    }

    @Bean
    public SubscriptionService subscriptionService() {
        switch (adminProperties.getMeta().getType()) {
            case ConfigConst.META_TYPE_NACOS:
                return new NacosSubscriptionService(adminProperties);
            case ConfigConst.META_TYPE_ETCD:
                return new EtcdSubscriptionService();
            default:
                throw new IllegalArgumentException("Unsupported EventMesh Meta type: " + adminProperties.getMeta().getType());
        }
    }

    @Bean
    public TopicService topicService() {
        switch (adminProperties.getStore().getType()) {
            case ConfigConst.STORE_TYPE_STANDALONE:
                return null; // TODO StandaloneTopicService
            case ConfigConst.STORE_TYPE_ROCKETMQ:
                return new RocketmqTopicService(adminProperties);
            case ConfigConst.STORE_TYPE_KAFKA:
                return null; // TODO KafkaTopicService
            default:
                throw new IllegalArgumentException("Unsupported EventMesh Store type: " + adminProperties.getStore().getType());
        }
    }
}