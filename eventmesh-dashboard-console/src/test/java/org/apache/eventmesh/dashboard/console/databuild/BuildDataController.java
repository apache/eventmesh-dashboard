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

import org.apache.eventmesh.dashboard.console.EventMeshDashboardApplication;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;

import org.junit.Before;
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
public class BuildDataController {


    @Autowired
    private BuildDataService buildDataService;


    @Before
    public void init() {
        System.out.println(buildDataService);
    }

    @Test
    public void mock_copyRuntimeByCluster() {
        RuntimeEntity runtimeEntity = new RuntimeEntity();
        runtimeEntity.setId(0L);
        this.buildDataService.runtimeCopyByCluster(runtimeEntity);
    }


    @Test
    public void mock_databases_sync_cap() {

    }

    @Test
    public void mock_databases_sync_mainSlave() {

    }


    @Test
    public void mock_databases_increment_independence() {

    }

    @Test
    public void mock_cluster_sync_cap() {

    }

    @Test
    public void mock_cluster_sync_mainSlave() {

    }


    @Test
    public void mock_cluster_increment_independence() {

    }
}
