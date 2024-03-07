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

package org.apache.eventmesh.dashboard.console.service.config.synchronous;

import org.apache.eventmesh.dashboard.console.entity.config.ConfigEntity;
import org.apache.eventmesh.dashboard.console.entity.topic.TopicEntity;
import org.apache.eventmesh.dashboard.console.service.config.ConfigService;
import org.apache.eventmesh.dashboard.console.service.config.instanceoperation.TopicConfigController;
import org.apache.eventmesh.dashboard.console.service.topic.TopicService;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class SynchronousTopicConfigDBToInstanceTask {

    @Autowired
    private TopicService topicService;

    @Autowired
    private TopicConfigController topicConfigController;

    @Autowired
    private ConfigService configService;

    public void synchronousTopicConfig(Long clusterId) {
        List<TopicEntity> topicEntityList = topicService.selectTopiByCluster(clusterId);
        for (TopicEntity topicEntity : topicEntityList) {

            ConcurrentHashMap<String, String> topicConfigMapFromInstance = this.configListToMap(
                topicConfigController.getTopicConfigFromInstance(clusterId, topicEntity.getTopicName()));

            ConfigEntity configEntity = this.getConfigEntityBelongInstance(clusterId, topicEntity.getId());

            ConcurrentHashMap<String, String> topicConfigMapFromDb = this.configListToMap(configService.selectByInstanceId(configEntity));

            ConcurrentHashMap<String, String> updateConfigMap = new ConcurrentHashMap<>();

            topicConfigMapFromInstance.entrySet().forEach(n -> {
                if (topicConfigMapFromDb.remove(n.getKey(), n.getValue())) {
                    topicConfigMapFromInstance.remove(n.getKey());
                }
                if (topicConfigMapFromDb.get(n.getKey()) != null) {
                    updateConfigMap.put(n.getKey(), topicConfigMapFromDb.get(n.getKey()));
                    topicConfigMapFromInstance.remove(n.getKey());
                    topicConfigMapFromDb.remove(n.getKey());
                }
            });
            //add  topicConfigMapFromDb

            //update  updateConfigMap

            //delete topicConfigMapFromInstance
        }
    }

    private ConcurrentHashMap<String, String> configListToMap(List<ConfigEntity> configEntityList) {
        ConcurrentHashMap<String, String> topicConfigMap = new ConcurrentHashMap<>();
        configEntityList.forEach(n -> {
                topicConfigMap.put(n.getConfigName(), n.getConfigValue());
            }
        );
        return topicConfigMap;
    }


    private ConfigEntity getConfigEntityBelongInstance(Long clusterId, Long id) {
        ConfigEntity configEntity = new ConfigEntity();
        configEntity.setClusterId(clusterId);
        configEntity.setInstanceId(id);
        configEntity.setInstanceType(1);
        return configEntity;
    }
}
