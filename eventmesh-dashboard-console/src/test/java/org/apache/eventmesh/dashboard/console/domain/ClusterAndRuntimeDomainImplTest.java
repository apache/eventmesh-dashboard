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

package org.apache.eventmesh.dashboard.console.domain;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.console.EventMeshDashboardApplication;
import org.apache.eventmesh.dashboard.console.domain.Impl.ClusterAndRuntimeDomainImpl;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.model.DO.domain.clusterAndRuntimeDomain.ClusterAndRuntimeOfRelationshipDO;
import org.apache.eventmesh.dashboard.console.model.DO.domain.clusterAndRuntimeDomain.GetClusterInSyncReturnDO;
import org.apache.eventmesh.dashboard.console.model.DO.domain.clusterAndRuntimeDomain.QueryClusterTreeDO;
import org.apache.eventmesh.dashboard.console.model.vo.cluster.ClusterTreeVO;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {EventMeshDashboardApplication.class})
public class ClusterAndRuntimeDomainImplTest {


    @Autowired
    private ClusterAndRuntimeDomainImpl clusterAndRuntimeDomain;




    @Test
    public void test_getAllClusterAndRuntimeByCluster_deploy(){
        ClusterEntity clusterEntity = new ClusterEntity();
        clusterEntity.setId(82L);
        ClusterAndRuntimeOfRelationshipDO data =
            this.clusterAndRuntimeDomain.getAllClusterAndRuntimeByCluster(clusterEntity, null);
        System.out.println(data);

        clusterEntity.setId(32L);
        data = this.clusterAndRuntimeDomain.getAllClusterAndRuntimeByCluster(clusterEntity, null);
        System.out.println(data);
    }


    @Test
    public void test_sync(){
        ClusterEntity clusterEntity = new ClusterEntity();
        clusterEntity.setId(82L);
        List<ClusterType> clusterTypeList = ClusterType.getStorageRuntimeCluster();
        clusterTypeList = List.of(ClusterType.STORAGE_JVM_BROKER);
        GetClusterInSyncReturnDO data =
            this.clusterAndRuntimeDomain.queryClusterInSync(clusterEntity, clusterTypeList);

        System.out.println(data);
    }

    @Test
    public void test_queryClusterTree(){
        QueryClusterTreeDO data = new QueryClusterTreeDO();
        data.setClusterId(82L);
        List<ClusterTreeVO> result = this.clusterAndRuntimeDomain.queryClusterTree(data);
        System.out.println(result);
    }

}
