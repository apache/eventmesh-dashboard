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


package org.apache.eventmesh.dashboard.console.controller.message;


import org.apache.eventmesh.dashboard.common.model.metadata.ClusterMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;
import org.apache.eventmesh.dashboard.console.controller.ClusterAbitityService;
import org.apache.eventmesh.dashboard.console.domain.metadata.ClusterMetadataDomain;
import org.apache.eventmesh.dashboard.console.domain.metadata.ClusterOperationHandler;
import org.apache.eventmesh.dashboard.console.entity.message.TopicEntity;
import org.apache.eventmesh.dashboard.console.mapstruct.message.TopicControllerMapper;
import org.apache.eventmesh.dashboard.console.modle.IdDTO;
import org.apache.eventmesh.dashboard.console.modle.dto.topic.CreateTopicDTO;
import org.apache.eventmesh.dashboard.console.modle.dto.topic.GetTopicListDTO;
import org.apache.eventmesh.dashboard.console.modle.vo.RuntimeIdDTO;
import org.apache.eventmesh.dashboard.console.modle.vo.topic.TopicDetailGroupVO;
import org.apache.eventmesh.dashboard.console.service.message.TopicService;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user/topic")
public class TopicController {

    @Autowired
    private TopicService topicService;


    @Autowired
    private ClusterAbitityService clusterAbitityService;

    @Autowired
    private ClusterMetadataDomain clusterMetadataDomain;

    /**
     * @param getTopicListDTO
     * @return
     */
    @PostMapping("/queryTopicListByClusterId")
    public List<TopicEntity> queryTopicListByClusterId(@Validated @RequestBody GetTopicListDTO getTopicListDTO) {
        // cap 的直接查询
        if (this.clusterAbitityService.isCAP(getTopicListDTO)) {
            topicService.getTopicListToFront(TopicControllerMapper.INSTANCE.queryTopicListByClusterId(getTopicListDTO));
        }
        // 非 CAP 另外查询
        return topicService.getTopicListToFront(TopicControllerMapper.INSTANCE.queryTopicListByClusterId(getTopicListDTO));
    }

    @PostMapping("queryTopicListById ")
    public TopicEntity queryTopicById(@Validated @RequestBody RuntimeIdDTO runtimeIdDTO) {
        TopicEntity topicEntity = topicService.selectTopicById(TopicControllerMapper.INSTANCE.queryTopicListById(runtimeIdDTO));
        if (this.clusterAbitityService.isCAP(topicEntity)) {
            return topicEntity;
        }
        //
        List<TopicEntity> queryList = new ArrayList<>();
        // 如果 是 eventmesh 集群。 得到 eventmesh 所有 runtime ， 所有存储
        clusterMetadataDomain.operation(topicEntity.getClusterId(), new ClusterOperationHandler() {

            @Override
            public void handler(RuntimeMetadata baseSyncBase) {
                TopicEntity topicEntity = new TopicEntity();
                queryList.add(topicEntity);
                topicEntity.setClusterId(baseSyncBase.getClusterId());
                topicEntity.setClusterType(baseSyncBase.getClusterType());
                topicEntity.setRuntimeId(baseSyncBase.getId());
                topicEntity.setTopicName(topicEntity.getTopicName());
            }

            @Override
            public void handler(ClusterMetadata clusterDO) {
                TopicEntity topicEntity = new TopicEntity();
                queryList.add(topicEntity);
                topicEntity.setClusterId(clusterDO.getClusterId());
                topicEntity.setClusterType(clusterDO.getClusterType());
                topicEntity.setRuntimeId(clusterDO.getClusterId());
                topicEntity.setTopicName(topicEntity.getTopicName());
            }
        });



        topicService.queryRuntimeByBaseSyncEntity(queryList);
        return null;
    }

    @GetMapping("deleteTopic")
    public Integer deleteTopic(@Validated @RequestBody IdDTO idDTO) {
        TopicEntity topicEntity = this.topicService.selectTopicById(TopicControllerMapper.INSTANCE.deleteTopic(idDTO));
        if (this.clusterAbitityService.isCAP(topicEntity)) {
            return this.topicService.deleteTopicById(topicEntity);
        }
        // 通过 clusterId，runtime id， topic name 删除

        List<TopicEntity> deleteList = new ArrayList<>();
        // 如果 是 eventmesh 集群。 得到 eventmesh 所有 runtime ， 所有存储
        clusterMetadataDomain.operation(topicEntity.getClusterId(), new ClusterOperationHandler() {

            @Override
            public void handler(RuntimeMetadata baseSyncBase) {
                TopicEntity topicEntity = new TopicEntity();
                deleteList.add(topicEntity);
                topicEntity.setClusterId(baseSyncBase.getClusterId());
                topicEntity.setClusterType(baseSyncBase.getClusterType());
                topicEntity.setRuntimeId(baseSyncBase.getId());
            }

            @Override
            public void handler(ClusterMetadata clusterDO) {
                TopicEntity topicEntity = new TopicEntity();
                deleteList.add(topicEntity);
                topicEntity.setClusterId(clusterDO.getClusterId());
                topicEntity.setClusterType(clusterDO.getClusterType());
            }
        });
        return topicService.deleteTopicByRuntimeIdAndTopicName(deleteList);
    }

    @PostMapping("createTopic")
    public void createTopic(@Validated @RequestBody CreateTopicDTO createTopicDTO) {
        List<TopicEntity> createTopicList = new ArrayList<>();
        // 如果 是 eventmesh 集群。 得到 eventmesh 所有 runtime ， 所有存储
        clusterMetadataDomain.operation(createTopicDTO.getClusterId(), new ClusterOperationHandler() {

            @Override
            public void handler(RuntimeMetadata baseSyncBase) {
                TopicEntity topicEntity = TopicControllerMapper.INSTANCE.createTopic(createTopicDTO);
                createTopicList.add(topicEntity);
                topicEntity.setClusterId(baseSyncBase.getClusterId());
                topicEntity.setClusterType(baseSyncBase.getClusterType());
                topicEntity.setRuntimeId(baseSyncBase.getId());

            }

            @Override
            public void handler(ClusterMetadata clusterDO) {
                TopicEntity topicEntity = TopicControllerMapper.INSTANCE.createTopic(createTopicDTO);
                createTopicList.add(topicEntity);
                topicEntity.setClusterId(clusterDO.getClusterId());
                topicEntity.setClusterType(clusterDO.getClusterType());
            }
        });
        this.topicService.batchInsert(createTopicList);
    }

    /**
     * TODO delete
     *
     * @param topicId
     * @return
     */
    @GetMapping("/cluster/topic/getTopicDetailGroups")
    public List<TopicDetailGroupVO> getTopicDetailGroups(Long topicId) {
        return topicService.getTopicDetailGroups(topicId);
    }

}
