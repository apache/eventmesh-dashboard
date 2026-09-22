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

package org.apache.eventmesh.dashboard.console.function.report.collect.exporter;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;
import org.apache.eventmesh.dashboard.console.function.report.ReportViewType;
import org.apache.eventmesh.dashboard.console.function.report.annotation.ReportMeta;
import org.apache.eventmesh.dashboard.console.function.report.annotation.ReportMetaData;
import org.apache.eventmesh.dashboard.console.function.report.annotation.ReportTag;
import org.apache.eventmesh.dashboard.console.function.report.iotdb.IotDBReportMetaHandler;
import org.apache.eventmesh.dashboard.console.function.report.model.base.RuntimeId.RuntimeFloatValue;
import org.apache.eventmesh.dashboard.console.function.report.model.base.RuntimeId.RuntimeLongValue;
import org.apache.eventmesh.dashboard.console.function.report.model.rocketmq.RocketmqBrokerMessagesIn;
import org.apache.eventmesh.dashboard.console.function.report.model.rocketmq.RocketmqBrokerMessagesOut;
import org.apache.eventmesh.dashboard.console.function.report.model.rocketmq.RocketmqConsumerConnectionNumber;
import org.apache.eventmesh.dashboard.console.function.report.model.rocketmq.RocketmqConsumerFailedTps;
import org.apache.eventmesh.dashboard.console.function.report.model.rocketmq.RocketmqConsumerGroupNumber;
import org.apache.eventmesh.dashboard.console.function.report.model.rocketmq.RocketmqConsumerLagLatency;
import org.apache.eventmesh.dashboard.console.function.report.model.rocketmq.RocketmqConsumerOffset;
import org.apache.eventmesh.dashboard.console.function.report.model.rocketmq.RocketmqConsumerProcessTime;
import org.apache.eventmesh.dashboard.console.function.report.model.rocketmq.RocketmqConsumerSuccessTps;
import org.apache.eventmesh.dashboard.console.function.report.model.rocketmq.RocketmqGroupMessagesOut;
import org.apache.eventmesh.dashboard.console.function.report.model.rocketmq.RocketmqStorageDiskFreeBytes;
import org.apache.eventmesh.dashboard.console.function.report.model.rocketmq.RocketmqStorageDiskUsage;
import org.apache.eventmesh.dashboard.console.function.report.model.rocketmq.RocketmqStorageDispatchBehindBytes;
import org.apache.eventmesh.dashboard.console.function.report.model.rocketmq.RocketmqStorageFlushBehindBytes;
import org.apache.eventmesh.dashboard.console.function.report.model.rocketmq.RocketmqStorageMessageReserveTime;
import org.apache.eventmesh.dashboard.console.function.report.model.rocketmq.RocketmqThreadPoolWartermark;
import org.apache.eventmesh.dashboard.console.function.report.model.rocketmq.RocketmqTopicMessagesIn;
import org.apache.eventmesh.dashboard.console.function.report.model.rocketmq.RocketmqTopicNumber;
import org.apache.eventmesh.dashboard.console.mapstruct.report.RocketMQCollectMapper;
import org.apache.eventmesh.dashboard.core.function.SDK.ConfigManage;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKManage;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKTypeEnum;
import org.apache.eventmesh.dashboard.core.function.SDK.config.AbstractSimpleCreateSDKConfig;
import org.apache.eventmesh.dashboard.core.function.SDK.config.NetAddress;
import org.apache.eventmesh.dashboard.core.function.SDK.operation.rocketmq.RocketMQRemotingSDKOperation.DefaultRemotingClient;

import org.apache.rocketmq.remoting.InvokeCallback;
import org.apache.rocketmq.remoting.netty.ResponseFuture;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;
import org.apache.rocketmq.remoting.protocol.RemotingSerializable;
import org.apache.rocketmq.remoting.protocol.RequestCode;
import org.apache.rocketmq.remoting.protocol.ResponseCode;
import org.apache.rocketmq.remoting.protocol.body.KVTable;
import org.apache.rocketmq.remoting.protocol.header.CreateTopicRequestHeader;
import org.apache.rocketmq.remoting.protocol.header.DeleteSubscriptionGroupRequestHeader;
import org.apache.rocketmq.remoting.protocol.header.DeleteTopicRequestHeader;
import org.apache.rocketmq.remoting.protocol.header.SendMessageRequestHeader;
import org.apache.rocketmq.remoting.protocol.header.UpdateConsumerOffsetRequestHeader;
import org.apache.rocketmq.remoting.protocol.header.ViewBrokerStatsDataRequestHeader;
import org.apache.rocketmq.remoting.protocol.heartbeat.HeartbeatData;
import org.apache.rocketmq.remoting.protocol.heartbeat.ProducerData;
import org.apache.rocketmq.remoting.protocol.subscription.SubscriptionGroupConfig;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;


public class RocketMQCollectTest {
    @Test
    public void mapsConsumerSamplesToDedicatedModels() {
        var offset = new org.apache.rocketmq.remoting.protocol.admin.OffsetWrapper();
        offset.setBrokerOffset(9007199254740993L);
        offset.setConsumerOffset(9007199254740991L);
        RocketmqConsumerOffset row = RocketMQCollectMapper.INSTANCE.consumerOffset("orders", "buyers", "3", offset);
        Assertions.assertEquals("orders", row.getTopicName());
        Assertions.assertEquals("buyers", row.getGroupName());
        Assertions.assertEquals("3", row.getQueueId());
        Assertions.assertNull(org.apache.commons.lang3.reflect.FieldUtils.getField(RocketmqConsumerOffset.class, "topicId", true));
        Assertions.assertNull(org.apache.commons.lang3.reflect.FieldUtils.getField(RocketmqConsumerOffset.class, "groupId", true));
        Assertions.assertEquals(9007199254740993L, row.getValueBrokerOffset());
        Assertions.assertEquals(9007199254740991L, row.getValueConsumerOffset());
        Assertions.assertEquals(2L, row.getValueOffsetLag());
        offset.setConsumerOffset(9007199254740994L);
        Assertions.assertEquals(0L,
            RocketMQCollectMapper.INSTANCE.consumerOffset("orders", "buyers", "3", offset).getValueOffsetLag());
        RocketmqConsumerConnectionNumber connections = RocketMQCollectMapper.INSTANCE.connections("buyers", 2L);
        Assertions.assertEquals("buyers", connections.getGroupName());
        Assertions.assertEquals(2L, connections.getValueConnectionCount());
        Assertions.assertEquals(0L, RocketMQCollectMapper.INSTANCE.connections("buyers", 0L).getValueConnectionCount());
    }

    @Test
    public void dedicatedModelsDescribeSeparateGaugeTables() {
        assertReportTable(org.apache.eventmesh.dashboard.console.function.report.model.rocketmq.Rocketmq2ProducerOffset.class,
            "rocketmq_producer_offset", List.of("topic_name", "queue_id"),
            List.of("value", "value_min_offset", "value_last_update_time", "value_min_offset_sum", "value_max_offset_sum"));
        assertReportTable(RocketmqConsumerOffset.class, "rocketmq_consumer_offset",
            List.of("topic_name", "group_name", "queue_id"),
            List.of("value_consumer_offset", "value_broker_offset", "value_offset_lag"));
        assertReportTable(RocketmqConsumerConnectionNumber.class, "rocketmq_consumer_connection_number",
            List.of("group_name"), List.of("value_connection_count"));
    }

    private void assertReportTable(Class<?> model, String table, List<String> tags, List<String> measurements) {
        assertReportTable(model, table, tags, measurements, List.of());
    }

    private void assertReportTable(Class<?> model, String table, List<String> tags, List<String> measurements, List<String> floats) {
        ReportMeta annotation = model.getAnnotation(ReportMeta.class);
        Assertions.assertNotNull(annotation);
        Assertions.assertEquals(table, annotation.tableName());
        Assertions.assertEquals(table, annotation.reportName());
        Assertions.assertEquals(ReportViewType.GAUGE, annotation.defaultViewType());
        var metadata = new ReportMetaData();
        metadata.setClazz(model);
        metadata.setTableName(annotation.tableName());
        metadata.setComment(annotation.comment());
        var handler = new IotDBReportMetaHandler();
        handler.setReportMeta(metadata);
        handler.setFieldList(org.apache.commons.lang3.reflect.FieldUtils.getAllFieldsList(model));
        String ddl = handler.createTable();
        Assertions.assertTrue(ddl.startsWith("create table if not exists " + table + " "));
        tags.forEach(tag -> Assertions.assertTrue(ddl.contains(tag + " string  tag"), ddl));
        measurements.forEach(field -> Assertions.assertTrue(ddl.contains(field + " int64  field"), ddl));
        floats.forEach(field -> Assertions.assertTrue(ddl.contains(field + " float  field"), ddl));
        Assertions.assertTrue(ddl.contains("runtime_id string  tag"), ddl);
    }

    @Test
    public void handsConsumerSamplesToParentUnderDedicatedClasses() throws Exception {
        DefaultRemotingClient client = Mockito.mock(DefaultRemotingClient.class);
        SDKManage sdk = Mockito.mock(SDKManage.class);
        RuntimeMetadata runtime = runtime();
        Mockito.when(sdk.getClient(SDKTypeEnum.ADMIN, runtime.getUnique())).thenReturn(client);
        AtomicInteger connectionRequests = new AtomicInteger();
        Mockito.doAnswer(invocation -> {
            RemotingCommand request = invocation.getArgument(0);
            InvokeCallback callback = invocation.getArgument(2);
            if (failBrokerRoot(request, callback)) {
                return null;
            }
            byte[] body;
            switch (request.getCode()) {
                case RequestCode.GET_ALL_TOPIC_CONFIG:
                    body = "{\"topicConfigTable\":{\"orders\":{},\"payments\":{}}}".getBytes(StandardCharsets.UTF_8);
                    break;
                case RequestCode.GET_TOPIC_STATS_INFO:
                    body = RemotingSerializable.encode(new org.apache.rocketmq.remoting.protocol.admin.TopicStatsTable());
                    break;
                case RequestCode.QUERY_TOPIC_CONSUME_BY_WHO:
                    body = "{\"groupList\":[\"buyers\"]}".getBytes(StandardCharsets.UTF_8);
                    break;
                case RequestCode.GET_CONSUMER_CONNECTION_LIST:
                    connectionRequests.incrementAndGet();
                    body = "{\"connectionSet\":[]}".getBytes(StandardCharsets.UTF_8);
                    break;
                case RequestCode.GET_CONSUME_STATS:
                    var header = (org.apache.rocketmq.remoting.protocol.header.GetConsumeStatsRequestHeader) request.readCustomHeader();
                    var stats = new org.apache.rocketmq.remoting.protocol.admin.ConsumeStats();
                    var offset = new org.apache.rocketmq.remoting.protocol.admin.OffsetWrapper();
                    offset.setBrokerOffset(3L);
                    offset.setConsumerOffset(2L);
                    stats.getOffsetTable().put(new org.apache.rocketmq.common.message.MessageQueue(header.getTopic(), "broker", 0), offset);
                    body = RemotingSerializable.encode(stats);
                    break;
                default:
                    throw new AssertionError("Unexpected request " + request.getCode());
            }
            reply(callback, ResponseCode.SUCCESS, body);
            return null;
        }).when(client).invokeAsync(Mockito.any(), Mockito.anyLong(), Mockito.any());
        var wrapper = Mockito.mock(org.apache.eventmesh.dashboard.console.function.report.collect.DataSyncHandler.DataSyncHandlerWrapper.class);
        try (var mocked = Mockito.mockStatic(SDKManage.class)) {
            mocked.when(SDKManage::getInstance).thenReturn(sdk);
            collector(runtime).collect(0, wrapper);
        }
        var capture = org.mockito.ArgumentCaptor.forClass(org.apache.eventmesh.dashboard.console.function.report.collect.RestoreData.class);
        Mockito.verify(wrapper).sync(capture.capture());
        var models = capture.getValue().getDataMap();
        Assertions.assertEquals(Set.of(RocketmqConsumerOffset.class, RocketmqConsumerConnectionNumber.class,
            org.apache.eventmesh.dashboard.console.function.report.model.rocketmq.Rocketmq2ProducerOffset.class, RocketmqTopicNumber.class), models.keySet());
        Assertions.assertEquals(2, models.get(RocketmqConsumerOffset.class).size());
        models.get(RocketmqConsumerOffset.class).stream().map(RocketmqConsumerOffset.class::cast).forEach(row -> {
            Assertions.assertEquals("buyers", row.getGroupName());
            Assertions.assertEquals(2L, row.getValueConsumerOffset());
            Assertions.assertEquals(3L, row.getValueBrokerOffset());
            Assertions.assertEquals(1L, row.getValueOffsetLag());
            Assertions.assertEquals(runtime.getId(), row.getRuntimeId());
            Assertions.assertEquals(7L, row.getOrganizationId());
        });
        Assertions.assertEquals(1, connectionRequests.get());
        Assertions.assertEquals(1, models.get(RocketmqConsumerConnectionNumber.class).size());
    }

    @Test
    public void waitsForNestedRequestsThenHandsModelsToParent() throws Exception {
        DefaultRemotingClient client = Mockito.mock(DefaultRemotingClient.class);
        SDKManage sdk = Mockito.mock(SDKManage.class);
        RuntimeMetadata runtime = runtime();
        RocketMQCollect collector = collector(runtime);
        Mockito.when(sdk.getClient(SDKTypeEnum.ADMIN, runtime.getUnique())).thenReturn(client);
        var calls = new java.util.concurrent.LinkedBlockingQueue<Call>();
        var started = new java.util.concurrent.CountDownLatch(1);
        Mockito.doAnswer(invocation -> {
            if (failBrokerRoot(invocation.getArgument(0), invocation.getArgument(2))) {
                return null;
            }
            calls.add(new Call(invocation.getArgument(0), invocation.getArgument(2)));
            started.countDown();
            return null;
        }).when(client).invokeAsync(Mockito.any(), Mockito.anyLong(), Mockito.any());
        var wrapper = Mockito.mock(org.apache.eventmesh.dashboard.console.function.report.collect.DataSyncHandler.DataSyncHandlerWrapper.class);
        var executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        try {
            var finished = executor.submit(() -> {
                try (var mocked = Mockito.mockStatic(SDKManage.class)) {
                    mocked.when(SDKManage::getInstance).thenReturn(sdk);
                    collector.collect(0, wrapper);
                }
            });
            Assertions.assertTrue(started.await(2, java.util.concurrent.TimeUnit.SECONDS));
            Call root = calls.poll(2, java.util.concurrent.TimeUnit.SECONDS);
            Assertions.assertNotNull(root);
            Assertions.assertEquals(RequestCode.GET_ALL_TOPIC_CONFIG, root.request().getCode());
            reply(root.callback(), ResponseCode.SUCCESS,
                "{\"topicConfigTable\":{\"test-topic\":{\"topicName\":\"test-topic\"}}}".getBytes(StandardCharsets.UTF_8));
            Call offset = null;
            for (int i = 0; i < 2; i++) {
                Call child = calls.poll(2, java.util.concurrent.TimeUnit.SECONDS);
                Assertions.assertNotNull(child);
                if (child.request().getCode() == RequestCode.GET_TOPIC_STATS_INFO) {
                    offset = child;
                } else {
                    reply(child.callback(), ResponseCode.SYSTEM_ERROR, null);
                    child.callback().operationFail(new IllegalStateException("duplicate completion"));
                }
            }
            Assertions.assertNotNull(offset);
            Assertions.assertFalse(finished.isDone(), "Nested Topic request must still be awaited");
            Mockito.verifyNoInteractions(wrapper);
            var stats = new org.apache.rocketmq.remoting.protocol.admin.TopicStatsTable();
            var position = new org.apache.rocketmq.remoting.protocol.admin.TopicOffset();
            position.setMinOffset(1L);
            position.setMaxOffset(9007199254740993L);
            position.setLastUpdateTimestamp(123L);
            stats.getOffsetTable().put(new org.apache.rocketmq.common.message.MessageQueue("test-topic", "broker", 0), position);
            reply(offset.callback(), ResponseCode.SUCCESS, RemotingSerializable.encode(stats));
            finished.get(2, java.util.concurrent.TimeUnit.SECONDS);
            var capture = org.mockito.ArgumentCaptor.forClass(org.apache.eventmesh.dashboard.console.function.report.collect.RestoreData.class);
            Mockito.verify(wrapper).sync(capture.capture());
            var rows = capture.getValue().getDataMap().get(
                org.apache.eventmesh.dashboard.console.function.report.model.rocketmq.Rocketmq2ProducerOffset.class);
            var queue = rows.stream().map(row ->
                (org.apache.eventmesh.dashboard.console.function.report.model.rocketmq.Rocketmq2ProducerOffset) row)
                .filter(row -> "0".equals(row.getQueueId())).findFirst().orElseThrow();
            Assertions.assertEquals(9007199254740993L, queue.getValue());
            Assertions.assertEquals(1L, queue.getValueMinOffset());
            Assertions.assertEquals(123L, queue.getValueLastUpdateTime());
            Assertions.assertEquals(runtime.getId(), queue.getRuntimeId());
            Assertions.assertEquals(7L, queue.getOrganizationId());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void timeoutDoesNotLetLateCallbacksWriteIntoParent() throws Exception {
        DefaultRemotingClient client = Mockito.mock(DefaultRemotingClient.class);
        SDKManage sdk = Mockito.mock(SDKManage.class);
        RuntimeMetadata runtime = runtime();
        RocketMQCollect collector = collector(runtime);
        Mockito.when(sdk.getClient(SDKTypeEnum.ADMIN, runtime.getUnique())).thenReturn(client);
        List<InvokeCallback> callbacks = new CopyOnWriteArrayList<>();
        Mockito.doAnswer(call -> {
            if (failBrokerRoot(call.getArgument(0), call.getArgument(2))) {
                return null;
            }
            callbacks.add(call.getArgument(2));
            return null;
        }).when(client).invokeAsync(Mockito.any(), Mockito.anyLong(), Mockito.any());
        var wrapper = Mockito.mock(org.apache.eventmesh.dashboard.console.function.report.collect.DataSyncHandler.DataSyncHandlerWrapper.class);
        try (var mocked = Mockito.mockStatic(SDKManage.class)) {
            mocked.when(SDKManage::getInstance).thenReturn(sdk);
            Assertions.assertTimeout(java.time.Duration.ofSeconds(6), () -> collector.collect(0, wrapper));
            var capture = org.mockito.ArgumentCaptor.forClass(org.apache.eventmesh.dashboard.console.function.report.collect.RestoreData.class);
            Mockito.verify(wrapper).sync(capture.capture());
            Assertions.assertTrue(capture.getValue().getDataMap().isEmpty());
            reply(callbacks.get(0), ResponseCode.SUCCESS, "{\"topicConfigTable\":{}}".getBytes(StandardCharsets.UTF_8));
            Assertions.assertTrue(capture.getValue().getDataMap().isEmpty());
            Mockito.verify(wrapper, Mockito.times(1)).sync(Mockito.any());
        }
    }

    @Test
    public void publishesDirectlyAndRejectsPreviousRoundCallbacks() throws Exception {
        DefaultRemotingClient client = Mockito.mock(DefaultRemotingClient.class);
        SDKManage sdk = Mockito.mock(SDKManage.class);
        RuntimeMetadata runtime = runtime();
        RocketMQCollect collector = collector(runtime);
        Mockito.when(sdk.getClient(SDKTypeEnum.ADMIN, runtime.getUnique())).thenReturn(client);
        var calls = new java.util.concurrent.LinkedBlockingQueue<Call>();
        Mockito.doAnswer(invocation -> {
            if (failBrokerRoot(invocation.getArgument(0), invocation.getArgument(2))) {
                return null;
            }
            calls.add(new Call(invocation.getArgument(0), invocation.getArgument(2)));
            return null;
        }).when(client).invokeAsync(Mockito.any(), Mockito.anyLong(), Mockito.any());
        var wrapper = Mockito.mock(org.apache.eventmesh.dashboard.console.function.report.collect.DataSyncHandler.DataSyncHandlerWrapper.class);
        var executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        Runnable collect = () -> {
            try (var mocked = Mockito.mockStatic(SDKManage.class)) {
                mocked.when(SDKManage::getInstance).thenReturn(sdk);
                collector.collect(0, wrapper);
            }
        };
        try {
            var first = executor.submit(collect);
            Call root = calls.poll(2, java.util.concurrent.TimeUnit.SECONDS);
            Assertions.assertNotNull(root);
            reply(root.callback(), ResponseCode.SUCCESS, "{\"topicConfigTable\":{\"orders\":{}}}".getBytes(StandardCharsets.UTF_8));
            Call stats = calls.poll(2, java.util.concurrent.TimeUnit.SECONDS);
            Call groups = calls.poll(2, java.util.concurrent.TimeUnit.SECONDS);
            Assertions.assertNotNull(stats);
            Assertions.assertNotNull(groups);
            Assertions.assertEquals(RequestCode.GET_TOPIC_STATS_INFO, stats.request().getCode());
            reply(stats.callback(), ResponseCode.SUCCESS,
                RemotingSerializable.encode(new org.apache.rocketmq.remoting.protocol.admin.TopicStatsTable()));
            var current = (org.apache.eventmesh.dashboard.console.function.report.collect.RestoreData)
                org.apache.commons.lang3.reflect.FieldUtils.readField(collector, "current", true);
            Assertions.assertEquals(1, current.getDataMap().get(org.apache.eventmesh.dashboard.console.function.report.model.rocketmq.Rocketmq2ProducerOffset.class).size(),
                "Completed metrics must reach setData before the remaining callback finishes");
            Assertions.assertFalse(first.isDone());
            first.get(6, java.util.concurrent.TimeUnit.SECONDS);
            Assertions.assertEquals(1, current.getDataMap().get(org.apache.eventmesh.dashboard.console.function.report.model.rocketmq.Rocketmq2ProducerOffset.class).size(),
                "Timeout retains samples already delivered through setData");
            var second = executor.submit(collect);
            Call nextRoot = calls.poll(2, java.util.concurrent.TimeUnit.SECONDS);
            Assertions.assertNotNull(nextRoot);
            reply(groups.callback(), ResponseCode.SUCCESS, "{\"groupList\":[\"late-group\"]}".getBytes(StandardCharsets.UTF_8));
            Assertions.assertTrue(calls.isEmpty(), "Previous round must not dispatch requests into a new round");
            reply(nextRoot.callback(), ResponseCode.SUCCESS, "{\"topicConfigTable\":{}}".getBytes(StandardCharsets.UTF_8));
            second.get(2, java.util.concurrent.TimeUnit.SECONDS);
            var capture = org.mockito.ArgumentCaptor.forClass(org.apache.eventmesh.dashboard.console.function.report.collect.RestoreData.class);
            Mockito.verify(wrapper, Mockito.times(2)).sync(capture.capture());
            Assertions.assertEquals(0L, rows(capture.getAllValues().get(1).getDataMap(), RocketmqTopicNumber.class).get(0).getValue());
        } finally {
            executor.shutdownNow();
        }
    }

    /** Offset regressions deliberately keep their callback queues independent of the new Broker roots. */
    private boolean failBrokerRoot(RemotingCommand request, InvokeCallback callback) {
        if (request.getCode() == RequestCode.GET_BROKER_CONFIG || request.getCode() == RequestCode.GET_BROKER_RUNTIME_INFO
            || request.getCode() == RequestCode.GET_ALL_SUBSCRIPTIONGROUP_CONFIG
            || request.getCode() == RequestCode.VIEW_BROKER_STATS_DATA
            || request.getCode() == RequestCode.QUERY_CONSUME_TIME_SPAN) {
            callback.operationFail(new IllegalStateException("Broker roots disabled in offset-only regression"));
            return true;
        }
        return false;
    }

    private void reply(InvokeCallback callback, int code, byte[] body) {
        RemotingCommand response = RemotingCommand.createResponseCommand(code, "test response");
        response.setBody(body);
        ResponseFuture future = Mockito.mock(ResponseFuture.class);
        Mockito.when(future.getResponseCommand()).thenReturn(response);
        callback.operationComplete(future);
    }

    private record Call(RemotingCommand request, InvokeCallback callback) {
    }

    private RocketMQCollect collector(RuntimeMetadata runtime) {
        var cluster = new org.apache.eventmesh.dashboard.common.model.metadata.ClusterMetadata();
        cluster.setId(runtime.getClusterId());
        cluster.setOrganizationId(7L);
        cluster.setClusterType(runtime.getClusterType());
        RocketMQCollect collector = new RocketMQCollect();
        collector.setRuntimeMetadata(runtime);
        collector.setClusterMetadata(cluster);
        return collector;
    }

    @Test
    public void sendFailureFinishesRoundWithoutWaitingForCallback() throws Exception {
        DefaultRemotingClient client = Mockito.mock(DefaultRemotingClient.class);
        SDKManage sdk = Mockito.mock(SDKManage.class);
        RuntimeMetadata runtime = runtime();
        RocketMQCollect collector = collector(runtime);
        Mockito.when(sdk.getClient(SDKTypeEnum.ADMIN, runtime.getUnique())).thenReturn(client);
        Mockito.doThrow(new IllegalStateException("send failed"))
            .when(client).invokeAsync(Mockito.any(), Mockito.anyLong(), Mockito.any());
        var wrapper = Mockito.mock(org.apache.eventmesh.dashboard.console.function.report.collect.DataSyncHandler.DataSyncHandlerWrapper.class);
        try (var mocked = Mockito.mockStatic(SDKManage.class)) {
            mocked.when(SDKManage::getInstance).thenReturn(sdk);
            Assertions.assertTimeout(java.time.Duration.ofSeconds(1), () -> collector.collect(0, wrapper));
            var capture = org.mockito.ArgumentCaptor.forClass(org.apache.eventmesh.dashboard.console.function.report.collect.RestoreData.class);
            Mockito.verify(wrapper).sync(capture.capture());
            Assertions.assertTrue(capture.getValue().getDataMap().isEmpty());
        }
    }

    @Test
    public void interruptedSendRestoresInterruptFlag() throws Exception {
        DefaultRemotingClient client = Mockito.mock(DefaultRemotingClient.class);
        SDKManage sdk = Mockito.mock(SDKManage.class);
        RuntimeMetadata runtime = runtime();
        RocketMQCollect collector = collector(runtime);
        Mockito.when(sdk.getClient(SDKTypeEnum.ADMIN, runtime.getUnique())).thenReturn(client);
        Mockito.doThrow(new InterruptedException("interrupted"))
            .when(client).invokeAsync(Mockito.any(), Mockito.anyLong(), Mockito.any());
        var wrapper = Mockito.mock(org.apache.eventmesh.dashboard.console.function.report.collect.DataSyncHandler.DataSyncHandlerWrapper.class);
        try (var mocked = Mockito.mockStatic(SDKManage.class)) {
            mocked.when(SDKManage::getInstance).thenReturn(sdk);
            collector.collect(0, wrapper);
            Assertions.assertTrue(Thread.currentThread().isInterrupted());
            Mockito.verify(wrapper).sync(Mockito.any());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    public void mapsBrokerRatesAndRuntimeValuesWithoutLosingLongPrecision() {
        long exact = 9007199254740993L;
        var incoming = RocketMQCollectMapper.INSTANCE.brokerMessagesIn("minute", exact, 1.25F);
        Assertions.assertEquals("minute", incoming.getWindow());
        Assertions.assertEquals(exact, incoming.getValueWindowCount());
        Assertions.assertEquals(Float.valueOf(1.25F), incoming.getValue());
        var outgoing = RocketMQCollectMapper.INSTANCE.brokerMessagesOut("day", Long.MAX_VALUE, 0F);
        Assertions.assertEquals("day", outgoing.getWindow());
        Assertions.assertEquals(Long.MAX_VALUE, outgoing.getValueWindowCount());
        Assertions.assertEquals(Float.valueOf(0F), outgoing.getValue());
        Assertions.assertEquals(exact, RocketMQCollectMapper.INSTANCE.dispatchBytes(exact).getValue());
        Assertions.assertEquals(exact, RocketMQCollectMapper.INSTANCE.flushBytes(exact).getValue());
        Assertions.assertEquals(exact, RocketMQCollectMapper.INSTANCE.reserveTime(exact).getValue());
        var pool = RocketMQCollectMapper.INSTANCE.threadPool("endTransaction", exact);
        Assertions.assertEquals("endTransaction", pool.getPoolName());
        Assertions.assertEquals(exact, pool.getValue());
    }

    @Test
    public void brokerModelsDescribeWindowPoolAndNumericColumnTypes() throws Exception {
        assertReportTable(RocketmqBrokerMessagesIn.class, "rocketmq_broker_messages_in", List.of("window"),
            List.of("value_window_count"), List.of("value"));
        assertReportTable(RocketmqBrokerMessagesOut.class, "rocketmq_broker_messages_out", List.of("window"),
            List.of("value_window_count"), List.of("value"));
        assertReportTable(RocketmqStorageDispatchBehindBytes.class, "rocketmq_storage_dispatch_behind_bytes",
            List.of(), List.of("value"));
        assertReportTable(RocketmqStorageFlushBehindBytes.class, "rocketmq_storage_flush_behind_bytes", List.of(), List.of("value"));
        assertReportTable(RocketmqStorageMessageReserveTime.class, "rocketmq_storage_message_reserve_time", List.of(), List.of("value"));
        assertReportTable(RocketmqThreadPoolWartermark.class, "rocketmq_thread_pool_wartermark", List.of("pool_name"), List.of("value"));
        Assertions.assertNotNull(RocketmqBrokerMessagesIn.class.getDeclaredField("window").getAnnotation(ReportTag.class));
        Assertions.assertNotNull(RocketmqBrokerMessagesOut.class.getDeclaredField("window").getAnnotation(ReportTag.class));
        Assertions.assertNotNull(RocketmqThreadPoolWartermark.class.getDeclaredField("poolName").getAnnotation(ReportTag.class));
        Assertions.assertTrue(RuntimeFloatValue.class.isAssignableFrom(RocketmqBrokerMessagesIn.class));
        Assertions.assertTrue(RuntimeFloatValue.class.isAssignableFrom(RocketmqBrokerMessagesOut.class));
        Assertions.assertTrue(RuntimeLongValue.class.isAssignableFrom(RocketmqStorageMessageReserveTime.class));
    }

    @Test
    public void registersAllRootsBeforeSynchronousFailureAndWaitsForNestedBrokerStats() throws Exception {
        DefaultRemotingClient client = Mockito.mock(DefaultRemotingClient.class);
        SDKManage sdk = Mockito.mock(SDKManage.class);
        RuntimeMetadata runtime = runtime();
        Mockito.when(sdk.getClient(SDKTypeEnum.ADMIN, runtime.getUnique())).thenReturn(client);
        var calls = new java.util.concurrent.LinkedBlockingQueue<Call>();
        var sent = new java.util.concurrent.CountDownLatch(3);
        AtomicInteger topicRoots = new AtomicInteger();
        AtomicInteger configRoots = new AtomicInteger();
        Mockito.doAnswer(invocation -> {
            RemotingCommand request = invocation.getArgument(0);
            InvokeCallback callback = invocation.getArgument(2);
            long timeout = invocation.getArgument(1);
            Assertions.assertTrue(timeout > 0 && timeout <= 3000);
            if (request.getCode() == RequestCode.GET_ALL_TOPIC_CONFIG) {
                topicRoots.incrementAndGet();
                callback.operationFail(new IllegalStateException("first root fails inline"));
            } else if (request.getCode() == RequestCode.GET_ALL_SUBSCRIPTIONGROUP_CONFIG) {
                callback.operationFail(new IllegalStateException("Group count unavailable"));
            } else if (request.getCode() == RequestCode.GET_BROKER_CONFIG) {
                configRoots.incrementAndGet();
                reply(callback, ResponseCode.SUCCESS, "brokerClusterName=cluster-A\nbrokerName=wrong-key\n".getBytes(StandardCharsets.UTF_8));
            } else {
                calls.add(new Call(request, callback));
                sent.countDown();
            }
            return null;
        }).when(client).invokeAsync(Mockito.any(), Mockito.anyLong(), Mockito.any());
        var wrapper = Mockito.mock(org.apache.eventmesh.dashboard.console.function.report.collect.DataSyncHandler.DataSyncHandlerWrapper.class);
        var executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        try {
            var finished = executor.submit(() -> {
                try (var mocked = Mockito.mockStatic(SDKManage.class)) {
                    mocked.when(SDKManage::getInstance).thenReturn(sdk);
                    collector(runtime).collect(0, wrapper);
                }
            });
            Assertions.assertTrue(sent.await(2, java.util.concurrent.TimeUnit.SECONDS));
            Assertions.assertEquals(1, topicRoots.get());
            Assertions.assertEquals(1, configRoots.get());
            Assertions.assertEquals(3, calls.size());
            Call runtimeCall = calls.stream().filter(call -> call.request().getCode() == RequestCode.GET_BROKER_RUNTIME_INFO)
                .findFirst().orElseThrow();
            List<Call> stats = calls.stream().filter(call -> call.request().getCode() == RequestCode.VIEW_BROKER_STATS_DATA).toList();
            Assertions.assertEquals(2, stats.size());
            Assertions.assertThrows(java.util.concurrent.TimeoutException.class,
                () -> finished.get(100, java.util.concurrent.TimeUnit.MILLISECONDS));
            Mockito.verifyNoInteractions(wrapper);
            reply(runtimeCall.callback(), ResponseCode.SUCCESS, runtimeBody(Map.of("dispatchBehindBytes", "17")));
            reply(stats.get(0).callback(), ResponseCode.SUCCESS, "{\"statsMinute\":{\"sum\":0,\"tps\":0}}".getBytes(StandardCharsets.UTF_8));
            stats.get(0).callback().operationFail(new IllegalStateException("duplicate stats completion"));
            Assertions.assertThrows(java.util.concurrent.TimeoutException.class,
                () -> finished.get(100, java.util.concurrent.TimeUnit.MILLISECONDS));
            Mockito.verifyNoInteractions(wrapper);
            reply(stats.get(1).callback(), ResponseCode.SUCCESS, "{\"statsMinute\":{\"sum\":2,\"tps\":0.5}}".getBytes(StandardCharsets.UTF_8));
            finished.get(2, java.util.concurrent.TimeUnit.SECONDS);
            var capture = org.mockito.ArgumentCaptor.forClass(org.apache.eventmesh.dashboard.console.function.report.collect.RestoreData.class);
            Mockito.verify(wrapper).sync(capture.capture());
            var models = capture.getValue().getDataMap();
            Assertions.assertEquals(Set.of(RocketmqBrokerMessagesIn.class, RocketmqBrokerMessagesOut.class,
                RocketmqStorageDispatchBehindBytes.class), models.keySet());
            Assertions.assertEquals(17L, rows(models, RocketmqStorageDispatchBehindBytes.class).get(0).getValue());
            Assertions.assertEquals(1, rows(models, RocketmqBrokerMessagesIn.class).size());
            Assertions.assertEquals(1, rows(models, RocketmqBrokerMessagesOut.class).size());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void brokerStatsUseNativeClusterKeyAndKeepZeroWindowsAndExactCounts() throws Exception {
        Set<String> names = new java.util.HashSet<>();
        var models = collectMock((request, callback) -> {
            switch (request.getCode()) {
                case RequestCode.GET_BROKER_CONFIG:
                    reply(callback, ResponseCode.SUCCESS,
                        "brokerClusterName=集群-A\nbrokerName=not-the-cluster\n".getBytes(StandardCharsets.UTF_8));
                    break;
                case RequestCode.VIEW_BROKER_STATS_DATA:
                    var header = (ViewBrokerStatsDataRequestHeader) request.readCustomHeader();
                    Assertions.assertEquals("集群-A", header.getStatsKey());
                    Assertions.assertTrue(names.add(header.getStatsName()), "Only one request per Broker statistic");
                    String body = "BROKER_PUT_NUMS".equals(header.getStatsName())
                        ? "{\"statsMinute\":{\"sum\":9007199254740993,\"tps\":1.25},\"statsHour\":{\"sum\":0,\"tps\":0}}"
                        : "{\"statsDay\":{\"sum\":9223372036854775807,\"tps\":0.5}}";
                    reply(callback, ResponseCode.SUCCESS, body.getBytes(StandardCharsets.UTF_8));
                    break;
                default:
                    callback.operationFail(new IllegalStateException("Other collection direction unavailable"));
            }
        });
        Assertions.assertEquals(Set.of("BROKER_PUT_NUMS", "BROKER_GET_NUMS"), names);
        Assertions.assertEquals(Set.of(RocketmqBrokerMessagesIn.class, RocketmqBrokerMessagesOut.class), models.keySet());
        var incoming = rows(models, RocketmqBrokerMessagesIn.class);
        Assertions.assertEquals(2, incoming.size());
        var minute = incoming.stream().filter(row -> "minute".equals(row.getWindow())).findFirst().orElseThrow();
        Assertions.assertEquals(9007199254740993L, minute.getValueWindowCount());
        Assertions.assertEquals(Float.valueOf(1.25F), minute.getValue());
        var hour = incoming.stream().filter(row -> "hour".equals(row.getWindow())).findFirst().orElseThrow();
        Assertions.assertEquals(0L, hour.getValueWindowCount());
        Assertions.assertEquals(Float.valueOf(0F), hour.getValue());
        var outgoing = rows(models, RocketmqBrokerMessagesOut.class);
        Assertions.assertEquals(1, outgoing.size());
        Assertions.assertEquals("day", outgoing.get(0).getWindow());
        Assertions.assertEquals(Long.MAX_VALUE, outgoing.get(0).getValueWindowCount());
        Assertions.assertEquals(Float.valueOf(0.5F), outgoing.get(0).getValue());
    }

    @Test
    public void invalidBrokerWindowsDoNotEraseValidWindowsOrInventZeros() throws Exception {
        List<String> invalid = List.of("null", "{}", "[]", "\"invalid\"", "{\"sum\":1}", "{\"tps\":1}",
            "{\"sum\":-1,\"tps\":1}", "{\"sum\":1.5,\"tps\":1}", "{\"sum\":9223372036854775808,\"tps\":1}",
            "{\"sum\":\"1\",\"tps\":1}", "{\"sum\":null,\"tps\":1}", "{\"sum\":true,\"tps\":1}",
            "{\"sum\":1,\"tps\":-1}", "{\"sum\":1,\"tps\":3.5e38}", "{\"sum\":1,\"tps\":1e309}",
            "{\"sum\":1,\"tps\":\"NaN\"}", "{\"sum\":1,\"tps\":\"Infinity\"}",
            "{\"sum\":1,\"tps\":\"0\"}", "{\"sum\":1,\"tps\":null}");
        for (String window : invalid) {
            var models = collectStatsBody("{\"statsMinute\":" + window
                + ",\"statsHour\":{\"sum\":5,\"tps\":2.5},\"statsDay\":{\"sum\":0,\"tps\":0}}");
            Assertions.assertEquals(Set.of(RocketmqBrokerMessagesIn.class, RocketmqBrokerMessagesOut.class), models.keySet(), window);
            for (Class<?> type : List.of(RocketmqBrokerMessagesIn.class, RocketmqBrokerMessagesOut.class)) {
                List<Object> samples = models.get(type);
                Assertions.assertEquals(2, samples.size(), window);
                Map<String, Long> counts = new java.util.HashMap<>();
                for (Object row : samples) {
                    String name = brokerWindow(row);
                    counts.put(name, brokerWindowCount(row));
                    Assertions.assertEquals(Float.valueOf("hour".equals(name) ? 2.5F : 0F), ((RuntimeFloatValue) row).getValue(), window);
                }
                Assertions.assertEquals(Map.of("hour", 5L, "day", 0L), counts, window);
            }
        }
        Assertions.assertTrue(collectStatsBody("{}").isEmpty(), "Missing windows are not zero-valued observations");
    }

    @Test
    public void unavailableBrokerStatsAndClusterNameNeverProduceOffsetDerivedRates() throws Exception {
        for (String config : List.of("", "brokerName=not-a-cluster\n", "brokerClusterName=   \n")) {
            AtomicInteger statsRequests = new AtomicInteger();
            var models = collectMock((request, callback) -> {
                if (request.getCode() == RequestCode.GET_BROKER_CONFIG) {
                    reply(callback, ResponseCode.SUCCESS, config.getBytes(StandardCharsets.UTF_8));
                } else if (request.getCode() == RequestCode.GET_BROKER_RUNTIME_INFO) {
                    reply(callback, ResponseCode.SUCCESS, runtimeBody(Map.of("dispatchBehindBytes", "3")));
                } else if (request.getCode() == RequestCode.VIEW_BROKER_STATS_DATA) {
                    statsRequests.incrementAndGet();
                    callback.operationFail(new IllegalStateException("Must not guess a Broker stats key"));
                } else {
                    callback.operationFail(new IllegalStateException("Topic unavailable"));
                }
            });
            Assertions.assertEquals(0, statsRequests.get(), config);
            Assertions.assertEquals(Set.of(RocketmqStorageDispatchBehindBytes.class), models.keySet(), config);
        }
        for (byte[] body : new byte[][] {null, "".getBytes(StandardCharsets.UTF_8), "bad-json".getBytes(StandardCharsets.UTF_8),
            "[]".getBytes(StandardCharsets.UTF_8)}) {
            Assertions.assertTrue(collectMock((request, callback) -> {
                if (request.getCode() == RequestCode.GET_BROKER_CONFIG) {
                    reply(callback, ResponseCode.SUCCESS, "brokerClusterName=cluster-A\n".getBytes(StandardCharsets.UTF_8));
                } else if (request.getCode() == RequestCode.VIEW_BROKER_STATS_DATA) {
                    reply(callback, ResponseCode.SUCCESS, body);
                } else {
                    callback.operationFail(new IllegalStateException("Other root unavailable"));
                }
            }).isEmpty(), "Malformed Broker responses must not produce default windows");
        }
        var models = collectMock((request, callback) -> {
            switch (request.getCode()) {
                case RequestCode.GET_BROKER_CONFIG:
                    reply(callback, ResponseCode.SUCCESS, "brokerClusterName=cluster-A\n".getBytes(StandardCharsets.UTF_8));
                    break;
                case RequestCode.VIEW_BROKER_STATS_DATA:
                    reply(callback, ResponseCode.SYSTEM_ERROR, "{\"statsMinute\":{\"sum\":99,\"tps\":99}}".getBytes(StandardCharsets.UTF_8));
                    break;
                case RequestCode.GET_ALL_TOPIC_CONFIG:
                    reply(callback, ResponseCode.SUCCESS, "{\"topicConfigTable\":{\"orders\":{}}}".getBytes(StandardCharsets.UTF_8));
                    break;
                case RequestCode.GET_TOPIC_STATS_INFO:
                    var stats = new org.apache.rocketmq.remoting.protocol.admin.TopicStatsTable();
                    var offset = new org.apache.rocketmq.remoting.protocol.admin.TopicOffset();
                    offset.setMinOffset(100);
                    offset.setMaxOffset(999);
                    stats.getOffsetTable().put(new org.apache.rocketmq.common.message.MessageQueue("orders", "broker", 0), offset);
                    reply(callback, ResponseCode.SUCCESS, RemotingSerializable.encode(stats));
                    break;
                default:
                    callback.operationFail(new IllegalStateException("No runtime or consumers"));
            }
        });
        Assertions.assertEquals(Set.of(org.apache.eventmesh.dashboard.console.function.report.model.rocketmq.Rocketmq2ProducerOffset.class,
            RocketmqTopicNumber.class), models.keySet(), "Topic positions must never become Broker message counts or rates");
        var offsets = rows(models, org.apache.eventmesh.dashboard.console.function.report.model.rocketmq.Rocketmq2ProducerOffset.class);
        Assertions.assertEquals(2, offsets.size());
        Assertions.assertTrue(offsets.stream().anyMatch(row -> Long.valueOf(999L).equals(row.getValueMaxOffsetSum())));
    }

    @Test
    public void runtimeUsesNativeKeysAndPreservesExactBytesAndAllPoolDimensions() throws Exception {
        long earliest = System.currentTimeMillis() - 60000;
        long before = System.currentTimeMillis();
        var values = new java.util.HashMap<String, String>();
        values.put("dispatchBehindBytes", "9007199254740993");
        values.put("remainHowManyDataToFlush", " 1.5 KiB ");
        values.put("remainHowManyDataToCommit", "0.5KiB");
        values.put("earliestMessageTimeStamp", Long.toString(earliest));
        values.put("sendThreadPoolQueueSize", "0");
        values.put("pullThreadPoolQueueSize", "1");
        values.put("litePullThreadPoolQueueSize", "2");
        values.put("queryThreadPoolQueueSize", "3");
        values.put("ackThreadPoolQueueSize", "4");
        values.put("EndTransactionQueueSize", "9007199254740993");
        values.put("putTps", "123 456 789");
        values.put("getTps", "123 456 789");
        values.put("putMessageTimesTotal", "1000");
        var models = collectRuntime(values);
        long after = System.currentTimeMillis();
        Assertions.assertEquals(Set.of(RocketmqStorageDispatchBehindBytes.class, RocketmqStorageFlushBehindBytes.class,
            RocketmqStorageMessageReserveTime.class, RocketmqThreadPoolWartermark.class), models.keySet());
        Assertions.assertEquals(9007199254740993L, rows(models, RocketmqStorageDispatchBehindBytes.class).get(0).getValue());
        Assertions.assertEquals(2048L, rows(models, RocketmqStorageFlushBehindBytes.class).get(0).getValue());
        long age = rows(models, RocketmqStorageMessageReserveTime.class).get(0).getValue();
        Assertions.assertTrue(age >= before - earliest && age <= after - earliest, "Reserve time is age in milliseconds");
        Map<String, Long> pools = new java.util.HashMap<>();
        rows(models, RocketmqThreadPoolWartermark.class).forEach(row -> {
            Assertions.assertNull(pools.put(row.getPoolName(), row.getValue()), "One sample per native pool");
            Assertions.assertEquals(7L, row.getOrganizationId());
            Assertions.assertEquals(1L, row.getClustersId());
            Assertions.assertNotNull(row.getRuntimeId());
            Assertions.assertNotNull(row.getTime());
        });
        Assertions.assertEquals(Map.of("send", 0L, "pull", 1L, "litePull", 2L, "query", 3L, "ack", 4L,
            "endTransaction", 9007199254740993L), pools);
    }

    @Test
    public void flushBytesParseNativeUnitsRoundAndRejectOverflowOrMalformedCommit() throws Exception {
        Map<String, Long> accepted = Map.ofEntries(Map.entry("0 B", 0L), Map.entry("0.5 B", 1L),
            Map.entry("9007199254740993 B", 9007199254740993L), Map.entry("9223372036854775807 B", Long.MAX_VALUE),
            Map.entry(" 1.5 KiB ", 1536L), Map.entry("1.5MiB", 1572864L), Map.entry("1 GiB", 1073741824L),
            Map.entry("1 TiB", 1099511627776L), Map.entry("1 PiB", 1125899906842624L));
        for (var entry : accepted.entrySet()) {
            var models = collectRuntime(Map.of("remainHowManyDataToFlush", entry.getKey()));
            Assertions.assertEquals(Set.of(RocketmqStorageFlushBehindBytes.class), models.keySet(), entry.getKey());
            Assertions.assertEquals(entry.getValue(), rows(models, RocketmqStorageFlushBehindBytes.class).get(0).getValue(), entry.getKey());
        }
        var summed = collectRuntime(Map.of("remainHowManyDataToFlush", "9007199254740993 B", "remainHowManyDataToCommit", "2 B"));
        Assertions.assertEquals(9007199254740995L, rows(summed, RocketmqStorageFlushBehindBytes.class).get(0).getValue());
        List<Map<String, String>> invalid = List.of(
            Map.of("remainHowManyDataToFlush", "9223372036854775807 B", "remainHowManyDataToCommit", "1 B"),
            Map.of("remainHowManyDataToFlush", "9223372036854775808 B"), Map.of("remainHowManyDataToFlush", "8192 PiB"),
            Map.of("remainHowManyDataToFlush", "-1 B"), Map.of("remainHowManyDataToFlush", "NaN B"),
            Map.of("remainHowManyDataToFlush", ""), Map.of("remainHowManyDataToFlush", "nonsense"),
            Map.of("remainHowManyDataToFlush", "1 B", "remainHowManyDataToCommit", "bad"),
            Map.of("remainHowManyDataToFlush", "1 B", "remainHowManyDataToCommit", ""),
            Map.of("remainHowManyDataToFlush", "1 B", "remainHowManyDataToCommit", "-1 B"),
            Map.of("remainHowManyDataToCommit", "1 B"));
        for (var fields : invalid) {
            var values = new java.util.HashMap<>(fields);
            values.put("dispatchBehindBytes", "5");
            var models = collectRuntime(values);
            Assertions.assertEquals(Set.of(RocketmqStorageDispatchBehindBytes.class), models.keySet(), fields.toString());
            Assertions.assertEquals(5L, rows(models, RocketmqStorageDispatchBehindBytes.class).get(0).getValue());
        }
    }

    @Test
    public void malformedRuntimeFieldsDoNotEraseOtherMeasurementsOrUseFallbacks() throws Exception {
        for (String invalid : List.of("-1", "1.5", "NaN", "9223372036854775808", "")) {
            var models = collectRuntime(Map.of("dispatchBehindBytes", invalid, "sendThreadPoolQueueSize", invalid,
                "remainHowManyDataToFlush", "2 B", "queryThreadPoolQueueSize", "7",
                "earliestMessageTimeStamp", Long.toString(System.currentTimeMillis() + 3600000),
                "putTps", "1 2 3", "getTps", "4 5 6"));
            Assertions.assertEquals(Set.of(RocketmqStorageFlushBehindBytes.class, RocketmqThreadPoolWartermark.class),
                models.keySet(), invalid);
            Assertions.assertEquals(2L, rows(models, RocketmqStorageFlushBehindBytes.class).get(0).getValue());
            var pools = rows(models, RocketmqThreadPoolWartermark.class);
            Assertions.assertEquals(1, pools.size());
            Assertions.assertEquals("query", pools.get(0).getPoolName());
            Assertions.assertEquals(7L, pools.get(0).getValue());
        }
        for (String earliest : List.of("0", "-1", "bad", "1.5", "9223372036854775808")) {
            Assertions.assertTrue(collectRuntime(Map.of("earliestMessageTimeStamp", earliest)).isEmpty(), earliest);
        }
        Assertions.assertTrue(collectRuntime(Map.of("putTps", "1 2 3", "getTps", "4 5 6",
            "dispatchBehind", "9", "flushBehindBytes", "9", "endTransactionThreadPoolQueueSize", "9")).isEmpty(),
            "Unknown keys and runtime throughput cannot substitute for native Broker statistics");
    }

    @Test
    public void filtersInvalidLagTimestampsAndRetainsZeroBacklog() throws Exception {
        long now = System.currentTimeMillis();
        for (long delay : new long[] {0, 100, -1, now + 1}) {
            var models = collectMock((request, callback) -> {
                switch (request.getCode()) {
                    case RequestCode.GET_ALL_TOPIC_CONFIG -> reply(callback, ResponseCode.SUCCESS,
                        "{\"topicConfigTable\":{\"orders\":{}}}".getBytes(StandardCharsets.UTF_8));
                    case RequestCode.QUERY_TOPIC_CONSUME_BY_WHO -> reply(callback, ResponseCode.SUCCESS,
                        "{\"groupList\":[\"buyers\"]}".getBytes(StandardCharsets.UTF_8));
                    case RequestCode.GET_CONSUME_STATS -> {
                        var stats = new org.apache.rocketmq.remoting.protocol.admin.ConsumeStats();
                        var offset = new org.apache.rocketmq.remoting.protocol.admin.OffsetWrapper();
                        offset.setBrokerOffset(3L);
                        offset.setConsumerOffset(2L);
                        stats.getOffsetTable().put(new org.apache.rocketmq.common.message.MessageQueue("orders", "broker", 0), offset);
                        reply(callback, ResponseCode.SUCCESS, RemotingSerializable.encode(stats));
                    }
                    case RequestCode.QUERY_CONSUME_TIME_SPAN -> {
                        byte[] body = RemotingSerializable.encode(Map.of("consumeTimeSpanSet", List.of(
                            Map.of("messageQueue", Map.of("topic", "orders", "queueId", 0), "delayTime", delay, "minTimeStamp", now - 1000),
                            Map.of("messageQueue", Map.of("topic", "orders", "queueId", 1), "delayTime", 0, "minTimeStamp", -1),
                            Map.of("messageQueue", Map.of("topic", "another", "queueId", 0), "delayTime", 0, "minTimeStamp", now - 1000))));
                        reply(callback, ResponseCode.SUCCESS, body);
                        reply(callback, ResponseCode.SUCCESS, body);
                    }
                    default -> callback.operationFail(new IllegalStateException("Other requests unavailable"));
                }
            });
            if (delay >= 0 && delay <= 100) {
                var rows = rows(models, RocketmqConsumerLagLatency.class);
                Assertions.assertEquals(1, rows.size(), "Duplicate callbacks must not duplicate samples");
                Assertions.assertEquals(delay, rows.get(0).getValue());
                Assertions.assertEquals("buyers", rows.get(0).getGroupName());
                Assertions.assertEquals("0", rows.get(0).getQueueId());
            } else {
                Assertions.assertFalse(models.containsKey(RocketmqConsumerLagLatency.class));
            }
        }
    }

    @Test
    public void collectsDiskRatiosAndApproximateFreeBytesWithoutInventingMissingValues() throws Exception {
        var models = collectRuntime(Map.of("commitLogDiskRatio", "0.75", "commitLogDiskRatio_/store/a", "0.75",
            "consumeQueueDiskRatio", "0", "commitLogDirCapacity", "Total : 10 GiB, Free : 2.5 GiB."));
        Assertions.assertEquals(3, rows(models, RocketmqStorageDiskUsage.class).size());
        Assertions.assertEquals(2684354560L, rows(models, RocketmqStorageDiskFreeBytes.class).get(0).getValue());
        var path = rows(models, RocketmqStorageDiskUsage.class).stream().filter(row -> row.getStoreType().equals("commitlog"))
            .findFirst().orElseThrow();
        Assertions.assertEquals("/store/a", path.getPath());
        Assertions.assertEquals(0.75f, path.getValue());
        for (String value : List.of("-1", "NaN", "Infinity", "1.5", "missing")) {
            Assertions.assertTrue(collectRuntime(Map.of("commitLogDiskRatio", value)).isEmpty());
        }
        for (String value : List.of("Total : 1 GiB, Free : 2 GiB.", "Total : 0 B, Free : 0 B.", "unknown")) {
            Assertions.assertTrue(collectRuntime(Map.of("commitLogDirCapacity", value)).isEmpty());
        }
        Assertions.assertEquals(0L, rows(collectRuntime(Map.of("commitLogDirCapacity", "Total : 1 GiB, Free : 0 B.")),
            RocketmqStorageDiskFreeBytes.class).get(0).getValue());
    }

    @Test
    public void collectsClientMetricsPerClientAndSkipsUnavailableValues() throws Exception {
        for (boolean available : List.of(true, false)) {
            AtomicInteger requests = new AtomicInteger();
            var models = collectMock((request, callback) -> {
                switch (request.getCode()) {
                    case RequestCode.GET_ALL_TOPIC_CONFIG -> reply(callback, ResponseCode.SUCCESS,
                        "{\"topicConfigTable\":{\"orders\":{},\"payments\":{}}}".getBytes(StandardCharsets.UTF_8));
                    case RequestCode.QUERY_TOPIC_CONSUME_BY_WHO -> reply(callback, ResponseCode.SUCCESS,
                        "{\"groupList\":[\"buyers\"]}".getBytes(StandardCharsets.UTF_8));
                    case RequestCode.GET_CONSUMER_CONNECTION_LIST -> reply(callback, ResponseCode.SUCCESS,
                        "{\"connectionSet\":[{\"clientId\":\"a\"},{\"clientId\":\"b\"}]}".getBytes(StandardCharsets.UTF_8));
                    case RequestCode.GET_CONSUMER_RUNNING_INFO -> {
                        requests.incrementAndGet();
                        if (available) {
                            reply(callback, ResponseCode.SUCCESS, RemotingSerializable.encode(Map.of("statusTable", Map.of(
                                "orders", Map.of("consumeOKTPS", 2.5, "consumeFailedTPS", 0, "consumeRT", 12.25),
                                "payments", Map.of("consumeOKTPS", -1, "consumeRT", "NaN")))));
                        } else {
                            callback.operationFail(new IllegalStateException("Client disconnected"));
                            callback.operationFail(new IllegalStateException("Duplicate failure notification"));
                        }
                    }
                    default -> callback.operationFail(new IllegalStateException("Not needed for this case"));
                }
            });
            Assertions.assertEquals(2, requests.get(), "Shared group is queried once; each client has one request");
            if (available) {
                var success = rows(models, RocketmqConsumerSuccessTps.class);
                Assertions.assertEquals(Set.of("a", "b"), success.stream().map(RocketmqConsumerSuccessTps::getClientId)
                    .collect(java.util.stream.Collectors.toSet()));
                Assertions.assertTrue(success.stream().allMatch(row -> row.getValue() == 2.5f && row.getTopicName().equals("orders")));
                Assertions.assertEquals(2, rows(models, RocketmqConsumerFailedTps.class).size());
                Assertions.assertEquals(0f, rows(models, RocketmqConsumerFailedTps.class).get(0).getValue());
                Assertions.assertEquals(12.25f, rows(models, RocketmqConsumerProcessTime.class).get(0).getValue());
            } else {
                Assertions.assertFalse(models.containsKey(RocketmqConsumerSuccessTps.class));
            }
        }
    }

    private Map<Class<?>, List<Object>> collectStatsBody(String body) throws Exception {
        return collectMock((request, callback) -> {
            if (request.getCode() == RequestCode.GET_BROKER_CONFIG) {
                reply(callback, ResponseCode.SUCCESS, "brokerClusterName=cluster-A\n".getBytes(StandardCharsets.UTF_8));
            } else if (request.getCode() == RequestCode.VIEW_BROKER_STATS_DATA) {
                reply(callback, ResponseCode.SUCCESS, body.getBytes(StandardCharsets.UTF_8));
            } else {
                callback.operationFail(new IllegalStateException("Other root unavailable"));
            }
        });
    }

    private Map<Class<?>, List<Object>> collectRuntime(Map<String, String> values) throws Exception {
        return collectMock((request, callback) -> {
            if (request.getCode() == RequestCode.GET_BROKER_RUNTIME_INFO) {
                reply(callback, ResponseCode.SUCCESS, runtimeBody(values));
            } else {
                callback.operationFail(new IllegalStateException("Other root unavailable"));
            }
        });
    }

    private byte[] runtimeBody(Map<String, String> values) {
        KVTable table = new KVTable();
        table.getTable().putAll(values);
        return RemotingSerializable.encode(table);
    }

    private Map<Class<?>, List<Object>> collectMock(java.util.function.BiConsumer<RemotingCommand, InvokeCallback> answer) throws Exception {
        DefaultRemotingClient client = Mockito.mock(DefaultRemotingClient.class);
        SDKManage sdk = Mockito.mock(SDKManage.class);
        RuntimeMetadata runtime = runtime();
        Mockito.when(sdk.getClient(SDKTypeEnum.ADMIN, runtime.getUnique())).thenReturn(client);
        Mockito.doAnswer(invocation -> {
            answer.accept(invocation.getArgument(0), invocation.getArgument(2));
            return null;
        }).when(client).invokeAsync(Mockito.any(), Mockito.anyLong(), Mockito.any());
        var wrapper = Mockito.mock(org.apache.eventmesh.dashboard.console.function.report.collect.DataSyncHandler.DataSyncHandlerWrapper.class);
        try (var mocked = Mockito.mockStatic(SDKManage.class)) {
            mocked.when(SDKManage::getInstance).thenReturn(sdk);
            collector(runtime).collect(0, wrapper);
        }
        var capture = org.mockito.ArgumentCaptor.forClass(org.apache.eventmesh.dashboard.console.function.report.collect.RestoreData.class);
        Mockito.verify(wrapper).sync(capture.capture());
        return capture.getValue().getDataMap();
    }

    private <T> List<T> rows(Map<Class<?>, List<Object>> models, Class<T> type) {
        Assertions.assertTrue(models.containsKey(type), "Missing collected model " + type.getSimpleName());
        return models.get(type).stream().map(type::cast).toList();
    }

    private String brokerWindow(Object row) {
        return row instanceof RocketmqBrokerMessagesIn
            ? ((RocketmqBrokerMessagesIn) row).getWindow() : ((RocketmqBrokerMessagesOut) row).getWindow();
    }

    private Long brokerWindowCount(Object row) {
        return row instanceof RocketmqBrokerMessagesIn
            ? ((RocketmqBrokerMessagesIn) row).getValueWindowCount() : ((RocketmqBrokerMessagesOut) row).getValueWindowCount();
    }

    @Test
    public void collectsFromRealBrokerThroughCallbacks() throws Exception {
        Assumptions.assumeTrue(Boolean.getBoolean("rocketmq.collect.live"));
        collectFromRealBroker(false);
    }

    @Test
    public void collectsFromRealBrokerAndReadsBackIotdbTables() throws Exception {
        Assumptions.assumeTrue(Boolean.getBoolean("rocketmq.collect.iotdb.live"));
        collectFromRealBroker(true);
    }

    private void collectFromRealBroker(boolean verifyIotdb) throws Exception {
        RuntimeMetadata runtime = runtime();
        AbstractSimpleCreateSDKConfig config = ConfigManage.getInstance()
            .getSimpleCreateSdkConfig(runtime.getClusterType(), SDKTypeEnum.ADMIN);
        NetAddress address = new NetAddress();
        address.setAddress("127.0.0.1");
        address.setPort(Integer.getInteger("rocketmq.collect.port", 21911));
        config.setNetAddress(address);
        DefaultRemotingClient real = SDKManage.getInstance().createClient(SDKTypeEnum.ADMIN, runtime, config, runtime.getClusterType());
        DefaultRemotingClient observed = Mockito.mock(DefaultRemotingClient.class);
        AtomicInteger outstanding = new AtomicInteger();
        Set<Integer> successful = ConcurrentHashMap.newKeySet();
        Map<String, byte[]> brokerResponses = new ConcurrentHashMap<>();
        var runtimeBefore = new java.util.concurrent.atomic.AtomicLong();
        var runtimeAfter = new java.util.concurrent.atomic.AtomicLong();
        Mockito.doAnswer(call -> {
            RemotingCommand request = call.getArgument(0);
            InvokeCallback callback = call.getArgument(2);
            outstanding.incrementAndGet();
            try {
                real.invokeAsync(request, call.getArgument(1), new InvokeCallback() {
                    @Override
                    public void operationComplete(ResponseFuture future) {
                        try {
                            if (future.getResponseCommand() != null && future.getResponseCommand().getCode() == ResponseCode.SUCCESS) {
                                successful.add(request.getCode());
                                if (request.getCode() == RequestCode.VIEW_BROKER_STATS_DATA) {
                                    var header = (ViewBrokerStatsDataRequestHeader) request.readCustomHeader();
                                    brokerResponses.put(header.getStatsName() + ":" + header.getStatsKey(), future.getResponseCommand().getBody());
                                    if (header.getStatsName().startsWith("BROKER_")) {
                                        brokerResponses.put(header.getStatsName(), future.getResponseCommand().getBody());
                                    }
                                } else if (request.getCode() == RequestCode.GET_BROKER_CONFIG
                                    || request.getCode() == RequestCode.GET_BROKER_RUNTIME_INFO
                                    || request.getCode() == RequestCode.GET_ALL_TOPIC_CONFIG
                                    || request.getCode() == RequestCode.GET_ALL_SUBSCRIPTIONGROUP_CONFIG) {
                                    brokerResponses.put(Integer.toString(request.getCode()), future.getResponseCommand().getBody());
                                }
                            }
                            if (request.getCode() == RequestCode.GET_BROKER_RUNTIME_INFO) {
                                runtimeBefore.set(System.currentTimeMillis());
                            }
                            callback.operationComplete(future);
                        } finally {
                            if (request.getCode() == RequestCode.GET_BROKER_RUNTIME_INFO) {
                                runtimeAfter.set(System.currentTimeMillis());
                            }
                            outstanding.decrementAndGet();
                        }
                    }

                    @Override
                    public void operationFail(Throwable error) {
                        callback.operationFail(error);
                    }
                });
            } catch (Exception e) {
                outstanding.decrementAndGet();
                throw e;
            }
            return null;
        }).when(observed).invokeAsync(Mockito.any(), Mockito.anyLong(), Mockito.any());
        String topic = "callback_collect_" + java.util.UUID.randomUUID().toString().replace("-", "");
        SDKManage sdk = Mockito.mock(SDKManage.class);
        Mockito.when(sdk.getClient(SDKTypeEnum.ADMIN, runtime.getUnique())).thenReturn(observed);
        try (var mocked = Mockito.mockStatic(SDKManage.class)) {
            mocked.when(SDKManage::getInstance).thenReturn(sdk);
            prepareBroker(real, topic);
            RocketMQCollect collector = collector(runtime);
            var wrapper = Mockito.mock(org.apache.eventmesh.dashboard.console.function.report.collect.DataSyncHandler.DataSyncHandlerWrapper.class);
            collector.collect(0, wrapper);
            var capture = org.mockito.ArgumentCaptor.forClass(org.apache.eventmesh.dashboard.console.function.report.collect.RestoreData.class);
            Mockito.verify(wrapper).sync(capture.capture());
            var models = capture.getValue().getDataMap();
            Assertions.assertEquals(Set.of(
                org.apache.eventmesh.dashboard.console.function.report.model.rocketmq.Rocketmq2ProducerOffset.class,
                RocketmqConsumerOffset.class, RocketmqConsumerConnectionNumber.class, RocketmqBrokerMessagesIn.class,
                RocketmqBrokerMessagesOut.class, RocketmqStorageDispatchBehindBytes.class, RocketmqStorageFlushBehindBytes.class,
                RocketmqStorageMessageReserveTime.class, RocketmqThreadPoolWartermark.class, RocketmqTopicNumber.class,
                RocketmqConsumerGroupNumber.class, RocketmqTopicMessagesIn.class, RocketmqGroupMessagesOut.class, RocketmqConsumerLagLatency.class, RocketmqConsumerSuccessTps.class, RocketmqConsumerFailedTps.class, RocketmqConsumerProcessTime.class, RocketmqStorageDiskUsage.class, RocketmqStorageDiskFreeBytes.class), models.keySet(),
                "Only existing Topic/consumer measurements and approved Broker stats/runtime measurements are collected");
            var requests = org.mockito.ArgumentCaptor.forClass(RemotingCommand.class);
            Mockito.verify(observed, Mockito.atLeastOnce()).invokeAsync(requests.capture(), Mockito.anyLong(), Mockito.any());
            Assertions.assertEquals(1L, requests.getAllValues().stream()
                .filter(request -> request.getCode() == RequestCode.GET_ALL_TOPIC_CONFIG).count(), "Topic list is shared");
            for (RemotingCommand request : requests.getAllValues()) {
                Assertions.assertTrue(Set.of(RequestCode.GET_ALL_TOPIC_CONFIG, RequestCode.GET_TOPIC_STATS_INFO,
                    RequestCode.QUERY_TOPIC_CONSUME_BY_WHO, RequestCode.GET_CONSUMER_CONNECTION_LIST,
                    RequestCode.GET_CONSUME_STATS, RequestCode.GET_BROKER_CONFIG, RequestCode.VIEW_BROKER_STATS_DATA,
                    RequestCode.GET_BROKER_RUNTIME_INFO, RequestCode.GET_ALL_SUBSCRIPTIONGROUP_CONFIG,
                    RequestCode.QUERY_CONSUME_TIME_SPAN, RequestCode.GET_CONSUMER_RUNNING_INFO).contains(request.getCode()), "Unexpected collection family");
                if (request.getCode() == RequestCode.GET_CONSUME_STATS) {
                    var header = (org.apache.rocketmq.remoting.protocol.header.GetConsumeStatsRequestHeader) request.readCustomHeader();
                    Assertions.assertNotNull(header.getTopic(), "Consumer query must be scoped to its Topic");
                }
            }
            var positions = models.get(org.apache.eventmesh.dashboard.console.function.report.model.rocketmq.Rocketmq2ProducerOffset.class);
            Assertions.assertTrue(positions.stream().map(row ->
                (org.apache.eventmesh.dashboard.console.function.report.model.rocketmq.Rocketmq2ProducerOffset) row)
                .anyMatch(row -> topic.equals(row.getTopicName()) && Long.valueOf(3).equals(row.getValue())));
            Assertions.assertTrue(models.containsKey(RocketmqConsumerConnectionNumber.class));
            var connectionRows = models.get(RocketmqConsumerConnectionNumber.class);
            Assertions.assertTrue(connectionRows.stream().map(RocketmqConsumerConnectionNumber.class::cast)
                .anyMatch(row -> topic.equals(row.getGroupName()) && Long.valueOf(1L).equals(row.getValueConnectionCount())));
            var consumers = models.get(org.apache.eventmesh.dashboard.console.function.report.model.rocketmq.RocketmqConsumerOffset.class);
            Assertions.assertTrue(consumers.stream().map(row ->
                (org.apache.eventmesh.dashboard.console.function.report.model.rocketmq.RocketmqConsumerOffset) row)
                .anyMatch(row -> topic.equals(row.getTopicName()) && topic.equals(row.getGroupName())
                    && Long.valueOf(2).equals(row.getValueConsumerOffset()) && Long.valueOf(1).equals(row.getValueOffsetLag())));
            consumers.stream().map(RocketmqConsumerOffset.class::cast).filter(row -> topic.equals(row.getTopicName()))
                .forEach(row -> LoggerFactory.getLogger(RocketMQCollectTest.class).info(
                    "消费进度验证：Topic={} Group={} Queue={} consumerOffset={} brokerOffset={} lag={}",
                    row.getTopicName(), row.getGroupName(), row.getQueueId(), row.getValueConsumerOffset(),
                    row.getValueBrokerOffset(), row.getValueOffsetLag()));
            long deadline = System.currentTimeMillis() + 15000;
            while (outstanding.get() != 0 && System.currentTimeMillis() < deadline) {
                Thread.sleep(20);
            }
            Assertions.assertEquals(0, outstanding.get(), "All callbacks must finish");
            for (int code : List.of(RequestCode.GET_ALL_TOPIC_CONFIG, RequestCode.GET_TOPIC_STATS_INFO,
                RequestCode.QUERY_TOPIC_CONSUME_BY_WHO, RequestCode.GET_CONSUME_STATS, RequestCode.GET_CONSUMER_CONNECTION_LIST,
                RequestCode.GET_BROKER_CONFIG, RequestCode.GET_BROKER_RUNTIME_INFO, RequestCode.VIEW_BROKER_STATS_DATA,
                RequestCode.GET_ALL_SUBSCRIPTIONGROUP_CONFIG)) {
                Assertions.assertTrue(successful.contains(code), "Missing successful request " + code);
            }
            assertLiveBrokerModels(models, runtime, brokerResponses, runtimeBefore.get(), runtimeAfter.get());
            assertScopeModels(models, brokerResponses, topic);
            Assertions.assertTrue(successful.contains(RequestCode.GET_CONSUMER_RUNNING_INFO));
            Assertions.assertTrue(successful.contains(RequestCode.QUERY_CONSUME_TIME_SPAN));
            var successfulClient = rows(models, RocketmqConsumerSuccessTps.class).stream()
                .filter(row -> row.getTopicName().equals(topic) && row.getClientId().equals(topic)).findFirst().orElseThrow();
            Assertions.assertEquals(topic, successfulClient.getGroupName());
            Assertions.assertEquals(2.5f, successfulClient.getValue());
            Assertions.assertEquals(0.5f, rows(models, RocketmqConsumerFailedTps.class).stream()
                .filter(row -> row.getTopicName().equals(topic)).findFirst().orElseThrow().getValue());
            Assertions.assertEquals(12.25f, rows(models, RocketmqConsumerProcessTime.class).stream()
                .filter(row -> row.getTopicName().equals(topic)).findFirst().orElseThrow().getValue());
            var lag = rows(models, RocketmqConsumerLagLatency.class).stream()
                .filter(row -> row.getTopicName().equals(topic)).findFirst().orElseThrow();
            Assertions.assertEquals("0", lag.getQueueId());
            Assertions.assertTrue(lag.getValue() >= 0 && lag.getValue() < 30000);
            var nativeRuntime = (Map<?, ?>) RemotingSerializable.decode(
                brokerResponses.get(Integer.toString(RequestCode.GET_BROKER_RUNTIME_INFO)), Map.class).get("table");
            for (var disk : rows(models, RocketmqStorageDiskUsage.class)) {
                String key = disk.getStoreType().equals("commitlog") ? "commitLogDiskRatio_" + disk.getPath()
                    : disk.getStoreType().equals("commitlog_min") ? "commitLogDiskRatio" : "consumeQueueDiskRatio";
                Assertions.assertEquals(Float.parseFloat(nativeRuntime.get(key).toString()), disk.getValue());
            }

            var nativeConfig = new java.util.Properties();
            nativeConfig.load(new java.io.StringReader(new String(
                brokerResponses.get(Integer.toString(RequestCode.GET_BROKER_CONFIG)), StandardCharsets.UTF_8)));
            for (int code : List.of(RequestCode.GET_BROKER_CONFIG, RequestCode.GET_BROKER_RUNTIME_INFO)) {
                Assertions.assertEquals(1L, requests.getAllValues().stream().filter(request -> request.getCode() == code).count());
            }
            var statsRequests = requests.getAllValues().stream()
                .filter(request -> request.getCode() == RequestCode.VIEW_BROKER_STATS_DATA)
                .filter(request -> ((ViewBrokerStatsDataRequestHeader) request.readCustomHeader()).getStatsName().startsWith("BROKER_"))
                .toList();
            Assertions.assertEquals(2, statsRequests.size());
            Set<String> names = new java.util.HashSet<>();
            for (RemotingCommand request : statsRequests) {
                var header = (ViewBrokerStatsDataRequestHeader) request.readCustomHeader();
                Assertions.assertEquals(nativeConfig.getProperty("brokerClusterName"), header.getStatsKey());
                names.add(header.getStatsName());
            }
            Assertions.assertEquals(Set.of("BROKER_PUT_NUMS", "BROKER_GET_NUMS"), names);
            if (verifyIotdb) {
                verifyIotdbTables(models, runtime, topic);
            }
        } finally {
            try {
                DeleteTopicRequestHeader delete = new DeleteTopicRequestHeader();
                delete.setTopic(topic);
                assertSuccess(real.invokeSync(RemotingCommand.createRequestCommand(RequestCode.DELETE_TOPIC_IN_BROKER, delete), 3000));
                DeleteSubscriptionGroupRequestHeader deleteGroup = new DeleteSubscriptionGroupRequestHeader();
                deleteGroup.setGroupName(topic);
                assertSuccess(real.invokeSync(RemotingCommand.createRequestCommand(RequestCode.DELETE_SUBSCRIPTIONGROUP, deleteGroup), 3000));
            } finally {
                real.shutdown();
                SDKManage.getInstance().deleteClient(null, runtime.getUnique());
            }
        }
    }

    /** Compare live observations with the exact response bodies, not with a later fluctuating Broker query. */
    @Test
    public void countsConfiguredResourcesAndScopesMessageStats() throws Exception {
        Set<String> statsKeys = new java.util.HashSet<>();
        AtomicInteger groupRequests = new AtomicInteger();
        var models = collectMock((request, callback) -> {
            switch (request.getCode()) {
                case RequestCode.GET_ALL_TOPIC_CONFIG:
                    reply(callback, ResponseCode.SUCCESS,
                        "{\"topicConfigTable\":{\"orders\":{},\"payments\":{},\"%DLQ%buyers\":{}}}".getBytes(StandardCharsets.UTF_8));
                    break;
                case RequestCode.GET_ALL_SUBSCRIPTIONGROUP_CONFIG:
                    groupRequests.incrementAndGet();
                    reply(callback, ResponseCode.SUCCESS,
                        "{\"subscriptionGroupTable\":{\"buyers\":{},\"offline\":{},\"system\":{}}}".getBytes(StandardCharsets.UTF_8));
                    break;
                case RequestCode.QUERY_TOPIC_CONSUME_BY_WHO:
                    reply(callback, ResponseCode.SUCCESS, "{\"groupList\":[\"buyers\"]}".getBytes(StandardCharsets.UTF_8));
                    break;
                case RequestCode.VIEW_BROKER_STATS_DATA:
                    var header = (ViewBrokerStatsDataRequestHeader) request.readCustomHeader();
                    Assertions.assertTrue(statsKeys.add(header.getStatsName() + ":" + header.getStatsKey()));
                    reply(callback, ResponseCode.SUCCESS,
                        ("{\"statsMinute\":{\"sum\":9007199254740993,\"tps\":1.5},"
                            + "\"statsHour\":{\"sum\":0,\"tps\":0},\"statsDay\":{\"sum\":-1,\"tps\":1}}")
                            .getBytes(StandardCharsets.UTF_8));
                    callback.operationFail(new IllegalStateException("duplicate"));
                    break;
                default:
                    callback.operationFail(new IllegalStateException("Unrelated data unavailable"));
            }
        });
        Assertions.assertEquals(3L, rows(models, RocketmqTopicNumber.class).get(0).getValue());
        Assertions.assertEquals(3L, rows(models, RocketmqConsumerGroupNumber.class).get(0).getValue());
        Assertions.assertEquals(1, groupRequests.get());
        Assertions.assertEquals(Set.of("TOPIC_PUT_NUMS:orders", "TOPIC_PUT_NUMS:payments", "TOPIC_PUT_NUMS:%DLQ%buyers",
            "GROUP_GET_NUMS:orders@buyers", "GROUP_GET_NUMS:payments@buyers"), statsKeys);
        Assertions.assertEquals(6, rows(models, RocketmqTopicMessagesIn.class).size());
        Assertions.assertEquals(4, rows(models, RocketmqGroupMessagesOut.class).size());
        rows(models, RocketmqGroupMessagesOut.class).forEach(row -> {
            Assertions.assertEquals("buyers", row.getGroupName());
            Assertions.assertEquals("minute".equals(row.getWindow()) ? 9007199254740993L : 0L, row.getValueWindowCount());
            Assertions.assertEquals("minute".equals(row.getWindow()) ? 1.5F : 0F, row.getValue());
            Assertions.assertNotNull(row.getRuntimeId());
        });
    }

    @Test
    public void missingConfigurationIsNotZeroButEmptyConfigurationIsZero() throws Exception {
        for (String body : List.of("{}", "{\"topicConfigTable\":null,\"subscriptionGroupTable\":null}", "not-json")) {
            var models = collectMock((request, callback) -> {
                if (request.getCode() == RequestCode.GET_ALL_TOPIC_CONFIG || request.getCode() == RequestCode.GET_ALL_SUBSCRIPTIONGROUP_CONFIG) {
                    reply(callback, ResponseCode.SUCCESS, body.getBytes(StandardCharsets.UTF_8));
                } else {
                    callback.operationFail(new IllegalStateException("Unavailable"));
                }
            });
            Assertions.assertTrue(models.isEmpty(), body);
        }
        var empty = collectMock((request, callback) -> {
            if (request.getCode() == RequestCode.GET_ALL_TOPIC_CONFIG || request.getCode() == RequestCode.GET_ALL_SUBSCRIPTIONGROUP_CONFIG) {
                reply(callback, ResponseCode.SUCCESS,
                    "{\"topicConfigTable\":{},\"subscriptionGroupTable\":{}}".getBytes(StandardCharsets.UTF_8));
            } else {
                callback.operationFail(new IllegalStateException("Unavailable"));
            }
        });
        Assertions.assertEquals(0L, rows(empty, RocketmqTopicNumber.class).get(0).getValue());
        Assertions.assertEquals(0L, rows(empty, RocketmqConsumerGroupNumber.class).get(0).getValue());
    }

    @Test
    public void scopedModelsHaveNameAndWindowTagsAndGaugeSemantics() {
        assertReportTable(RocketmqTopicNumber.class, "rocketmq_topic_number", List.of("runtime_id"), List.of("value"));
        assertReportTable(RocketmqConsumerGroupNumber.class, "rocketmq_consumer_group_number", List.of("runtime_id"), List.of("value"));
        assertReportTable(RocketmqTopicMessagesIn.class, "rocketmq_topic_messages_in",
            List.of("topic_name", "window"), List.of("value_window_count"), List.of("value"));
        assertReportTable(RocketmqGroupMessagesOut.class, "rocketmq_group_messages_out",
            List.of("topic_name", "group_name", "window"), List.of("value_window_count"), List.of("value"));
        var row = RocketMQCollectMapper.INSTANCE.groupMessages("orders", "buyers", "minute", 9007199254740993L, 1.5F);
        Assertions.assertEquals("orders", row.getTopicName());
        Assertions.assertEquals("buyers", row.getGroupName());
        Assertions.assertEquals(9007199254740993L, row.getValueWindowCount());
        Assertions.assertEquals(1.5F, row.getValue());
    }

    private void assertScopeModels(Map<Class<?>, List<Object>> models, Map<String, byte[]> responses, String topic) {
        Map<?, ?> topics = RemotingSerializable.decode(responses.get(Integer.toString(RequestCode.GET_ALL_TOPIC_CONFIG)), Map.class);
        Map<?, ?> groups = RemotingSerializable.decode(responses.get(Integer.toString(RequestCode.GET_ALL_SUBSCRIPTIONGROUP_CONFIG)), Map.class);
        Assertions.assertEquals((long) ((Map<?, ?>) topics.get("topicConfigTable")).size(), rows(models, RocketmqTopicNumber.class).get(0).getValue());
        Assertions.assertEquals((long) ((Map<?, ?>) groups.get("subscriptionGroupTable")).size(),
            rows(models, RocketmqConsumerGroupNumber.class).get(0).getValue());
        var incoming = rows(models, RocketmqTopicMessagesIn.class).stream().filter(row -> topic.equals(row.getTopicName())).toList();
        var outgoing = rows(models, RocketmqGroupMessagesOut.class).stream().filter(row -> topic.equals(row.getTopicName())
            && topic.equals(row.getGroupName())).toList();
        Assertions.assertEquals(3, incoming.size());
        Assertions.assertEquals(3, outgoing.size());
        for (Object row : java.util.stream.Stream.concat(incoming.stream(), outgoing.stream()).toList()) {
            boolean in = row instanceof RocketmqTopicMessagesIn;
            String window = in ? ((RocketmqTopicMessagesIn) row).getWindow() : ((RocketmqGroupMessagesOut) row).getWindow();
            long count = in ? ((RocketmqTopicMessagesIn) row).getValueWindowCount() : ((RocketmqGroupMessagesOut) row).getValueWindowCount();
            String key = in ? "TOPIC_PUT_NUMS:" + topic : "GROUP_GET_NUMS:" + topic + "@" + topic;
            Map<?, ?> wire = RemotingSerializable.decode(responses.get(key), Map.class);
            Map<?, ?> sample = (Map<?, ?>) wire.get(Map.of("minute", "statsMinute", "hour", "statsHour", "day", "statsDay").get(window));
            Assertions.assertEquals(Long.parseLong(sample.get("sum").toString()), count);
            Assertions.assertEquals(((Number) sample.get("tps")).floatValue(), ((RuntimeFloatValue) row).getValue());
        }
    }

    private void assertLiveBrokerModels(Map<Class<?>, List<Object>> models, RuntimeMetadata runtime,
        Map<String, byte[]> responses, long before, long after) {
        Map<String, String> windows = Map.of("minute", "statsMinute", "hour", "statsHour", "day", "statsDay");
        Map<Class<?>, String> stats = Map.of(RocketmqBrokerMessagesIn.class, "BROKER_PUT_NUMS",
            RocketmqBrokerMessagesOut.class, "BROKER_GET_NUMS");
        for (var entry : stats.entrySet()) {
            byte[] body = responses.get(entry.getValue());
            Assertions.assertNotNull(body, entry.getValue());
            var wire = RemotingSerializable.decode(body, java.util.HashMap.class);
            List<Object> samples = models.get(entry.getKey());
            Assertions.assertEquals(3, samples.size(), "Native Broker responses expose all three windows, including valid zero windows");
            Set<String> observedWindows = new java.util.HashSet<>();
            for (Object sample : samples) {
                String name = brokerWindow(sample);
                Assertions.assertTrue(observedWindows.add(name));
                Assertions.assertInstanceOf(Map.class, wire.get(windows.get(name)));
                var nativeWindow = (Map<?, ?>) wire.get(windows.get(name));
                Assertions.assertInstanceOf(Number.class, nativeWindow.get("sum"));
                Assertions.assertInstanceOf(Number.class, nativeWindow.get("tps"));
                long count = new java.math.BigDecimal(nativeWindow.get("sum").toString()).longValueExact();
                float rate = ((Number) nativeWindow.get("tps")).floatValue();
                Assertions.assertTrue(count >= 0 && rate >= 0 && Float.isFinite(rate));
                Assertions.assertEquals(count, brokerWindowCount(sample));
                Assertions.assertEquals(Float.valueOf(rate), ((RuntimeFloatValue) sample).getValue());
                Assertions.assertEquals(runtime.getId(), ((RuntimeFloatValue) sample).getRuntimeId());
                LoggerFactory.getLogger(RocketMQCollectTest.class).info(
                    "Broker 窗口验证：stats={} window={} count={} messagesPerSecond={}", entry.getValue(), name, count, rate);
            }
            Assertions.assertEquals(windows.keySet(), observedWindows);
        }
        byte[] body = responses.get(Integer.toString(RequestCode.GET_BROKER_RUNTIME_INFO));
        Assertions.assertNotNull(body);
        var values = RemotingSerializable.decode(body, KVTable.class).getTable();
        long dispatch = Long.parseLong(values.get("dispatchBehindBytes"));
        Assertions.assertEquals(1, rows(models, RocketmqStorageDispatchBehindBytes.class).size());
        Assertions.assertEquals(dispatch, rows(models, RocketmqStorageDispatchBehindBytes.class).get(0).getValue());
        long flush = expectedNativeBytes(values.get("remainHowManyDataToFlush"));
        if (values.containsKey("remainHowManyDataToCommit")) {
            flush = Math.addExact(flush, expectedNativeBytes(values.get("remainHowManyDataToCommit")));
        }
        Assertions.assertEquals(1, rows(models, RocketmqStorageFlushBehindBytes.class).size());
        Assertions.assertEquals(flush, rows(models, RocketmqStorageFlushBehindBytes.class).get(0).getValue());
        long earliest = Long.parseLong(values.get("earliestMessageTimeStamp"));
        Assertions.assertTrue(earliest > 0 && earliest <= before, "Fixture must expose a real earliest stored message");
        Assertions.assertEquals(1, rows(models, RocketmqStorageMessageReserveTime.class).size());
        long age = rows(models, RocketmqStorageMessageReserveTime.class).get(0).getValue();
        Assertions.assertTrue(age >= before - earliest && age <= after - earliest, "Native timestamp is converted to age in ms");
        Map<String, String> poolKeys = Map.of("send", "sendThreadPoolQueueSize", "pull", "pullThreadPoolQueueSize",
            "litePull", "litePullThreadPoolQueueSize", "query", "queryThreadPoolQueueSize", "ack", "ackThreadPoolQueueSize",
            "endTransaction", "EndTransactionQueueSize");
        Map<String, Long> expectedPools = new java.util.HashMap<>();
        poolKeys.forEach((pool, key) -> {
            if (values.containsKey(key)) {
                expectedPools.put(pool, Long.parseLong(values.get(key)));
            }
        });
        Assertions.assertFalse(expectedPools.isEmpty(), "Fixture must expose native thread pool queues");
        Map<String, Long> actualPools = new java.util.HashMap<>();
        rows(models, RocketmqThreadPoolWartermark.class).forEach(row ->
            Assertions.assertNull(actualPools.put(row.getPoolName(), row.getValue()), "Duplicate pool sample"));
        Assertions.assertEquals(expectedPools, actualPools);
        LoggerFactory.getLogger(RocketMQCollectTest.class).info(
            "Broker 运行时验证：dispatchBytes={} flushAndCommitBytes={} reserveAgeMs={} poolQueues={}", dispatch, flush, age, actualPools);
    }

    private long expectedNativeBytes(String value) {
        Assertions.assertNotNull(value, "Native byte value must be present");
        var matcher = java.util.regex.Pattern.compile("([0-9]+(?:\\.[0-9]+)?)\\s*(B|KiB|MiB|GiB|TiB|PiB|EiB)?")
            .matcher(value.trim());
        Assertions.assertTrue(matcher.matches(), value);
        String unit = matcher.group(2);
        int power = unit == null || "B".equals(unit) ? 0 : List.of("KiB", "MiB", "GiB", "TiB", "PiB", "EiB").indexOf(unit) + 1;
        return new java.math.BigDecimal(matcher.group(1)).multiply(java.math.BigDecimal.valueOf(1024).pow(power))
            .setScale(0, java.math.RoundingMode.HALF_UP).longValueExact();
    }

    /** 使用真实报表引擎建表和写入，再通过独立 JDBC 连接读回本次采集的数据。 */
    private void verifyIotdbTables(java.util.Map<Class<?>, List<Object>> models, RuntimeMetadata runtime, String topic) throws Exception {
        var engine = new org.apache.eventmesh.dashboard.console.function.report.iotdb.IotDBReportEngine();
        Class.forName("org.apache.iotdb.jdbc.IoTDBDriver");
        String address = System.getProperty("rocketmq.collect.iotdb.address", "127.0.0.1:6667");
        String configuredDatabase = System.getProperty("rocketmq.collect.iotdb.database");
        String database = configuredDatabase == null
            ? "collect_test_" + java.util.UUID.randomUUID().toString().replace("-", "") : configuredDatabase;
        Assertions.assertTrue(database.matches("collect_test_[a-z0-9_]+"), "Use a dedicated collection test database");
        String url = "jdbc:iotdb://" + address + "/" + database + "?sql_dialect=table";
        var tables = new java.util.HashMap<Class<?>, String>(java.util.Map.<Class<?>, String>of(
            RocketmqConsumerOffset.class, "rocketmq_consumer_offset",
            RocketmqConsumerConnectionNumber.class, "rocketmq_consumer_connection_number",
            RocketmqBrokerMessagesIn.class, "rocketmq_broker_messages_in",
            RocketmqBrokerMessagesOut.class, "rocketmq_broker_messages_out",
            RocketmqStorageDispatchBehindBytes.class, "rocketmq_storage_dispatch_behind_bytes",
            RocketmqStorageFlushBehindBytes.class, "rocketmq_storage_flush_behind_bytes",
            RocketmqStorageMessageReserveTime.class, "rocketmq_storage_message_reserve_time",
            RocketmqThreadPoolWartermark.class, "rocketmq_thread_pool_wartermark"));
        tables.put(RocketmqTopicNumber.class, "rocketmq_topic_number");
        tables.put(RocketmqConsumerGroupNumber.class, "rocketmq_consumer_group_number");
        tables.put(RocketmqTopicMessagesIn.class, "rocketmq_topic_messages_in");
        tables.put(RocketmqGroupMessagesOut.class, "rocketmq_group_messages_out");
        tables.put(RocketmqConsumerLagLatency.class, "rocketmq_consumer_lag_latency");
        tables.put(RocketmqConsumerSuccessTps.class, "rocketmq_consumer_success_tps");
        tables.put(RocketmqConsumerFailedTps.class, "rocketmq_consumer_failed_tps");
        tables.put(RocketmqConsumerProcessTime.class, "rocketmq_consumer_process_time");
        tables.put(RocketmqStorageDiskUsage.class, "rocketmq_storage_disk_usage");
        tables.put(RocketmqStorageDiskFreeBytes.class, "rocketmq_storage_disk_free_bytes");
        engine.setClazzToTableName(tables);
        try {
            // 使用独立测试库，避免同名历史表结构影响测试；建表及写入仍调用现有报表引擎。
            try (var connection = java.sql.DriverManager.getConnection(
                "jdbc:iotdb://" + address + "/?sql_dialect=table", "root", "root");
                var statement = connection.createStatement()) {
                statement.execute("create database if not exists " + database);
            }
            var source = new com.alibaba.druid.pool.DruidDataSource();
            org.apache.commons.lang3.reflect.FieldUtils.writeField(engine, "dataSource", source, true);
            source.setUrl(url);
            source.setDriverClassName("org.apache.iotdb.jdbc.IoTDBDriver");
            source.setUsername("root");
            source.setPassword("root");
            source.setMaxActive(2);
            source.setMaxWait(5000);
            source.init();
            for (var entry : tables.entrySet()) {
                Class<?> model = entry.getKey();
                ReportMeta annotation = model.getAnnotation(ReportMeta.class);
                var metadata = new ReportMetaData();
                metadata.setClazz(model);
                metadata.setTableName(annotation.tableName());
                metadata.setReportName(annotation.reportName());
                metadata.setComment(annotation.comment());
                var fields = org.apache.commons.lang3.reflect.FieldUtils.getAllFieldsList(model);
                engine.createReportHandler(metadata, fields);
                engine.createReport(entry.getValue());
            }
            var rows = new java.util.HashMap<Class<?>, List<Object>>();
            rows.put(RocketmqConsumerOffset.class, models.get(RocketmqConsumerOffset.class).stream()
                .filter(row -> topic.equals(((RocketmqConsumerOffset) row).getTopicName())).toList());
            rows.put(RocketmqConsumerConnectionNumber.class, models.get(RocketmqConsumerConnectionNumber.class).stream()
                .filter(row -> topic.equals(((RocketmqConsumerConnectionNumber) row).getGroupName())).toList());
            Assertions.assertFalse(rows.get(RocketmqConsumerOffset.class).isEmpty());
            Assertions.assertEquals(1, rows.get(RocketmqConsumerConnectionNumber.class).size());
            for (Class<?> model : tables.keySet()) {
                if (model != RocketmqConsumerOffset.class && model != RocketmqConsumerConnectionNumber.class) {
                    Assertions.assertTrue(models.containsKey(model), "Missing live observations for " + model.getSimpleName());
                    Assertions.assertFalse(models.get(model).isEmpty());
                    rows.put(model, models.get(model));
                }
            }
            // Producer aggregate rows have nullable numeric fields; the unchanged formatter is not exercised for those rows.
            engine.batchInsertByClass(rows);
            try (var connection = java.sql.DriverManager.getConnection(
                url, "root", "root");
                var statement = connection.createStatement()) {
                for (var entry : tables.entrySet()) {
                    String sql = "select * from " + entry.getValue() + " where runtime_id = '" + runtime.getId() + "'";
                    try (var result = statement.executeQuery(sql)) {
                        int count = 0;
                        boolean expectedSample = false;
                        Set<String> dimensions = new java.util.HashSet<>();
                        while (result.next()) {
                            count++;
                            Assertions.assertEquals("7", result.getString("organization_id"));
                            Assertions.assertEquals(runtime.getClusterId().toString(), result.getString("clusters_id"));
                            Assertions.assertEquals(runtime.getId().toString(), result.getString("runtime_id"));
                            Assertions.assertNotNull(result.getObject("time"));
                            if (entry.getKey() == RocketmqConsumerOffset.class) {
                                Assertions.assertEquals(topic, result.getString("group_name"));
                                Assertions.assertEquals(topic, result.getString("topic_name"));
                                Assertions.assertEquals("0", result.getString("queue_id"));
                                expectedSample |= result.getLong("value_consumer_offset") == 2L
                                    && result.getLong("value_broker_offset") == 3L && result.getLong("value_offset_lag") == 1L;
                            } else if (entry.getKey() == RocketmqConsumerConnectionNumber.class) {
                                Assertions.assertEquals(topic, result.getString("group_name"));
                                long expected = ((RocketmqConsumerConnectionNumber) rows.get(entry.getKey()).get(0)).getValueConnectionCount();
                                Assertions.assertEquals(expected, result.getLong("value_connection_count"));
                                expectedSample = expected == 1;
                            } else if (entry.getKey() == RocketmqTopicMessagesIn.class || entry.getKey() == RocketmqGroupMessagesOut.class) {
                                String observedTopic = result.getString("topic_name");
                                String window = result.getString("window");
                                String group = entry.getKey() == RocketmqGroupMessagesOut.class ? result.getString("group_name") : "";
                                Object expected = rows.get(entry.getKey()).stream().filter(row -> {
                                    if (row instanceof RocketmqTopicMessagesIn incoming) {
                                        return observedTopic.equals(incoming.getTopicName()) && window.equals(incoming.getWindow());
                                    }
                                    RocketmqGroupMessagesOut outgoing = (RocketmqGroupMessagesOut) row;
                                    return observedTopic.equals(outgoing.getTopicName()) && window.equals(outgoing.getWindow())
                                        && group.equals(outgoing.getGroupName());
                                }).findFirst().orElseThrow();
                                long countValue = expected instanceof RocketmqTopicMessagesIn incoming
                                    ? incoming.getValueWindowCount() : ((RocketmqGroupMessagesOut) expected).getValueWindowCount();
                                Assertions.assertEquals(countValue, result.getLong("value_window_count"));
                                Assertions.assertEquals(((RuntimeFloatValue) expected).getValue(), result.getFloat("value"));
                                Assertions.assertTrue(dimensions.add(observedTopic + ":" + group + ":" + window));
                                expectedSample = true;
                            } else {
                                String dimension = assertBrokerReadback(result, entry.getKey(), rows.get(entry.getKey()));
                                Assertions.assertTrue(dimensions.add(dimension), "Duplicate persisted dimension " + dimension);
                                expectedSample = true;
                            }
                        }
                        Assertions.assertEquals(rows.get(entry.getKey()).size(), count, entry.getValue());
                        Assertions.assertTrue(expectedSample, "Missing expected sample in " + entry.getValue());
                        LoggerFactory.getLogger(RocketMQCollectTest.class).info(
                            "IoTDB readback verified database={} table={} runtime={} topic={} rows={}",
                            database, entry.getValue(), runtime.getId(), topic, count);
                    }
                }
            }
        } finally {
            var source = (com.alibaba.druid.pool.DruidDataSource)
                org.apache.commons.lang3.reflect.FieldUtils.readField(engine, "dataSource", true);
            if (source != null) {
                source.close();
            }
            if (configuredDatabase == null) {
                try (var connection = java.sql.DriverManager.getConnection(
                    "jdbc:iotdb://" + address + "/?sql_dialect=table", "root", "root");
                    var statement = connection.createStatement()) {
                    statement.execute("drop database if exists " + database);
                }
            }
        }
    }

    private String assertBrokerReadback(java.sql.ResultSet result, Class<?> model, List<Object> expectedRows) throws Exception {
        if (Set.of(RocketmqConsumerLagLatency.class, RocketmqConsumerSuccessTps.class, RocketmqConsumerFailedTps.class, RocketmqConsumerProcessTime.class, RocketmqStorageDiskUsage.class, RocketmqStorageDiskFreeBytes.class).contains(model)) {
            var tags = org.apache.commons.lang3.reflect.FieldUtils.getAllFieldsList(model).stream()
                .filter(field -> field.isAnnotationPresent(ReportTag.class)).toList();
            List<Object> matches = new java.util.ArrayList<>();
            for (Object row : expectedRows) {
                boolean match = true;
                for (var tag : tags) {
                    String column = tag.getName().replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase(java.util.Locale.ROOT);
                    Object value = org.apache.commons.lang3.reflect.FieldUtils.readField(tag, row, true);
                    match &= java.util.Objects.equals(value == null ? null : value.toString(), result.getString(column));
                }
                if (match) {
                    matches.add(row);
                }
            }
            Assertions.assertEquals(1, matches.size());
            Object row = matches.get(0);
            if (row instanceof RuntimeFloatValue numeric) {
                Assertions.assertEquals(numeric.getValue(), result.getFloat("value"));
            } else {
                Assertions.assertEquals(((RuntimeLongValue) row).getValue(), result.getLong("value"));
            }
            Assertions.assertFalse(result.wasNull());
            return row.toString();
        }
        String dimension = "broker";
        Object expected;
        if (model == RocketmqBrokerMessagesIn.class || model == RocketmqBrokerMessagesOut.class) {
            String window = result.getString("window");
            expected = expectedRows.stream().filter(row -> window.equals(brokerWindow(row))).findFirst().orElseThrow();
            Assertions.assertEquals(brokerWindowCount(expected).longValue(), result.getLong("value_window_count"));
            Assertions.assertFalse(result.wasNull());
            Assertions.assertEquals(((RuntimeFloatValue) expected).getValue().floatValue(), result.getFloat("value"));
            Assertions.assertFalse(result.wasNull());
            dimension = window;
        } else {
            if (model == RocketmqThreadPoolWartermark.class) {
                String pool = result.getString("pool_name");
                expected = expectedRows.stream().map(RocketmqThreadPoolWartermark.class::cast)
                    .filter(row -> pool.equals(row.getPoolName())).findFirst().orElseThrow();
                dimension = pool;
            } else {
                Assertions.assertEquals(1, expectedRows.size());
                expected = expectedRows.get(0);
            }
            Assertions.assertEquals(((RuntimeLongValue) expected).getValue().longValue(), result.getLong("value"));
            Assertions.assertFalse(result.wasNull());
        }
        LoggerFactory.getLogger(RocketMQCollectTest.class).info("IoTDB Broker sample verified model={} dimension={} value={}",
            model.getSimpleName(), dimension, result.getObject("value"));
        return dimension;
    }

    private void assertSuccess(RemotingCommand response) {
        Assertions.assertEquals(ResponseCode.SUCCESS, response.getCode(), response.getRemark());
    }

    private void prepareBroker(DefaultRemotingClient client, String topic) throws Exception {
        // 真实 Broker 转发请求到已注册连接；客户端响应采用固定测试样本，不代表业务压力测试。
        client.registerProcessor(RequestCode.GET_CONSUMER_RUNNING_INFO, new org.apache.rocketmq.remoting.netty.NettyRequestProcessor() {
            @Override
            public RemotingCommand processRequest(io.netty.channel.ChannelHandlerContext context, RemotingCommand request) {
                RemotingCommand response = RemotingCommand.createResponseCommand(ResponseCode.SUCCESS, null);
                response.setBody(RemotingSerializable.encode(Map.of("statusTable", Map.of(topic,
                    Map.of("consumeOKTPS", 2.5, "consumeFailedTPS", 0.5, "consumeRT", 12.25)))));
                return response;
            }

            @Override
            public boolean rejectRequest() {
                return false;
            }
        }, null);
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
        org.apache.rocketmq.remoting.protocol.heartbeat.ConsumerData consumer =
            new org.apache.rocketmq.remoting.protocol.heartbeat.ConsumerData();
        consumer.setGroupName(topic);
        consumer.setConsumeType(org.apache.rocketmq.remoting.protocol.heartbeat.ConsumeType.CONSUME_ACTIVELY);
        consumer.setMessageModel(org.apache.rocketmq.remoting.protocol.heartbeat.MessageModel.CLUSTERING);
        consumer.setConsumeFromWhere(org.apache.rocketmq.common.consumer.ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
        consumer.getSubscriptionDataSet().add(org.apache.rocketmq.remoting.protocol.filter.FilterAPI.buildSubscriptionData(topic, "*"));
        heartbeat.getConsumerDataSet().add(consumer);
        RemotingCommand consumerBeat = RemotingCommand.createRequestCommand(RequestCode.HEART_BEAT, null);
        consumerBeat.setVersion(org.apache.rocketmq.common.MQVersion.CURRENT_VERSION);
        consumerBeat.setBody(RemotingSerializable.encode(heartbeat));
        assertSuccess(client.invokeSync(consumerBeat, 3000));
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
    }

    private RuntimeMetadata runtime() {
        RuntimeMetadata runtime = new RuntimeMetadata();
        runtime.setId(System.nanoTime());
        runtime.setClusterId(1L);
        runtime.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE);
        return runtime;
    }


}
