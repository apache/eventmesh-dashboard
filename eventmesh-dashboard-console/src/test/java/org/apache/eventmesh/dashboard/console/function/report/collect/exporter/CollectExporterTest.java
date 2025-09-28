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

package org.apache.eventmesh.dashboard.console.function.report.collect.exporter;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.model.metadata.ClusterMetadata;
import org.apache.eventmesh.dashboard.console.function.report.ReportEngine;
import org.apache.eventmesh.dashboard.console.function.report.ReportHandlerManage;
import org.apache.eventmesh.dashboard.console.function.report.iotdb.IotDBReportEngine;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.junit.Before;
import org.junit.Test;

public class CollectExporterTest {

    private CollectExporter collectExporter = new CollectExporter();

    private ReportHandlerManage reportHandlerManage = new ReportHandlerManage();


    @Before
    public void init(){

        collectExporter.setClusterType(ClusterType.STORAGE_ROCKETMQ_CLUSTER);

        ClusterMetadata clusterMetadata = new ClusterMetadata();
        clusterMetadata.setOrganizationId(1L);
        clusterMetadata.setClusterId(2L);
        clusterMetadata.setClusterName("test");
        collectExporter.setClusterMetadata(clusterMetadata);

        ReportEngine reportEngine = new IotDBReportEngine();
        reportHandlerManage.setReportEngine(reportEngine);
        reportHandlerManage.init();
        collectExporter.setClassMap(reportHandlerManage.getReportMetaDataMap());

        Map<String, String> fieldMapper = new HashMap<>();
        fieldMapper.put("groupName", "group");
        fieldMapper.put("topicName", "topic");
        fieldMapper.put("clusterName", "cluster");
        fieldMapper.put("runtimeName", "runtime");
        fieldMapper.put("queueId","queueid");
        collectExporter.setFieldMapper(fieldMapper);
        collectExporter.init();
    }

    @Test
    public void test_buildObject() throws ExecutionException, InterruptedException {
        String data = """
            rocketmq_group_get_latency{cluster="MQCluster",broker="broker-b",topic="DEV_TID_topic_tfq",group="DEV_CID_consumer_cfq",queueid="0",} 0.05""";
        this.collectExporter.handler(data);
    }

}
