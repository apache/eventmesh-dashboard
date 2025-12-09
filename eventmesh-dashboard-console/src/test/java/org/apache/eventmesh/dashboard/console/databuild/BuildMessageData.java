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

package org.apache.eventmesh.dashboard.console.databuild;


import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.enums.MetadataType;
import org.apache.eventmesh.dashboard.console.entity.base.BaseSyncEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.entity.function.ConfigEntity;
import org.apache.eventmesh.dashboard.console.entity.message.GroupEntity;
import org.apache.eventmesh.dashboard.console.entity.message.GroupMemberEntity;
import org.apache.eventmesh.dashboard.console.entity.message.TopicEntity;

import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import lombok.Getter;

/**
 * 可以删除
 */
@Getter
public class BuildMessageData {

    @Deprecated
    private final Map<ClusterType, List<ConfigEntity>> clusterTypeListHashMap = new HashMap<>();

    @Deprecated
    private final Map<ClusterType, Map<MetadataType, List<ClusterEntity>>> entityMapHashMap = new HashMap<>();

    private final List<ConfigEntity> configEntityList = new ArrayList<>();

    private final List<TopicEntity> topicEntityList = new ArrayList<>();

    @Deprecated
    private final List<Pair<List<TopicEntity>, List<GroupEntity>>> pairList = new ArrayList<>();

    private final List<GroupEntity> groupEntityList = new ArrayList<>();

    private final List<GroupMemberEntity> groupMemberEntityList = new ArrayList<>();

    private BaseSyncEntity baseSyncEntity;

    private Long clusterId;

    private Long runtimeId;

    private MetadataType metadataType;

    private void recognize(BaseSyncEntity baseSyncEntity) {
        this.baseSyncEntity = baseSyncEntity;
        if (baseSyncEntity instanceof ClusterEntity) {
            this.clusterId = baseSyncEntity.getId();
            this.runtimeId = 0L;
            this.metadataType = MetadataType.CLUSTER;
        } else {
            this.clusterId = baseSyncEntity.getClusterId();
            this.runtimeId = baseSyncEntity.getId();
            this.metadataType = MetadataType.RUNTIME;
        }
    }

    public void setBaseSyncEntity(BaseSyncEntity baseSyncEntity) {
        this.baseSyncEntity = baseSyncEntity;
        this.recognize(baseSyncEntity);
    }

    public void buildBaseSyncEntity(BaseSyncEntity baseSyncEntity) {
        List<TopicEntity> topicEntityList = this.buildTopic();
        List<GroupEntity> groupEntityList = this.buildGroup();
        this.topicEntityList.addAll(topicEntityList);
        this.groupEntityList.addAll(groupEntityList);

        this.buildConfig(this.metadataType, baseSyncEntity, baseSyncEntity.getId());

        pairList.add(Pair.of(topicEntityList, groupEntityList));
    }

    public List<TopicEntity> buildTopic() {
        List<TopicEntity> topicEntityList = new ArrayList<>();
        int num = new Random().nextInt(10) + 10;
        for (int i = 0; i < num; i++) {
            TopicEntity topicEntity = new TopicEntity();
            topicEntity.setOrganizationId(baseSyncEntity.getOrganizationId());
            topicEntity.setClusterId(this.clusterId);
            topicEntity.setRuntimeId(0L);
            topicEntity.setTopicType("test-data");
            topicEntity.setTopicName("cluster-" + this.clusterId + ".topic-" + i);

            if (Objects.equals(baseSyncEntity.getClusterType(), ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE) ||
                Objects.equals(baseSyncEntity.getClusterType(), ClusterType.STORAGE_ROCKETMQ_BROKER_RAFT)) {
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


    public List<GroupEntity> buildGroup() {
        List<GroupEntity> groupEntityArrayList = new ArrayList<>();
        Random random = new Random();
        int num = random.nextInt(10) + 10;
        for (int i = 0; i < num; i++) {
            GroupEntity groupEntity = new GroupEntity();
            groupEntity.setOrganizationId(this.baseSyncEntity.getOrganizationId());
            groupEntity.setClusterId(this.clusterId);

            groupEntity.setRuntimeId(this.runtimeId);
            groupEntity.setName("cluster-" + this.baseSyncEntity.getId() + ".group-" + i);
            groupEntity.setType(random.nextInt(1));
            groupEntity.setOwnType("0");
            groupEntityList.add(groupEntity);
        }
        return groupEntityArrayList;
    }

    public void buildSubscription(List<TopicEntity> topicEntityList, List<GroupEntity> groupEntityList) {
        List<GroupMemberEntity> groupMemberEntityList = new ArrayList<>();
        topicEntityList.forEach(topicEntity -> {
            groupEntityList.forEach(groupEntity -> {
                GroupMemberEntity groupMemberEntity = new GroupMemberEntity();
                groupMemberEntity.setOrganizationId(groupEntity.getOrganizationId());
                groupMemberEntity.setClusterId(groupEntity.getClusterId());
                groupMemberEntity.setGroupName(groupEntity.getName());
                groupMemberEntity.setTopicName(topicEntity.getTopicName());
                groupMemberEntity.setEventMeshUser("11111");
                groupMemberEntity.setStatus(1L);
                groupMemberEntityList.add(groupMemberEntity);
            });
        });
        this.groupMemberEntityList.addAll(groupMemberEntityList);
    }

    public void buildSyncConfig() {
        this.buildConfig(this.metadataType, baseSyncEntity, baseSyncEntity.getId());
    }

    public void buildConfig(MetadataType metadataType, BaseSyncEntity clusterEntity, Long id) {
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
