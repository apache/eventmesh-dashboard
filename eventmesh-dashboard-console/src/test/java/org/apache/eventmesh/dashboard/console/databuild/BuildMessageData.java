/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.eventmesh.dashboard.console.databuild;


import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.enums.MetadataType;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.entity.function.ConfigEntity;
import org.apache.eventmesh.dashboard.console.entity.message.GroupEntity;
import org.apache.eventmesh.dashboard.console.entity.message.SubscriptionEntity;
import org.apache.eventmesh.dashboard.console.entity.message.TopicEntity;

import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import lombok.Getter;

@Getter
public class BuildMessageData {

    private Map<ClusterType, List<ConfigEntity>> clusterTypeListHashMap = new HashMap<>();

    private Map<ClusterType, Map<MetadataType, List<ClusterEntity>>> entityMapHashMap = new HashMap<>();

    private List<ConfigEntity> configEntityList = new ArrayList<>();

    private List<TopicEntity> topicEntityList = new ArrayList<>();

    private List<Pair<List<TopicEntity>, List<GroupEntity>>> pairList = new ArrayList<>();

    private List<GroupEntity> groupEntityList = new ArrayList<>();

    private List<SubscriptionEntity> subscriptionEntityList = new ArrayList<>();


    public void buildCluster(ClusterEntity clusterEntity) {
        List<TopicEntity> topicEntityList = this.buildTopic(clusterEntity);
        List<GroupEntity> groupEntityList = this.buildGroup(clusterEntity);
        this.topicEntityList.addAll(topicEntityList);
        this.groupEntityList.addAll(groupEntityList);

        this.buildConfig(MetadataType.CLUSTER, clusterEntity, clusterEntity.getId());

        pairList.add(Pair.of(topicEntityList, groupEntityList));
    }

    public List<TopicEntity> buildTopic(ClusterEntity clusterEntity) {
        List<TopicEntity> topicEntityList = new ArrayList<>();
        int num = new Random().nextInt(10) + 10;
        for (int i = 0; i < num; i++) {
            TopicEntity topicEntity = new TopicEntity();
            topicEntity.setOrganizationId(clusterEntity.getOrganizationId());
            topicEntity.setClusterId(clusterEntity.getId());
            topicEntity.setRuntimeId(0L);
            topicEntity.setTopicType("test-data");
            topicEntity.setTopicName("cluster-" + clusterEntity.getId() + ".topic-" + i);

            if (Objects.equals(clusterEntity.getClusterType(), ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE) ||
                Objects.equals(clusterEntity.getClusterType(), ClusterType.STORAGE_ROCKETMQ_BROKER_RAFT)) {
                topicEntity.setReadQueueNum(8);
                topicEntity.setWriteQueueNum(8);
                topicEntity.setReplicationFactor(0);
                topicEntity.setOrder(i % 3 == 0 ? 1 : 2);
                topicEntity.setTopicFilterType("");


            } else {
                topicEntity.setReadQueueNum(8);
                topicEntity.setWriteQueueNum(0);
                topicEntity.setReplicationFactor(2);
                topicEntity.setOrder(0);
                topicEntity.setTopicFilterType("");

            }
            topicEntity.setAttributes("");

            topicEntity.setDescription(topicEntity.getTopicName());
            topicEntity.setCreateProgress(1);
            topicEntity.setRetentionMs(-1L);
            topicEntityList.add(topicEntity);
        }
        return topicEntityList;
    }


    public List<GroupEntity> buildGroup(ClusterEntity clusterEntity) {
        List<GroupEntity> groupEntityArrayList = new ArrayList<>();
        Random random = new Random();
        int num = random.nextInt(10) + 10;
        for (int i = 0; i < num; i++) {
            GroupEntity groupEntity = new GroupEntity();
            groupEntity.setOrganizationId(clusterEntity.getOrganizationId());
            groupEntity.setClusterId(clusterEntity.getId());

            groupEntity.setRuntimeId(0L);
            groupEntity.setName("cluster-" + clusterEntity.getId() + ".group-" + i);
            groupEntity.setType(random.nextInt(1));
            groupEntity.setOwnType("0");
            groupEntityList.add(groupEntity);
        }
        return groupEntityArrayList;
    }

    public void buildSubscription(ClusterEntity clusterEntity, List<TopicEntity> topicEntityList, List<GroupEntity> groupEntityList) {
        List<SubscriptionEntity> subscriptionEntityList = new ArrayList<>();
        topicEntityList.forEach(topicEntity -> {
            groupEntityList.forEach(groupEntity -> {
                SubscriptionEntity subscriptionEntity = new SubscriptionEntity();
                subscriptionEntity.setOrganizationId(groupEntity.getOrganizationId());
                subscriptionEntity.setClusterId(groupEntity.getClusterId());
                subscriptionEntity.setGroupName(groupEntity.getName());
                subscriptionEntity.setTopicName(topicEntity.getTopicName());
                subscriptionEntity.setEventMeshUser("11111");
                subscriptionEntity.setStatus(1L);
                subscriptionEntityList.add(subscriptionEntity);
            });
        });
        this.subscriptionEntityList.addAll(subscriptionEntityList);
    }

    public void buildConfig(MetadataType metadataType, ClusterEntity clusterEntity, Long id) {
        for (int i = 0; i < 15; i++) {
            ConfigEntity configEntity = new ConfigEntity();
            configEntity.setOrganizationId(clusterEntity.getOrganizationId());
            configEntity.setClusterId(clusterEntity.getId());
            configEntity.setInstanceType(metadataType);
            configEntity.setInstanceId(id);
            configEntity.setConfigType("test-data");
            configEntity.setConfigName("config~" + "id~" + id + "~index~" + i);
            configEntity.setConfigValue(String.valueOf(i));
            configEntity.setConfigValueType("number");
            configEntity.setConfigValueRange("{num}");
            configEntity.setStartVersion("2");
            configEntity.setEndVersion("1");
            configEntity.setDescription("test-data");
            configEntity.setEdit(0);
            configEntity.setIsDefault(1);

            this.configEntityList.add(configEntity);
        }
    }

}
