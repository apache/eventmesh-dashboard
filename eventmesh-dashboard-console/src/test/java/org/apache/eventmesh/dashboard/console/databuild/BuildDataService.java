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
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.entity.function.ConfigEntity;
import org.apache.eventmesh.dashboard.console.entity.message.GroupEntity;
import org.apache.eventmesh.dashboard.console.entity.message.GroupMemberEntity;
import org.apache.eventmesh.dashboard.console.entity.message.TopicEntity;
import org.apache.eventmesh.dashboard.console.mapper.cluster.ClusterMapper;
import org.apache.eventmesh.dashboard.console.mapper.cluster.RuntimeMapper;
import org.apache.eventmesh.dashboard.console.mapper.function.ConfigMapper;
import org.apache.eventmesh.dashboard.console.mapper.message.GroupMapper;
import org.apache.eventmesh.dashboard.console.mapper.message.GroupMemberMapper;
import org.apache.eventmesh.dashboard.console.mapper.message.TopicMapper;

import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson2.JSON;

import lombok.Data;
import lombok.Getter;

@Component
public class BuildDataService {

    @Getter
    private final BuildCompleteData buildCompleteData = new BuildCompleteData();
    private final List<BuildMetadataOperation> buildMetadataOperationList = new ArrayList<>();
    private final Map<Long, BuildMetadataOperation> clusterBuildMetadataOperation = new HashMap<>();
    @Autowired
    private ClusterMapper clusterMapper;
    @Autowired
    private RuntimeMapper runtimeMapper;
    @Autowired
    private TopicMapper topicMapper;
    @Autowired
    private GroupMapper groupMapper;
    @Autowired
    private GroupMemberMapper groupMemberMapper;
    @Autowired
    private ConfigMapper configMapper;

    public void runtimeCopyByCluster(RuntimeEntity runtimeEntity) {
        runtimeEntity = this.runtimeMapper.queryRuntimeEntityById(runtimeEntity);
        CopyMetadata copyMetadata = new CopyMetadata();
        copyMetadata.setRuntimeEntity(runtimeEntity);
        copyMetadata.copyRuntimeConfigByCluster();
        copyMetadata.copyTopicByCluster();
    }


    public void setBaseSyncEntity(List<BaseSyncEntity> baseSyncEntity) {
        baseSyncEntity.forEach(this::setBaseSyncEntity);
    }

    public void buildFullData(Object baseSyncEntity) {
        this.buildFullData(baseSyncEntity, true);
    }

    @SuppressWarnings("unchecked")
    public void buildFullData(Object baseSyncEntity, boolean isRuntime) {
        List<BaseSyncEntity> baseSyncEntityList;
        if (baseSyncEntity instanceof List) {
            baseSyncEntityList = (List<BaseSyncEntity>) baseSyncEntity;
            baseSyncEntityList.forEach(this::setBaseSyncEntity);
        } else {
            this.setBaseSyncEntity((BaseSyncEntity) baseSyncEntity);
        }
        if (isRuntime) {
            this.buildSyncConfig();
        }
        this.buildTopicAndGroup();
    }

    public void setBaseSyncEntity(BaseSyncEntity baseSyncEntity) {
        BuildMetadataOperation buildMetadataOperation = new BuildMetadataOperation();
        buildMetadataOperation.setBaseSyncEntity(baseSyncEntity);
        this.buildMetadataOperationList.add(buildMetadataOperation);
    }

    public void buildTopicAndGroup() {
        this.buildTopic();
        this.buildGroup();
        this.buildGroupMember();
        this.buildMetadataConfig();

        //this.clusterBuildMetadataOperation.forEach(this::runtimeDataWriteCluster);
    }

    public void runtimeDataWriteCluster(Long cluster, BuildMetadataOperation buildMetadataOperation) {
        List<TopicEntity> topicEntityList = new ArrayList<>();
        buildMetadataOperation.operationData.topicEntityList.forEach(topicEntity -> {
            topicEntityList.add(topicEntity);
            topicEntity.setRuntimeId(0L);
        });
        this.topicMapper.batchInsert(topicEntityList);

    }

    public List<ConfigEntity> buildSyncConfig() {
        this.buildMetadataOperationList.forEach(BuildMetadataOperation::buildSyncConfig);
        return this.buildCompleteData.getConfigEntityList();
    }

    public void buildTopic() {
        this.buildMetadataOperationList.forEach(BuildMetadataOperation::buildTopic);
        this.topicMapper.batchInsert(this.buildCompleteData.getTopicEntityList());
    }

    public void buildGroup() {
        this.buildMetadataOperationList.forEach(BuildMetadataOperation::buildGroup);
        this.groupMapper.batchInsert(this.buildCompleteData.getGroupEntityList());
    }

    public void buildGroupMember() {
        this.buildMetadataOperationList.forEach(BuildMetadataOperation::buildSubscription);
        this.groupMapper.batchInsert(this.buildCompleteData.getGroupEntityList());
    }

    public void buildMetadataConfig() {
        this.buildCompleteData.getConfigEntityList().clear();
        this.buildMetadataOperationList.forEach(BuildMetadataOperation::buildGroupConfig);
        this.buildMetadataOperationList.forEach(BuildMetadataOperation::buildTopicConfig);
        this.configMapper.batchInsert(this.buildCompleteData.getConfigEntityList());
    }

    @Data
    public static class BuildCompleteData {

        private final List<ConfigEntity> configEntityList = new ArrayList<>();

        private final List<TopicEntity> topicEntityList = new ArrayList<>();

        private final List<GroupEntity> groupEntityList = new ArrayList<>();

        private final List<GroupMemberEntity> groupMemberEntityList = new ArrayList<>();

    }

    public class CopyMetadata {

        private Long clusterId;

        private Long instanceId;

        private RuntimeEntity runtimeEntity;

        public void setRuntimeEntity(RuntimeEntity runtimeEntity) {
            this.runtimeEntity = runtimeEntity;
            this.clusterId = runtimeEntity.getClusterId();
            this.instanceId = runtimeEntity.getId();
        }

        public void copyRuntimeConfigByCluster() {
            ConfigEntity configEntity = new ConfigEntity();
            configEntity.setInstanceType(MetadataType.CLUSTER);
            configEntity.setClusterId(clusterId);
            configEntity.setInstanceId(instanceId);
            List<ConfigEntity> configEntityList = configMapper.selectConfigsByInstance(configEntity);
            if (CollectionUtils.isEmpty(configEntityList)) {
                throw new RuntimeException(" config is empty");
            }
            List<ConfigEntity> newConfigEntityList = JSON.parseArray(JSON.toJSONString(configEntityList), ConfigEntity.class);
            newConfigEntityList.forEach(configEntity1 -> {
                configEntity1.setInstanceType(MetadataType.RUNTIME);
                configEntity1.setInstanceId(instanceId);
            });

        }

        public void copyTopicByCluster() {
            TopicEntity topicEntity = new TopicEntity();
            topicEntity.setClusterId(clusterId);
            topicEntity.setRuntimeId(0L);
            List<TopicEntity> topicList = topicMapper.queryTopicsToFrontByClusterId(topicEntity);
            if (CollectionUtils.isEmpty(topicList)) {
                throw new RuntimeException(" topic is empty");
            }
            topicList.forEach(topicEntity1 -> {
                topicEntity1.setRuntimeId(instanceId);
            });
            topicMapper.batchInsert(topicList);
            topicList.forEach(topicEntity1 -> {

            });
        }
    }

    public class BuildMetadataOperation {

        private final BuildCompleteData operationData = new BuildCompleteData();

        private BaseSyncEntity baseSyncEntity;

        private Long organizationId;

        private Long clusterId;

        private Long runtimeId;

        private MetadataType metadataType;

        private void recognize(BaseSyncEntity baseSyncEntity) {
            this.baseSyncEntity = baseSyncEntity;
            this.organizationId = baseSyncEntity.getOrganizationId();
            if (baseSyncEntity instanceof ClusterEntity) {
                this.clusterId = baseSyncEntity.getId();
                this.runtimeId = 0L;
                this.metadataType = MetadataType.CLUSTER;
            } else {
                this.clusterId = baseSyncEntity.getClusterId();
                this.runtimeId = baseSyncEntity.getId();
                this.metadataType = MetadataType.RUNTIME;
                clusterBuildMetadataOperation.put(this.clusterId, this);
            }
        }

        public void setBaseSyncEntity(BaseSyncEntity baseSyncEntity) {
            this.baseSyncEntity = baseSyncEntity;
            this.recognize(baseSyncEntity);
        }

        public void buildTopic() {
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

                operationData.topicEntityList.add(topicEntity);
                buildCompleteData.topicEntityList.add(topicEntity);
            }
        }


        public void buildGroup() {
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
                operationData.groupEntityList.add(groupEntity);
                buildCompleteData.groupEntityList.add(groupEntity);
            }
        }

        public void buildSubscription() {
            this.buildSubscription(operationData.topicEntityList, operationData.groupEntityList);
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
            buildCompleteData.groupMemberEntityList.addAll(groupMemberEntityList);
        }

        public void buildSyncConfig() {
            this.buildConfig(this.metadataType, baseSyncEntity, baseSyncEntity.getId());
        }

        public void buildTopicConfig() {
            operationData.topicEntityList.forEach(topicEntity -> {
                this.buildConfig(MetadataType.TOPIC, null, topicEntity.getId());
            });
        }

        public void buildGroupConfig() {
            operationData.groupEntityList.forEach(topicEntity -> {
                this.buildConfig(MetadataType.GROUP, null, topicEntity.getId());
            });
        }

        public void buildConfig(MetadataType metadataType, BaseSyncEntity clusterEntity, Long id) {
            for (int i = 0; i < 15; i++) {
                ConfigEntity configEntity = new ConfigEntity();
                configEntity.setOrganizationId(this.organizationId);
                configEntity.setClusterId(this.clusterId);
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

                buildCompleteData.configEntityList.add(configEntity);
            }
        }

    }

}
