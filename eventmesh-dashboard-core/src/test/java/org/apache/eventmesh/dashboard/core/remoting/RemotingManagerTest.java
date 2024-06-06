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

package org.apache.eventmesh.dashboard.core.remoting;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.model.metadata.ClusterMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.ClusterRelationshipMetadata;
import org.apache.eventmesh.dashboard.common.model.remoting.topic.CreateTopicRequest;
import org.apache.eventmesh.dashboard.service.remoting.RemotingIntegrationService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

public class RemotingManagerTest {

    private RemotingManager remotingManager = new RemotingManager();


    @Test
    public void init_test() {
        RemotingIntegrationService proxyObject = (RemotingIntegrationService) remotingManager.getProxyObject();

        CreateTopicRequest createTopicRequest = new CreateTopicRequest();
        createTopicRequest.setClusterId(1L);

        proxyObject.createTopic(createTopicRequest);

    }

    @Test
    public void mock_overall_logic() {
        List<ClusterRelationshipMetadata> clusterRelationshipMetadataList = new ArrayList<>();
        AtomicLong clusterId = new AtomicLong(1);
        List<ClusterMetadata> clusterMetadataList = new ArrayList<>();

        //  两个 eventmesh 集群
        ClusterMetadata eventMeshCluster1 = new ClusterMetadata();
        eventMeshCluster1.setClusterType(ClusterType.EVENTMESH_CLUSTER);
        eventMeshCluster1.setId(clusterId.incrementAndGet());
        clusterMetadataList.add(eventMeshCluster1);

        ClusterMetadata eventMeshCluster2 = new ClusterMetadata();
        eventMeshCluster2.setClusterType(ClusterType.EVENTMESH_CLUSTER);
        eventMeshCluster2.setId(clusterId.incrementAndGet());
        clusterMetadataList.add(eventMeshCluster2);

        // 两个注册中心
        ClusterMetadata eventMeshMetaNacos1 = new ClusterMetadata();
        eventMeshMetaNacos1.setClusterType(ClusterType.EVENTMESH_META_NACOS);
        eventMeshMetaNacos1.setId(clusterId.incrementAndGet());
        clusterMetadataList.add(eventMeshMetaNacos1);
        this.relationship(clusterRelationshipMetadataList, eventMeshCluster1, eventMeshMetaNacos1);
        this.relationship(clusterRelationshipMetadataList, eventMeshCluster2, eventMeshMetaNacos1);

        ClusterMetadata eventMeshMetaNacos2 = new ClusterMetadata();
        eventMeshMetaNacos2.setClusterType(ClusterType.EVENTMESH_META_NACOS);
        eventMeshMetaNacos2.setId(clusterId.incrementAndGet());
        clusterMetadataList.add(eventMeshMetaNacos2);
        this.relationship(clusterRelationshipMetadataList, eventMeshCluster1, eventMeshMetaNacos2);
        this.relationship(clusterRelationshipMetadataList, eventMeshCluster2, eventMeshMetaNacos2);

        //  2个 eventmesh runtime
        ClusterMetadata eventMeshRuntime1 = new ClusterMetadata();
        eventMeshRuntime1.setClusterType(ClusterType.EVENTMESH_RUNTIME);
        eventMeshRuntime1.setId(clusterId.incrementAndGet());
        clusterMetadataList.add(eventMeshRuntime1);
        this.relationship(clusterRelationshipMetadataList, eventMeshCluster1, eventMeshRuntime1);

        ClusterMetadata eventMeshRuntime2 = new ClusterMetadata();
        eventMeshRuntime2.setClusterType(ClusterType.EVENTMESH_RUNTIME);
        eventMeshRuntime2.setId(clusterId.incrementAndGet());
        clusterMetadataList.add(eventMeshRuntime2);
        this.relationship(clusterRelationshipMetadataList, eventMeshCluster2, eventMeshRuntime2);

        // 两个个 rocketmq 集群
        ClusterMetadata rocketMCluster = new ClusterMetadata();
        rocketMCluster.setClusterType(ClusterType.STORAGE_ROCKETMQ_CLUSTER);
        rocketMCluster.setId(clusterId.incrementAndGet());
        clusterMetadataList.add(rocketMCluster);
        this.relationship(clusterRelationshipMetadataList, eventMeshCluster1, rocketMCluster);

        ClusterMetadata rocketMCluster1 = new ClusterMetadata();
        rocketMCluster1.setClusterType(ClusterType.STORAGE_ROCKETMQ_CLUSTER);
        rocketMCluster1.setId(clusterId.incrementAndGet());
        clusterMetadataList.add(rocketMCluster1);
        this.relationship(clusterRelationshipMetadataList, eventMeshCluster2, rocketMCluster1);

        // 2个 rocketmq broker 集群
        ClusterMetadata rocketBroker = new ClusterMetadata();
        rocketBroker.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER);
        rocketBroker.setId(clusterId.incrementAndGet());
        clusterMetadataList.add(rocketBroker);
        this.relationship(clusterRelationshipMetadataList, rocketMCluster, rocketBroker);

        ClusterMetadata rocketBroker1 = new ClusterMetadata();
        rocketBroker1.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER);
        rocketBroker1.setId(clusterId.incrementAndGet());
        clusterMetadataList.add(rocketBroker1);
        this.relationship(clusterRelationshipMetadataList, rocketMCluster1, rocketBroker1);

        // 两个 nameservier
        ClusterMetadata rocketMQNameserver = new ClusterMetadata();
        rocketMQNameserver.setClusterType(ClusterType.STORAGE_ROCKETMQ_NAMESERVER);
        rocketMQNameserver.setId(clusterId.incrementAndGet());
        clusterMetadataList.add(rocketMQNameserver);
        this.relationship(clusterRelationshipMetadataList, rocketBroker, rocketMQNameserver);
        this.relationship(clusterRelationshipMetadataList, rocketBroker1, rocketMQNameserver);

        ClusterMetadata rocketMQNameserver1 = new ClusterMetadata();
        rocketMQNameserver1.setClusterType(ClusterType.STORAGE_ROCKETMQ_NAMESERVER);
        rocketMQNameserver1.setId(clusterId.incrementAndGet());
        clusterMetadataList.add(rocketMQNameserver1);
        this.relationship(clusterRelationshipMetadataList, rocketBroker, rocketMQNameserver1);
        this.relationship(clusterRelationshipMetadataList, rocketBroker1, rocketMQNameserver1);

        try {
            remotingManager.cacheCluster(clusterMetadataList);
            remotingManager.cacheClusterRelationship(clusterRelationshipMetadataList);
            remotingManager.loadingCompleted();

            RemotingIntegrationService proxyObject = (RemotingIntegrationService) remotingManager.getProxyObject();
            CreateTopicRequest createTopicRequest = new CreateTopicRequest();
            createTopicRequest.setClusterId(9L);
            proxyObject.createTopic(createTopicRequest);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }


    }


    private void relationship(List<ClusterRelationshipMetadata> clusterRelationshipMetadataList, ClusterMetadata clusterMetadata,
        ClusterMetadata relationship) {
        clusterMetadata.setStatus(0);
        relationship.setStatus(0);

        ClusterRelationshipMetadata clusterRelationshipMetadata = new ClusterRelationshipMetadata();
        clusterRelationshipMetadata.setClusterType(clusterMetadata.getClusterType());
        clusterRelationshipMetadata.setClusterId(clusterMetadata.getId());
        clusterRelationshipMetadata.setRelationshipId(relationship.getId());
        clusterRelationshipMetadata.setRelationshipType(relationship.getClusterType());
        clusterRelationshipMetadataList.add(clusterRelationshipMetadata);
    }


}
