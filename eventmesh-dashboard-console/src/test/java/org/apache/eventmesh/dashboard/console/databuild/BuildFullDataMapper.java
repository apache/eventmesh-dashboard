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

import org.apache.eventmesh.dashboard.common.enums.MetadataType;
import org.apache.eventmesh.dashboard.console.EventMeshDashboardApplication;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.mapper.cluster.ClusterMapper;
import org.apache.eventmesh.dashboard.console.mapper.function.ConfigMapper;
import org.apache.eventmesh.dashboard.console.mapper.message.GroupMapper;
import org.apache.eventmesh.dashboard.console.mapper.message.GroupMemberMapper;
import org.apache.eventmesh.dashboard.console.mapper.message.TopicMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {EventMeshDashboardApplication.class})
//@Sql(scripts = {"classpath:eventmesh-dashboard.sql"})
@AutoConfigureTestDatabase(replace = Replace.NONE)
public class BuildFullDataMapper {

    @Autowired
    private ClusterMapper clusterMapper;

    @Autowired
    private TopicMapper topicMapper;

    @Autowired
    private GroupMapper groupMapper;

    @Autowired
    private GroupMemberMapper groupMemberMapper;

    @Autowired
    private ConfigMapper configMapper;

    private final BuildMessageData buildMessageData = new BuildMessageData();

    @Test
    public void buildFullDataMapper() {
        Map<Long, ClusterEntity> clusterEntityMap = new HashMap<Long, ClusterEntity>();
        List<ClusterEntity> clusterList = this.clusterMapper.queryAllCluster();
        clusterList.forEach(cluster -> {
            buildMessageData.buildCluster(cluster);
            clusterEntityMap.put(cluster.getId(), cluster);
        });
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

        this.buildMessageData.buildSubscription(null, this.buildMessageData.getTopicEntityList(), this.buildMessageData.getGroupEntityList());

        this.groupMemberMapper.batchInsert(this.buildMessageData.getSubscriptionEntityList());
    }
}
