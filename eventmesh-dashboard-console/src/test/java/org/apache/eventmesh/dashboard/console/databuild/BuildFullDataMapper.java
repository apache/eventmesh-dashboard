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

import org.apache.eventmesh.dashboard.common.enums.ClusterFramework;
import org.apache.eventmesh.dashboard.common.enums.ClusterSyncMetadataEnum;
import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.enums.MetadataType;
import org.apache.eventmesh.dashboard.console.EventMeshDashboardApplication;
import org.apache.eventmesh.dashboard.console.entity.base.BaseRuntimeIdEntity;
import org.apache.eventmesh.dashboard.console.entity.base.BaseSyncEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.entity.function.ConfigEntity;
import org.apache.eventmesh.dashboard.console.entity.message.GroupEntity;
import org.apache.eventmesh.dashboard.console.entity.message.TopicEntity;
import org.apache.eventmesh.dashboard.console.mapper.cluster.ClusterMapper;
import org.apache.eventmesh.dashboard.console.mapper.cluster.RuntimeMapper;
import org.apache.eventmesh.dashboard.console.mapper.function.ConfigMapper;
import org.apache.eventmesh.dashboard.console.mapper.message.GroupMapper;
import org.apache.eventmesh.dashboard.console.mapper.message.GroupMemberMapper;
import org.apache.eventmesh.dashboard.console.mapper.message.TopicMapper;
import org.apache.eventmesh.dashboard.console.modle.DO.runtime.QueryRuntimeByBigExpandClusterDO;

import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.alibaba.fastjson2.JSON;


/**
 *  这个类的功能，要慢慢搬到 BuildDataController 里面 </p>
 *  main 目录下需要一个 基于 db 的操作
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {EventMeshDashboardApplication.class})
//@Sql(scripts = {"classpath:eventmesh-dashboard.sql"})
@AutoConfigureTestDatabase(replace = Replace.NONE)
public class BuildFullDataMapper {

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

    @Autowired
    private BuildDataService buildDataService;

    private final BuildMessageData buildMessageData = new BuildMessageData();

    private final Map<Long, BaseSyncEntity> clusterEntityMap = new HashMap<>();

    @Test
    public void buildClusterData() {
        ClusterEntity clusterEntity = new ClusterEntity();
        clusterEntity.setId(1L);
        clusterEntity = this.clusterMapper.queryByClusterId(clusterEntity);
        this.buildFullDataMapper(List.of(clusterEntity));
    }

    @Test
    public void buildEventMeshInStorageCluster() {
        ClusterEntity clusterEntity = new ClusterEntity();
        clusterEntity.setId(5L);
        this.doBuildEventMeshInStorageCluster(clusterEntity, true, false);
    }

    /**
     * 把 集群 初始化数据 同步到 新的 runtime 节点中
     *  不急，重构。只需要同步 Topic， runtime config。所有没必要重构？
     */
    @Test
    @Deprecated
    public void buildRuntimeData(){
        RuntimeEntity runtimeEntity = new RuntimeEntity();
        runtimeEntity.setId(0L);
        runtimeEntity = this.runtimeMapper.queryRuntimeEntityById(runtimeEntity);

        Long clusterId = runtimeEntity.getClusterId();
        final Long instanceId = runtimeEntity.getId();

        ConfigEntity configEntity = new ConfigEntity();
        configEntity.setInstanceType(MetadataType.CLUSTER);
        configEntity.setClusterId(clusterId);
        configEntity.setInstanceId(instanceId);
        List<ConfigEntity> configEntityList = this.configMapper.selectConfigsByInstance(configEntity);
        if(CollectionUtils.isEmpty(configEntityList)){
            throw  new RuntimeException(" config is empty");
        }
        List<ConfigEntity>  newConfigEntityList = JSON.parseArray(JSON.toJSONString(configEntityList), ConfigEntity.class);
        RuntimeEntity finalRuntimeEntity = runtimeEntity;
        newConfigEntityList.forEach(configEntity1 -> {
            configEntity1.setInstanceType(MetadataType.RUNTIME);
            configEntity1.setInstanceId(instanceId);
        });
        this.configMapper.batchInsert(newConfigEntityList);

        ClusterFramework clusterFramework = ClusterSyncMetadataEnum.getClusterFramework(runtimeEntity.getClusterType());
        // eventmesh runtime ， meta 的 runtime 是否要同步 cluster 的信息
        if(clusterFramework.isCAP()) {
            return;
        }
        TopicEntity topicEntity = new TopicEntity();
        topicEntity.setClusterId(clusterId);
        topicEntity.setRuntimeId(0L);
        List<TopicEntity> topicList = this.topicMapper.queryTopicsToFrontByClusterId(topicEntity);
        if(CollectionUtils.isEmpty(topicList)){
            throw  new RuntimeException(" topic is empty");
        }
        topicList.forEach(topicEntity1 -> {
           topicEntity1.setRuntimeId(instanceId);
        });
        this.topicMapper.batchInsert(topicList);

        GroupEntity groupEntity = new GroupEntity();
        groupEntity.setClusterId(clusterId);
        groupEntity.setRuntimeId(0L);
        List<GroupEntity> groupEntityList = this.groupMapper.queryClusterOrRuntimeGroupByClusterId(topicEntity);
        if(CollectionUtils.isEmpty(groupEntityList)){
            throw  new RuntimeException(" group is empty");
        }
        groupEntityList.forEach(groupEntity1 -> {
           groupEntity1.setRuntimeId(instanceId);
        });
        this.groupMapper.batchInsert(groupEntityList);
    }

    /**
     * 创建 metadata 类型 直接 meta cluster 的管理
     */
    public void doBuildEventMeshInStorageCluster(ClusterEntity clusterEntity, boolean isEventMesh, boolean onlyEventMesh) {
        if (Objects.nonNull(clusterEntity.getClusterType())) {
            if (clusterEntity.getClusterType().isStorage()) {
                if (clusterEntity.getClusterType().isDefinition()) {
                    List<ClusterEntity> newClusterEntityList = this.clusterMapper.queryRelationClusterByClusterIdAndType(clusterEntity);
                    // 得到 里面的 runtime
                    List<RuntimeEntity> runtimeList = this.runtimeMapper.queryRuntimeByClusterId(newClusterEntityList);
                    buildDataService.buildFullData(runtimeList);
                } else {
                    buildDataService.buildFullData(clusterEntity);
                }
                return;
            }else if(clusterEntity.getClusterType().isEventMethRuntime()){
                List<RuntimeEntity> runtimeList = this.runtimeMapper.queryRuntimeByClusterId(List.of(clusterEntity));
                buildDataService.buildFullData(runtimeList);
                return;
            }
        }
        List<ClusterEntity> clusterList = this.clusterMapper.queryRelationClusterByClusterIdAndType(clusterEntity);
        if(CollectionUtils.isEmpty(clusterList)){
            throw new RuntimeException(" clusterList is empty");
        }
        List<ClusterEntity> notMainClusterList = new ArrayList<>();
        List<ClusterEntity> mainClusterList = new ArrayList<>();
        List<ClusterEntity> eventMeshRuntime = new ArrayList<>();
        clusterList.forEach(clusterEntity1 -> {
            if (clusterEntity1.getClusterType().isStorageRuntime() && clusterEntity1.getClusterType().isDefinition()) {
                mainClusterList.add(clusterEntity1);
            } else if (clusterEntity1.getClusterType().isStorageCluster()) {
                notMainClusterList.add(clusterEntity1);
            } else if (clusterEntity1.getClusterType().isEventMethRuntime()) {
                eventMeshRuntime.add(clusterEntity1);
            }
        });
        if (isEventMesh) {
            buildDataService.buildFullData(eventMeshRuntime);
            if (onlyEventMesh) {
                return;
            }
        }
        // 构建 cap 存储 集群
        buildDataService.buildFullData(notMainClusterList);
        // 不存在 主从集群，就结束
        if (mainClusterList.isEmpty()) {
            return;
        }
        // 得到 主从集群里面的集群
        List<ClusterEntity> newClusterEntityList = this.clusterMapper.queryRelationshipClusterByClusterIdAndType(mainClusterList);
        // 得到 里面的 runtime
        List<RuntimeEntity> runtimeList = this.runtimeMapper.queryRuntimeByClusterId(newClusterEntityList);
        buildDataService.buildFullData(runtimeList);


    }

    @Test
    public void buildClusterInAllRuntime() throws Exception {
        RuntimeEntity runtimeEntity = new RuntimeEntity();
        runtimeEntity.setClusterId(1L);
        List<RuntimeEntity> runtimeEntityList = this.runtimeMapper.getRuntimesToFrontByCluster(runtimeEntity);
        buildDataService.buildFullData(runtimeEntityList);
    }

    /**
     *
     */
    @Test
    public void buildMainSlaveClusterInAllRuntime() {
        RuntimeEntity runtimeEntity = new RuntimeEntity();
        runtimeEntity.setClusterId(1L);
        List<RuntimeEntity> runtimeEntityList = this.runtimeMapper.getRuntimesToFrontByCluster(runtimeEntity);
        buildDataService.buildFullData(runtimeEntityList,false);
    }

    /**
     *
     */
    @Test
    public void buildRuntimeFullDataMapper() {
        boolean all = false;
        List<RuntimeEntity> runtimeEntityList;
        if (all) {
            runtimeEntityList = this.runtimeMapper.queryAll();
        } else {
            Long clusterId = 1L;
            List<ClusterType> clusterTypeList = ClusterType.getStorageCluster();
            QueryRuntimeByBigExpandClusterDO queryRuntimeByBigExpandClusterDO =
                QueryRuntimeByBigExpandClusterDO.builder().followClusterId(clusterId).queryClusterTypeList(clusterTypeList).build();
            runtimeEntityList = this.runtimeMapper.queryClusterRuntimeOnClusterSpecifyByClusterId(queryRuntimeByBigExpandClusterDO);
        }
        runtimeEntityList.size();
        //this.buildFullDataMapper((List<BaseSyncEntity>) t(runtimeEntityList));
    }

    public void buildFullDataMapper(List<ClusterEntity> clusterEntityList,List<RuntimeEntity> runtimeEntityList){

    }

    /**
     *  TODO 构建问题， metadata 数据 与 cluster 和 runtime 数据之间的关系。</p>
     *       不管什么模式， cluster 都有一份数据，runtime 从 cluster 备份一份数据。
     *       需要备份的数据： config
     *       不备份的数据： topic ， group ， 订阅
     *       又得重构
     */
    private void buildFullDataMapper(List<BaseSyncEntity> baseSyncEntities) {
        this.buildFullDataMapper(baseSyncEntities,true);
    }

    private void buildFullDataMapper(List<BaseSyncEntity> baseSyncEntities , boolean onlyMetadata){
        if (CollectionUtils.isEmpty(baseSyncEntities)) {
            return;
        }
        baseSyncEntities.forEach(this::setBaseSyncEntity);
        if(!onlyMetadata) {
            this.buildMessageData.buildSyncConfig();
            this.configMapper.batchInsert(this.buildMessageData.getConfigEntityList());
            this.buildMessageData.getConfigEntityList().clear();
            this.buildConfig();
        }
        this.topicMapper.batchInsert(this.buildMessageData.getTopicEntityList());
        this.groupMapper.batchInsert(this.buildMessageData.getGroupEntityList());

        this.buildMessageData.buildSubscription(this.buildMessageData.getTopicEntityList(), this.buildMessageData.getGroupEntityList());
        this.groupMemberMapper.batchInsert(this.buildMessageData.getGroupMemberEntityList());

    }

    public void setBaseSyncEntity(BaseSyncEntity baseSyncEntity) {
        buildMessageData.setBaseSyncEntity(baseSyncEntity);
        clusterEntityMap.put(baseSyncEntity.getId(), baseSyncEntity);


    }

    public void buildFullDataMapper(BaseSyncEntity baseSyncEntity) {
        buildMessageData.setBaseSyncEntity(baseSyncEntity);
        clusterEntityMap.put(baseSyncEntity.getId(), baseSyncEntity);
        this.buildMessageData.buildSyncConfig();
        this.configMapper.batchInsert(this.buildMessageData.getConfigEntityList());
        this.buildMessageData.getConfigEntityList().clear();

        this.topicMapper.batchInsert(this.buildMessageData.getTopicEntityList());
        this.groupMapper.batchInsert(this.buildMessageData.getGroupEntityList());

        this.buildMessageData.getTopicEntityList().forEach(topicEntity -> {
            buildMessageData.buildConfig(MetadataType.TOPIC, clusterEntityMap.get(topicEntity.getClusterId()), topicEntity.getId());
        });
        this.configMapper.batchInsert(this.buildMessageData.getConfigEntityList());
        this.buildMessageData.getConfigEntityList().clear();

        this.buildMessageData.getGroupEntityList().forEach(groupEntity -> {
            buildMessageData.buildConfig(MetadataType.GROUP, clusterEntityMap.get(groupEntity.getClusterId()), groupEntity.getId());
        });
        this.configMapper.batchInsert(this.buildMessageData.getConfigEntityList());
        this.buildMessageData.getConfigEntityList().clear();

        this.buildMessageData.buildSubscription(this.buildMessageData.getTopicEntityList(), this.buildMessageData.getGroupEntityList());

        this.groupMemberMapper.batchInsert(this.buildMessageData.getGroupMemberEntityList());
    }

    public void buildConfig() {
        this.buildConfig(null, null);
    }

    public void buildConfig(MetadataType metadataType, List<BaseRuntimeIdEntity> baseRuntimeIdEntitieList) {
        if (Objects.nonNull(baseRuntimeIdEntitieList)) {
            baseRuntimeIdEntitieList.forEach(runtimeIdEntity -> {
                buildMessageData.buildConfig(metadataType, clusterEntityMap.get(runtimeIdEntity.getClusterId()), runtimeIdEntity.getId());
            });
        }
        this.configMapper.batchInsert(this.buildMessageData.getConfigEntityList());
        this.buildMessageData.getConfigEntityList().clear();
    }


}
