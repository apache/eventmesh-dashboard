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

package org.apache.eventmesh.dashboard.console.controller.cluster;

import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType;
import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType.FirstToWhom;
import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.console.model.dto.cluster.cluster.BatchCreateClusterDataDTO;
import org.apache.eventmesh.dashboard.console.model.dto.cluster.cluster.SimpleCreateClusterDataDTO;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.alibaba.fastjson.JSON;

public class ClusterControllerMock {


    public void mock_createClusterByBachAddress() {
        ClusterType clusterType = ClusterType.EVENTMESH_CLUSTER;

        List<ClusterType> runtimeClusterType = clusterType.getRuntimeClusterType();

        List<ClusterType> metaClusterType = clusterType.getMetaClusterType();
    }

    @Test
    public void mock_createClusterByBachAddress_jvmType() {

        BatchCreateClusterDataDTO data = new BatchCreateClusterDataDTO();
        data.setClusterType(ClusterType.EVENTMESH_JVM_CLUSTER);
        data.setOrganizationId(1L);
        data.setName("mock_data"+ System.currentTimeMillis());
        data.setDescription("mock_describe"+ System.currentTimeMillis());
        data.setVersion("1.0.0");
        List<SimpleCreateClusterDataDTO> simpleCreateClusterDataDTOList = new ArrayList<>();
        data.setSimpleCreateClusterDataList(simpleCreateClusterDataDTOList);

        SimpleCreateClusterDataDTO simpleCreateClusterDataDTO = new SimpleCreateClusterDataDTO();
        simpleCreateClusterDataDTO.setName("eventmesh-runtime" + System.currentTimeMillis());
        simpleCreateClusterDataDTO.setClusterType(ClusterType.EVENTMESH_JVM_RUNTIME);
        simpleCreateClusterDataDTO.setFirstToWhom(FirstToWhom.RUNTIME);
        simpleCreateClusterDataDTO.setClusterTrusteeshipType(ClusterTrusteeshipType.TRUSTEESHIP_FIND_REVERSE);
        simpleCreateClusterDataDTO.setAddress("127.0.0.1,127.0.0.2,127.0.0.3/?tcp=9876&http=9999&grpc=9898");
        simpleCreateClusterDataDTO.setDescription("mock_describe"+ System.currentTimeMillis());
        simpleCreateClusterDataDTO.setVersion("1.0.0");
        simpleCreateClusterDataDTOList.add(simpleCreateClusterDataDTO);

        simpleCreateClusterDataDTO = new SimpleCreateClusterDataDTO();
        simpleCreateClusterDataDTO.setName("eventmesh-meta" + System.currentTimeMillis());
        simpleCreateClusterDataDTO.setClusterType(ClusterType.EVENTMESH_JVM_META);
        simpleCreateClusterDataDTO.setFirstToWhom(FirstToWhom.RUNTIME);
        simpleCreateClusterDataDTO.setClusterTrusteeshipType(ClusterTrusteeshipType.TRUSTEESHIP_FIND_REVERSE);
        simpleCreateClusterDataDTO.setAddress("127.0.0.1:9090");
        simpleCreateClusterDataDTO.setDescription("mock_describe"+ System.currentTimeMillis());
        simpleCreateClusterDataDTO.setVersion("1.0.0");
        simpleCreateClusterDataDTOList.add(simpleCreateClusterDataDTO);


        System.out.println(JSON.toJSONString(data));


    }

    @Test
    public void mock_createClusterByBachAddress_storage_jvm(){

        BatchCreateClusterDataDTO data = new BatchCreateClusterDataDTO();
        data.setClusterType(ClusterType.STORAGE_JVM_CLUSTER);
        data.setOrganizationId(1L);
        data.setName("mock_rocketmq"+ System.currentTimeMillis());
        data.setDescription("mock_describe"+ System.currentTimeMillis());
        data.setVersion("1.0.0");
        data.setMainClusterId(82L);
        List<SimpleCreateClusterDataDTO> simpleCreateClusterDataDTOList = new ArrayList<>();
        data.setSimpleCreateClusterDataList(simpleCreateClusterDataDTOList);

        SimpleCreateClusterDataDTO simpleCreateClusterDataDTO = new SimpleCreateClusterDataDTO();
        simpleCreateClusterDataDTO.setName("storage-meta" + System.currentTimeMillis());
        simpleCreateClusterDataDTO.setClusterType(ClusterType.STORAGE_JVM_META);
        simpleCreateClusterDataDTO.setFirstToWhom(FirstToWhom.RUNTIME);
        simpleCreateClusterDataDTO.setClusterTrusteeshipType(ClusterTrusteeshipType.TRUSTEESHIP_FIND_REVERSE);
        simpleCreateClusterDataDTO.setAddress("127.0.0.11,127.0.0.12,127.0.0.13:9999");
        simpleCreateClusterDataDTO.setDescription("mock_describe"+ System.currentTimeMillis());
        simpleCreateClusterDataDTO.setVersion("1.0.0");
        simpleCreateClusterDataDTOList.add(simpleCreateClusterDataDTO);


        System.out.println(JSON.toJSONString(data));
    }
}
