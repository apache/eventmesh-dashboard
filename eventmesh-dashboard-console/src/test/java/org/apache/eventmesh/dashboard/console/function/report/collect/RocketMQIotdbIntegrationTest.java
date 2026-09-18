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
import org.apache.eventmesh.dashboard.common.enums.MetricFamily;
import org.apache.eventmesh.dashboard.common.model.metadata.ClusterMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;
import org.apache.eventmesh.dashboard.console.function.report.ReportConfig.ReportEngineConfig;
import org.apache.eventmesh.dashboard.console.function.report.ReportHandlerManage;
import org.apache.eventmesh.dashboard.console.function.report.iotdb.RocketMQIotdbReportEngine;
import org.apache.eventmesh.dashboard.console.function.report.model.SingleGeneralReportDO;
import org.apache.eventmesh.dashboard.core.function.SDK.ConfigManage;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKManage;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKTypeEnum;
import org.apache.eventmesh.dashboard.core.function.SDK.config.AbstractSimpleCreateSDKConfig;
import org.apache.eventmesh.dashboard.core.function.SDK.config.NetAddress;
import org.apache.eventmesh.dashboard.core.function.SDK.operation.rocketmq.RocketMQRemotingSDKOperation.DefaultRemotingClient;

import org.apache.rocketmq.remoting.protocol.RemotingCommand;
import org.apache.rocketmq.remoting.protocol.RemotingSerializable;
import org.apache.rocketmq.remoting.protocol.RequestCode;
import org.apache.rocketmq.remoting.protocol.ResponseCode;
import org.apache.rocketmq.remoting.protocol.header.CreateTopicRequestHeader;
import org.apache.rocketmq.remoting.protocol.header.DeleteSubscriptionGroupRequestHeader;
import org.apache.rocketmq.remoting.protocol.header.DeleteTopicRequestHeader;
import org.apache.rocketmq.remoting.protocol.header.SendMessageRequestHeader;
import org.apache.rocketmq.remoting.protocol.header.UpdateConsumerOffsetRequestHeader;
import org.apache.rocketmq.remoting.protocol.heartbeat.HeartbeatData;
import org.apache.rocketmq.remoting.protocol.heartbeat.ProducerData;
import org.apache.rocketmq.remoting.protocol.subscription.SubscriptionGroupConfig;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import lombok.extern.slf4j.Slf4j;

/** Opt-in real RocketMQ -> asynchronous collector -> IoTDB -> report query test. No MySQL. */
@Slf4j
class RocketMQIotdbIntegrationTest {
    @Test
    void collectsTwoRoundsAndReadsPersistedSamples() throws Exception {
        RuntimeMetadata runtime = new RuntimeMetadata();
        runtime.setId(System.currentTimeMillis());
        runtime.setClusterId(runtime.getId());
        runtime.setName("iotdb-test-broker");
        runtime.setStatus(1L);
        runtime.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE);
        ClusterMetadata cluster = new ClusterMetadata();
        cluster.setId(runtime.getClusterId());
        cluster.setOrganizationId(99001L);
        cluster.setName("iotdb-test");
        cluster.setStatus(1L);
        cluster.setClusterType(runtime.getClusterType());
        AbstractSimpleCreateSDKConfig sdk = ConfigManage.getInstance().getSimpleCreateSdkConfig(runtime.getClusterType(), SDKTypeEnum.ADMIN);
        NetAddress address = new NetAddress();
        address.setAddress("127.0.0.1");
        address.setPort(Integer.getInteger("rocketmq.admin.broker.port", 21911));
        sdk.setNetAddress(address);
        SDKManage.getInstance().createClient(SDKTypeEnum.ADMIN, runtime, sdk, runtime.getClusterType());
        DefaultRemotingClient client = SDKManage.getInstance().getClient(SDKTypeEnum.ADMIN, runtime.getUnique());
        RocketMQIotdbReportEngine engine = new RocketMQIotdbReportEngine();
        ReportEngineConfig engineConfig = new ReportEngineConfig();
        engineConfig.setEngineAddress(System.getProperty("iotdb.address", "127.0.0.1:6667"));
        engine.setReportEngineConfig(engineConfig);
        engine.init();
        engine.createReport("rocketmq_broker_sample");
        ReportHandlerManage reports = new ReportHandlerManage();
        reports.setReportEngine(engine);
        org.springframework.context.annotation.AnnotationConfigApplicationContext collectionContext =
            new org.springframework.context.annotation.AnnotationConfigApplicationContext();
        collectionContext.getBeanFactory().registerSingleton("rocketMQMetricsEngine", engine);
        collectionContext.register(RocketMQDataSyncHandler.class, RocketMQCollectManage.class);
        collectionContext.refresh();
        RocketMQCollectManage manager = collectionContext.getBean(RocketMQCollectManage.class);
        RuntimeMetadata unavailable = new RuntimeMetadata();
        unavailable.setId(runtime.getId() + 1);
        unavailable.setClusterId(runtime.getClusterId());
        unavailable.setName("unreachable-test-broker");
        unavailable.setClusterType(runtime.getClusterType());
        unavailable.setStatus(1L);
        AbstractSimpleCreateSDKConfig unavailableConfig = ConfigManage.getInstance().getSimpleCreateSdkConfig(
            unavailable.getClusterType(), SDKTypeEnum.ADMIN);
        NetAddress unavailableAddress = new NetAddress();
        unavailableAddress.setAddress("127.0.0.1");
        try (java.net.ServerSocket socket = new java.net.ServerSocket(0)) {
            unavailableAddress.setPort(socket.getLocalPort());
        }
        unavailableConfig.setNetAddress(unavailableAddress);
        SDKManage.getInstance().createClient(SDKTypeEnum.ADMIN, unavailable, unavailableConfig, unavailable.getClusterType());
        DefaultRemotingClient unavailableClient = SDKManage.getInstance().getClient(SDKTypeEnum.ADMIN, unavailable.getUnique());
        manager.register(cluster, unavailable);
        String topic = "iotdb_collect_" + UUID.randomUUID().toString().replace("-", "");
        LocalDateTime start = LocalDateTime.now().minusSeconds(1);
        try {
            CreateTopicRequestHeader create = new CreateTopicRequestHeader();
            create.setTopic(topic);
            create.setDefaultTopic("TBW102");
            create.setReadQueueNums(1);
            create.setWriteQueueNums(1);
            create.setPerm(6);
            create.setTopicFilterType("SINGLE_TAG");
            assertSuccess(client.invokeSync(RemotingCommand.createRequestCommand(RequestCode.UPDATE_AND_CREATE_TOPIC, create), 3000));
            HeartbeatData heartbeat = new HeartbeatData();
            heartbeat.setClientID(topic);
            ProducerData producer = new ProducerData();
            producer.setGroupName(topic);
            heartbeat.getProducerDataSet().add(producer);
            RemotingCommand beat = RemotingCommand.createRequestCommand(RequestCode.HEART_BEAT, null);
            beat.setBody(RemotingSerializable.encode(heartbeat));
            assertSuccess(client.invokeSync(beat, 3000));
            for (int i = 0; i < 3; i++) {
                SendMessageRequestHeader header = new SendMessageRequestHeader();
                header.setProducerGroup(topic);
                header.setTopic(topic);
                header.setDefaultTopic(topic);
                header.setDefaultTopicQueueNums(1);
                header.setQueueId(0);
                header.setSysFlag(0);
                header.setBornTimestamp(System.currentTimeMillis());
                header.setFlag(0);
                header.setReconsumeTimes(0);
                header.setUnitMode(false);
                header.setBatch(false);
                RemotingCommand send = RemotingCommand.createRequestCommand(RequestCode.SEND_MESSAGE, header);
                send.setBody(("iotdb-metric-" + i).getBytes(StandardCharsets.UTF_8));
                assertSuccess(client.invokeSync(send, 3000));
            }
            SubscriptionGroupConfig group = new SubscriptionGroupConfig();
            group.setGroupName(topic);
            RemotingCommand createGroup = RemotingCommand.createRequestCommand(RequestCode.UPDATE_AND_CREATE_SUBSCRIPTIONGROUP, null);
            createGroup.setBody(RemotingSerializable.encode(group));
            assertSuccess(client.invokeSync(createGroup, 3000));
            org.apache.rocketmq.remoting.protocol.header.PullMessageRequestHeader pull =
                new org.apache.rocketmq.remoting.protocol.header.PullMessageRequestHeader();
            pull.setConsumerGroup(topic);
            pull.setTopic(topic);
            pull.setQueueId(0);
            pull.setQueueOffset(0L);
            pull.setMaxMsgNums(2);
            pull.setSysFlag(org.apache.rocketmq.common.sysflag.PullSysFlag.buildSysFlag(false, false, true, false));
            pull.setCommitOffset(0L);
            pull.setSuspendTimeoutMillis(0L);
            pull.setSubscription("*");
            pull.setSubVersion(System.currentTimeMillis());
            pull.setExpressionType("TAG");
            assertSuccess(client.invokeSync(RemotingCommand.createRequestCommand(RequestCode.PULL_MESSAGE, pull), 3000));
            UpdateConsumerOffsetRequestHeader committed = new UpdateConsumerOffsetRequestHeader();
            committed.setConsumerGroup(topic);
            committed.setTopic(topic);
            committed.setQueueId(0);
            committed.setCommitOffset(2L);
            assertSuccess(client.invokeSync(RemotingCommand.createRequestCommand(RequestCode.UPDATE_CONSUMER_OFFSET, committed), 3000));
            manager.register(cluster, runtime);
            CompletableFuture.allOf(Arrays.stream(
                MetricFamily.values())
                .map(manager::collectAsync).toArray(CompletableFuture<?>[]::new)).get(15, TimeUnit.SECONDS);
            Thread.sleep(5000);
            CompletableFuture.allOf(Arrays.stream(
                MetricFamily.values())
                .map(manager::collectAsync).toArray(CompletableFuture<?>[]::new)).get(15, TimeUnit.SECONDS);
            SingleGeneralReportDO query = new SingleGeneralReportDO();
            query.setReportName("rocketmq_broker_sample");
            query.setReportType("gauge");
            query.setOrganizationId(cluster.getOrganizationId());
            query.setRuntimeId(runtime.getId());
            query.setStartTime(start);
            query.setEndTime(LocalDateTime.now().plusSeconds(1));
            List<Map<String, Object>> rows = reports.queryResultIsMap(List.of(query)).get("gauge");
            this.logCollectedMetrics("正常实例", runtime.getId(), rows);
            Assertions.assertFalse(rows.isEmpty());
            List<Map<String, Object>> offsets = rows.stream().filter(row -> "queue_max_offset".equals(row.get("metric_id"))
                && topic.equals(row.get("topic_key_id"))).toList();
            Assertions.assertEquals(2, offsets.size(), rows.toString());
            offsets.forEach(row -> Assertions.assertEquals(3.0, ((Number) row.get("value")).doubleValue()));
            List<Map<String, Object>> lag = rows.stream().filter(row -> "offset_lag".equals(row.get("metric_id"))
                && topic.equals(row.get("topic_key_id")) && topic.equals(row.get("group_key_id"))).toList();
            Assertions.assertEquals(2, lag.size());
            lag.forEach(row -> Assertions.assertEquals(1L, ((Number) row.get("value_long")).longValue()));
            Assertions.assertTrue(rows.stream().anyMatch(row -> "topic_put_nums_tps".equals(row.get("metric_id"))
                && topic.equals(row.get("topic_key_id"))));
            Assertions.assertTrue(rows.stream().anyMatch(row -> "connection_count".equals(row.get("metric_id"))
                && ((Number) row.get("value")).doubleValue() >= 1));
            Assertions.assertTrue(rows.stream().anyMatch(row -> "broker_reachable".equals(row.get("metric_id"))
                && ((Number) row.get("value")).doubleValue() == 1));
            Assertions.assertTrue(rows.stream().anyMatch(row -> "group_get_nums_tps".equals(row.get("metric_id"))
                && topic.equals(row.get("topic_key_id")) && topic.equals(row.get("group_key_id"))));
            Assertions.assertTrue(rows.stream().anyMatch(row -> "broker_put_nums_tps".equals(row.get("metric_id"))));
            Assertions.assertTrue(rows.stream().anyMatch(row -> "flush_behind_bytes".equals(row.get("metric_id"))));
            Assertions.assertTrue(rows.stream().filter(row -> "collection_failures".equals(row.get("metric_id")))
                .allMatch(row -> ((Number) row.get("value")).doubleValue() == 0), rows.toString());
            query.setRuntimeId(unavailable.getId());
            List<Map<String, Object>> missing = reports.queryResultIsMap(List.of(query)).get("gauge");
            this.logCollectedMetrics("不可达实例", unavailable.getId(), missing);
            Assertions.assertFalse(missing.isEmpty());
            Assertions.assertTrue(missing.stream().anyMatch(row -> "broker_reachable".equals(row.get("metric_id"))
                && ((Number) row.get("value")).doubleValue() == 0));
            Assertions.assertFalse(missing.stream().anyMatch(row -> "connection_count".equals(row.get("metric_id"))));
            System.out.println("REAL COLLECTION VERIFIED runtime=" + runtime.getId() + " rows=" + rows.size() + " topic=" + topic);
        } finally {
            collectionContext.close();
            unavailableClient.shutdown();
            SDKManage.getInstance().deleteClient(SDKTypeEnum.ADMIN, unavailable.getUnique());
            engine.close();
            DeleteTopicRequestHeader delete = new DeleteTopicRequestHeader();
            delete.setTopic(topic);
            assertSuccess(client.invokeSync(RemotingCommand.createRequestCommand(RequestCode.DELETE_TOPIC_IN_BROKER, delete), 3000));
            DeleteSubscriptionGroupRequestHeader deleteGroup = new DeleteSubscriptionGroupRequestHeader();
            deleteGroup.setGroupName(topic);
            assertSuccess(client.invokeSync(RemotingCommand.createRequestCommand(RequestCode.DELETE_SUBSCRIPTIONGROUP, deleteGroup), 3000));
            client.shutdown();
            SDKManage.getInstance().deleteClient(SDKTypeEnum.ADMIN, runtime.getUnique());
        }
    }

    /** Print persisted observations, retaining exact integer values and every resource/window dimension. */
    private void logCollectedMetrics(String target, Long runtimeId, List<Map<String, Object>> rows) {
        List<String> metrics = rows.stream().map(row -> String.valueOf(row.get("metric_id"))).distinct().sorted().toList();
        log.info("【采集回读汇总】目标={}，实例={}，样本数={}，指标种类={}，指标清单={}", target, runtimeId, rows.size(), metrics.size(), metrics);
        rows.stream().sorted(Comparator
            .comparing((Map<String, Object> row) -> String.valueOf(row.get("time")))
            .thenComparing(row -> String.valueOf(row.get("family_id")))
            .thenComparing(row -> String.valueOf(row.get("metric_id")))
            .thenComparing(row -> String.valueOf(row.get("topic_key_id")))
            .thenComparing(row -> String.valueOf(row.get("group_key_id")))
            .thenComparing(row -> String.valueOf(row.get("queue_key_id")))
            .thenComparing(row -> String.valueOf(row.get("window_id"))))
            .forEach(row -> {
                String metric = String.valueOf(row.get("metric_id"));
                Object value = row.get("value_long") == null ? row.get("value") : row.get("value_long");
                log.info("【采集指标】目标={}，实例={}，采样时间={}，分类={}，指标={}（{}），Topic={}，Group={}，Queue={}，窗口={}，数值={}",
                    target, runtimeId, row.get("time"), row.get("family_id"), metric, this.metricDescription(metric),
                    row.get("topic_key_id"), row.get("group_key_id"), row.get("queue_key_id"), row.get("window_id"), value);
            });
        if ("不可达实例".equals(target)) {
            log.info("【失败隔离】实例={}：仅记录可达性和采集自监控；连接数、位点、速率缺失，不补零。", runtimeId);
        }
    }

    private String metricDescription(String metric) {
        switch (metric) {
            case "topic_count":
                return "配置的 Topic 数量，个，含系统 Topic";
            case "group_count":
                return "配置的消费组数量，个";
            case "connection_count":
                return "实例连接去重数量，条";
            case "queue_min_offset":
                return "队列最小逻辑位点";
            case "queue_max_offset":
                return "队列最大逻辑位点";
            case "queue_last_update_ms":
            case "topic_last_update_ms":
                return "最后更新时间，Unix 毫秒";
            case "topic_min_offset_sum":
            case "broker_topic_min_offset_sum":
                return "最小逻辑位点之和";
            case "topic_max_offset_sum":
            case "broker_topic_max_offset_sum":
                return "最大逻辑位点之和，不是累计消息数";
            case "consumer_offset":
                return "消费组已提交逻辑位点";
            case "broker_offset":
                return "Broker 逻辑位点";
            case "offset_lag":
                return "消费位点差，max(Broker位点-消费位点,0)";
            case "offset_negative":
                return "位点差异常标记，1=负差，0=正常";
            case "topic_put_nums_tps":
                return "Topic 写入速率，条/秒";
            case "topic_put_size_tps":
                return "Topic 写入字节速率，字节/秒";
            case "group_get_nums_tps":
                return "消费组读取速率，条/秒";
            case "group_get_size_tps":
                return "消费组读取字节速率，字节/秒";
            case "sndbck_put_nums_tps":
                return "发送回退速率，条/秒";
            case "broker_put_nums_tps":
            case "putTps":
                return "Broker 写入速率，条/秒";
            case "broker_get_nums_tps":
            case "getTransferredTps":
                return "Broker 读取速率，条/秒";
            case "broker_reachable":
                return "采集可达性，1=成功，0=失败";
            case "flush_behind_bytes":
                return "刷盘积压，字节";
            case "dispatch_behind_bytes":
                return "分发积压，字节";
            case "broker_stored_bytes_total":
                return "进程周期累计存储字节，窗口字段为启动时间";
            case "collection_duration_ms":
                return "采集耗时，毫秒";
            case "collection_failures":
                return "当前采集器累计失败次数";
            case "collection_last_success_ms":
                return "最后成功时间，Unix 毫秒";
            default:
                return "原始指标值";
        }
    }

    private void assertSuccess(RemotingCommand reply) {
        Assertions.assertEquals(ResponseCode.SUCCESS, reply.getCode(), reply.getRemark());
    }
}
