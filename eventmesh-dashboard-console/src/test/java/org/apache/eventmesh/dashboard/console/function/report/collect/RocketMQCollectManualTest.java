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

package org.apache.eventmesh.dashboard.console.function.report.collect;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.model.metadata.ClusterMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;
import org.apache.eventmesh.dashboard.console.function.report.collect.DataSyncHandler.DataSyncHandlerWrapper;
import org.apache.eventmesh.dashboard.console.function.report.collect.exporter.RocketMQCollect;
import org.apache.eventmesh.dashboard.console.function.report.model.base.OrganizationId;
import org.apache.eventmesh.dashboard.core.function.SDK.ConfigManage;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKManage;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKTypeEnum;
import org.apache.eventmesh.dashboard.core.function.SDK.config.AbstractSimpleCreateSDKConfig;
import org.apache.eventmesh.dashboard.core.function.SDK.config.NetAddress;
import org.apache.eventmesh.dashboard.core.function.SDK.operation.rocketmq.RocketMQRemotingSDKOperation.DefaultRemotingClient;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;
import org.apache.rocketmq.remoting.protocol.RequestCode;
import org.apache.rocketmq.remoting.protocol.ResponseCode;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.alibaba.fastjson.JSON;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class RocketMQCollectManualTest {

    /** 在 IDE 中运行此方法：调用一次 CollectManage.collect，读取真实 Broker 并打印全部采集结果。 */
    @Test
    @DisplayName("手动执行采集任务并打印真实 Broker 指标")
    @SuppressWarnings("unchecked")
    public void collectOnceFromRealBrokerAndPrint() throws Exception {
        RuntimeMetadata runtime = new RuntimeMetadata();
        runtime.setId(System.nanoTime());
        runtime.setClusterId(1L);
        runtime.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE);
        ClusterMetadata cluster = new ClusterMetadata();
        cluster.setId(runtime.getClusterId());
        cluster.setOrganizationId(7L);
        cluster.setClusterType(runtime.getClusterType());
        AtomicInteger printed = new AtomicInteger();
        RocketMQCollect exporter = new RocketMQCollect() {
            @Override
            protected void setData(OrganizationId data) {
                super.setData(data);
                log.info("Broker 指标={} 数据={}", data.getClass().getSimpleName(), JSON.toJSONString(data));
                printed.incrementAndGet();
            }
        };
        exporter.setRuntimeMetadata(runtime);
        exporter.setClusterMetadata(cluster);

        AbstractSimpleCreateSDKConfig config = ConfigManage.getInstance()
            .getSimpleCreateSdkConfig(runtime.getClusterType(), SDKTypeEnum.ADMIN);
        NetAddress address = new NetAddress();
        address.setAddress(System.getProperty("rocketmq.collect.host", "127.0.0.1"));
        address.setPort(Integer.getInteger("rocketmq.collect.port", 20911));
        config.setNetAddress(address);
        DefaultRemotingClient client = SDKManage.getInstance()
            .createClient(SDKTypeEnum.ADMIN, runtime, config, runtime.getClusterType());

        CollectManage collectManage = new CollectManage();
        Map<String, Collect> collectors = (Map<String, Collect>) FieldUtils.readField(collectManage, "executeingCollectMap", true);
        collectors.put(runtime.getUnique(), exporter);

        // 测试接收器只通知采集完成，不调用 IoTDB 写入链路。
        CountDownLatch completed = new CountDownLatch(1);
        DataSyncHandler printHandler = Mockito.mock(DataSyncHandler.class);
        DataSyncHandlerWrapper wrapper = Mockito.mock(DataSyncHandlerWrapper.class);
        Mockito.when(printHandler.getDataSyncHandlerWrapper(1)).thenReturn(wrapper);
        Mockito.doAnswer(call -> {
            completed.countDown();
            return null;
        }).when(wrapper).sync(Mockito.any(RestoreData.class));
        FieldUtils.writeField(collectManage, "dataSyncHandler", printHandler, true);

        ExecutorService workers = (ExecutorService) FieldUtils.readField(collectManage, "threadPoolExecutor", true);
        try {
            RemotingCommand response = client.invokeSync(
                RemotingCommand.createRequestCommand(RequestCode.GET_BROKER_RUNTIME_INFO, null), 3000);
            Assertions.assertNotNull(response, "Broker unavailable at " + address);
            Assertions.assertEquals(ResponseCode.SUCCESS, response.getCode(), "Broker unavailable at " + address);

            // 这一行就是 ReportHandlerManage 定时任务实际调用的采集方法。
            collectManage.collect();
            Assertions.assertTrue(completed.await(10, TimeUnit.SECONDS), "Collection did not finish");
            Assertions.assertTrue(printed.get() > 0, "Broker collection returned no data");
        } finally {
            workers.shutdownNow();
            client.shutdown();
            SDKManage.getInstance().deleteClient(null, runtime.getUnique());
        }
    }
}
