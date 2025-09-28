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

package org.apache.eventmesh.dashboard.console.function.report.iotdb;

import org.apache.eventmesh.dashboard.console.function.report.ReportConfig;
import org.apache.eventmesh.dashboard.console.function.report.ReportHandlerManage;
import org.apache.eventmesh.dashboard.console.function.report.ReportViewType;
import org.apache.eventmesh.dashboard.console.function.report.model.SingleGeneralReportDO;
import org.apache.eventmesh.dashboard.console.function.report.model.rocketmq.RocketmqConsumerGroupCreateExecutionTime;
import org.apache.eventmesh.dashboard.console.function.report.model.rocketmq.RocketmqConsumerQueueingLatency;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.Before;
import org.junit.Test;

public class IotDBReportEngineTest {

    private IotDBReportEngine iotDBReportEngine = new IotDBReportEngine();

    private ReportHandlerManage reportHandlerManage = new ReportHandlerManage();

    private ReportConfig reportConfig = new ReportConfig();

    @Before
    public void init() {
        reportHandlerManage.setReportEngine(iotDBReportEngine);
        reportHandlerManage.init();

        reportConfig.setEngineAddress("127.0.0.1:6667");
        iotDBReportEngine.setReportConfig(reportConfig);
        iotDBReportEngine.doInit();
    }


    @Test
    public void test_batchInsert() {

        List<RocketmqConsumerQueueingLatency> list = new ArrayList<>();
        RocketmqConsumerQueueingLatency rocketmqConsumerQueueingLatency = new RocketmqConsumerQueueingLatency();
        rocketmqConsumerQueueingLatency.setOrganizationId(1L);
        rocketmqConsumerQueueingLatency.setOrganizationName("test");
        rocketmqConsumerQueueingLatency.setClustersId(1L);
        rocketmqConsumerQueueingLatency.setClustersName("test-cluster");
        rocketmqConsumerQueueingLatency.setRuntimeId(3L);
        rocketmqConsumerQueueingLatency.setRuntimeName("test-runtime");
        rocketmqConsumerQueueingLatency.setRuntimeType("type-type");
        rocketmqConsumerQueueingLatency.setTopicId(4L);
        rocketmqConsumerQueueingLatency.setTopicName("test-topic");
        rocketmqConsumerQueueingLatency.setGroupId(5L);
        rocketmqConsumerQueueingLatency.setGroupName("test-group");
        rocketmqConsumerQueueingLatency.setValue(6666L);
        rocketmqConsumerQueueingLatency.setTime(LocalDateTime.now());

        list.add(rocketmqConsumerQueueingLatency);
        //iotDBReportEngine.batchInsert("rocketmq_consumer_queueing_latency", (List<Object>) ((Object) list));

        RocketmqConsumerGroupCreateExecutionTime rocketmqConsumerGroupCreateExecutionTime = new RocketmqConsumerGroupCreateExecutionTime();
        rocketmqConsumerGroupCreateExecutionTime.setOrganizationId(1L);
        rocketmqConsumerGroupCreateExecutionTime.setOrganizationName("test");
        rocketmqConsumerGroupCreateExecutionTime.setClustersId(1L);
        rocketmqConsumerGroupCreateExecutionTime.setClustersName("test-cluster");
        rocketmqConsumerGroupCreateExecutionTime.setRuntimeId(3L);
        rocketmqConsumerGroupCreateExecutionTime.setRuntimeName("test-runtime");
        rocketmqConsumerGroupCreateExecutionTime.setRuntimeType("type-type");
        rocketmqConsumerGroupCreateExecutionTime.setValueLe1s(1L);
        rocketmqConsumerGroupCreateExecutionTime.setValueLe3s(2L);
        rocketmqConsumerGroupCreateExecutionTime.setValueLe5s(3L);
        rocketmqConsumerGroupCreateExecutionTime.setValueLe10ms(4L);
        rocketmqConsumerGroupCreateExecutionTime.setValueLe100ms(5L);
        rocketmqConsumerGroupCreateExecutionTime.setValueLeOverflow(6L);
        rocketmqConsumerGroupCreateExecutionTime.setInvocationStatus("123");
        rocketmqConsumerGroupCreateExecutionTime.setTime(LocalDateTime.now());
        List<Object> objects = new ArrayList<>();
        objects.add(rocketmqConsumerGroupCreateExecutionTime);
        iotDBReportEngine.batchInsert("rocketmq_consumer_group_create_execution_time", objects);

    }

    @Test
    public void test_createReport() {

        //iotDBReportEngine.createReport("rocketmq_message_size");

        iotDBReportEngine.createReport("rocketmq_consumer_queueing_latency");

        //iotDBReportEngine.createReport("rocketmq_consumer_group_create_execution_time");
    }

    @Test
    public void test_query() throws ExecutionException, InterruptedException {
        SingleGeneralReportDO singleGeneralReportDO = new SingleGeneralReportDO();
        singleGeneralReportDO.setReportName("rocketmq_consumer_group_create_execution_time");
        singleGeneralReportDO.setReportType(ReportViewType.COUNTER.getName());
        singleGeneralReportDO.setOrganizationId(1L);
        singleGeneralReportDO.setClustersId(1L);
        singleGeneralReportDO.setRuntimeId(3L);
        LocalDateTime endTime = LocalDateTime.now();
        singleGeneralReportDO.setStartTime(endTime.minusDays(1L));
        singleGeneralReportDO.setEndTime(endTime);
        singleGeneralReportDO.setInterval("15m");
        CompletableFuture<List<Map<String, Object>>> query = iotDBReportEngine.query(singleGeneralReportDO);
        List<Map<String, Object>> object = query.get();
        System.out.println(object);
    }

}
