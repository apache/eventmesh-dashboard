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

package org.apache.eventmesh.dashboard.console.service.cluster.impl;

import org.apache.eventmesh.dashboard.console.EventMeshDashboardApplication;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterService;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = EventMeshDashboardApplication.class)
public class ClusterEntityServiceImplTest {

    @Autowired
    private ClusterService clusterService;


    @Before
    public void nameExist() {
    }

    public void queryClusterById() {
    }

    public void queryClusterByOrganizationIdAndType() {
    }


    public void queryRelationClusterByClusterIdAndType() {
    }

    public void queryRelationshipClusterByClusterIdAndType() {
    }

    public void queryStorageByClusterId() {
    }

    public void queryAllSubClusterByClusterId() {
    }

    public void createCluster() {
    }

    public void getClusterBaseMessage() {
    }


    public void queryHomeClusterData() {
    }

    public void batchInsert() {
    }

    public void selectAll() {
    }

    public void selectNewlyIncreased() {
    }

    void insertCluster() {
    }

    void insertClusterAndRelationship() {
    }

    public void selectAllCluster() {
    }


    public void updateClusterById() {
    }

    public void deactivate() {
    }

    public void queryByUpdateTime() {
    }

    public void getIndex() {
    }


}
