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
import org.apache.eventmesh.dashboard.console.function.report.iotdb.IotDBReportMetaHandler;
import org.apache.eventmesh.dashboard.console.function.report.model.rocketmq.RocketmqConsumerConnectionNumber;
import org.apache.eventmesh.dashboard.console.function.report.model.rocketmq.RocketmqConsumerOffset;
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
import org.apache.rocketmq.remoting.protocol.RequestCode;
import org.apache.rocketmq.remoting.protocol.ResponseCode;

import org.apache.rocketmq.remoting.protocol.RemotingSerializable;
import org.apache.rocketmq.remoting.protocol.header.CreateTopicRequestHeader;
import org.apache.rocketmq.remoting.protocol.header.DeleteSubscriptionGroupRequestHeader;
import org.apache.rocketmq.remoting.protocol.header.DeleteTopicRequestHeader;
import org.apache.rocketmq.remoting.protocol.header.SendMessageRequestHeader;
import org.apache.rocketmq.remoting.protocol.header.UpdateConsumerOffsetRequestHeader;
import org.apache.rocketmq.remoting.protocol.heartbeat.HeartbeatData;
import org.apache.rocketmq.remoting.protocol.heartbeat.ProducerData;
import org.apache.rocketmq.remoting.protocol.subscription.SubscriptionGroupConfig;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

public class RocketMQCollectTest {
    @Test
    public void mapsConsumerSamplesToDedicatedModels() {
        var offset = new org.apache.rocketmq.remoting.protocol.admin.OffsetWrapper();
        offset.setBrokerOffset(9007199254740993L);
        offset.setConsumerOffset(9007199254740991L);
        RocketmqConsumerOffset row = RocketMQCollectMapper.INSTANCE.consumerOffset("orders", "buyers", "3", offset);
        Assertions.assertEquals("orders", row.getTopicKeyId());
        Assertions.assertEquals("orders", row.getTopicName());
        Assertions.assertEquals("buyers", row.getGroupKeyId());
        Assertions.assertEquals("buyers", row.getGroupName());
        Assertions.assertEquals("3", row.getQueueKeyId());
        Assertions.assertEquals(9007199254740993L, row.getValueBrokerOffset());
        Assertions.assertEquals(9007199254740991L, row.getValueConsumerOffset());
        Assertions.assertEquals(2L, row.getValueOffsetLag());
        offset.setConsumerOffset(9007199254740994L);
        Assertions.assertEquals(0L,
            RocketMQCollectMapper.INSTANCE.consumerOffset("orders", "buyers", "3", offset).getValueOffsetLag());
        RocketmqConsumerConnectionNumber connections = RocketMQCollectMapper.INSTANCE.connections("buyers", 2L);
        Assertions.assertEquals("buyers", connections.getGroupKeyId());
        Assertions.assertEquals(2L, connections.getValueConnectionCount());
        Assertions.assertEquals(0L, RocketMQCollectMapper.INSTANCE.connections("buyers", 0L).getValueConnectionCount());
    }

    @Test
    public void dedicatedModelsDescribeSeparateGaugeTables() {
        assertReportTable(RocketmqConsumerOffset.class, "rocketmq_consumer_offset",
            List.of("topic_key_id", "group_key_id", "queue_key_id"),
            List.of("value_consumer_offset", "value_broker_offset", "value_offset_lag"));
        assertReportTable(RocketmqConsumerConnectionNumber.class, "rocketmq_consumer_connection_number",
            List.of("group_key_id"), List.of("value_connection_count"));
    }

    private void assertReportTable(Class<?> model, String table, List<String> tags, List<String> measurements) {
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
            org.apache.eventmesh.dashboard.console.function.report.model.rocketmq.Rocketmq2ProducerOffset.class), models.keySet());
        Assertions.assertEquals(2, models.get(RocketmqConsumerOffset.class).size());
        models.get(RocketmqConsumerOffset.class).stream().map(RocketmqConsumerOffset.class::cast).forEach(row -> {
            Assertions.assertEquals("buyers", row.getGroupKeyId());
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
                .filter(row -> "0".equals(row.getQueueKeyId())).findFirst().orElseThrow();
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
        MetricLog logs = new MetricLog();
        Logger logger = (Logger) LoggerFactory.getLogger(RocketMQCollect.class);
        logs.start();
        logger.addAppender(logs);
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
                            }
                            callback.operationComplete(future);
                        } finally {
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
            Assertions.assertEquals(3, models.size(), "Only Topic offsets, consumer offsets and connections are collected");
            var requests = org.mockito.ArgumentCaptor.forClass(RemotingCommand.class);
            Mockito.verify(observed, Mockito.atLeastOnce()).invokeAsync(requests.capture(), Mockito.anyLong(), Mockito.any());
            Assertions.assertEquals(1L, requests.getAllValues().stream()
                .filter(request -> request.getCode() == RequestCode.GET_ALL_TOPIC_CONFIG).count(), "Topic list is shared");
            for (RemotingCommand request : requests.getAllValues()) {
                Assertions.assertTrue(Set.of(RequestCode.GET_ALL_TOPIC_CONFIG, RequestCode.GET_TOPIC_STATS_INFO,
                    RequestCode.QUERY_TOPIC_CONSUME_BY_WHO, RequestCode.GET_CONSUMER_CONNECTION_LIST,
                    RequestCode.GET_CONSUME_STATS).contains(request.getCode()), "Unexpected collection family");
                if (request.getCode() == RequestCode.GET_CONSUME_STATS) {
                    var header = (org.apache.rocketmq.remoting.protocol.header.GetConsumeStatsRequestHeader) request.readCustomHeader();
                    Assertions.assertNotNull(header.getTopic(), "Consumer query must be scoped to its Topic");
                }
            }
            var positions = models.get(org.apache.eventmesh.dashboard.console.function.report.model.rocketmq.Rocketmq2ProducerOffset.class);
            Assertions.assertTrue(positions.stream().map(row ->
                (org.apache.eventmesh.dashboard.console.function.report.model.rocketmq.Rocketmq2ProducerOffset) row)
                .anyMatch(row -> topic.equals(row.getTopicKeyId()) && Long.valueOf(3).equals(row.getValue())));
            Assertions.assertTrue(models.containsKey(RocketmqConsumerConnectionNumber.class));
            var connectionRows = models.get(RocketmqConsumerConnectionNumber.class);
            Assertions.assertTrue(connectionRows.stream().map(RocketmqConsumerConnectionNumber.class::cast)
                .anyMatch(row -> topic.equals(row.getGroupKeyId()) && row.getValueConnectionCount() > 0));
            var consumers = models.get(org.apache.eventmesh.dashboard.console.function.report.model.rocketmq.RocketmqConsumerOffset.class);
            Assertions.assertTrue(consumers.stream().map(row ->
                (org.apache.eventmesh.dashboard.console.function.report.model.rocketmq.RocketmqConsumerOffset) row)
                .anyMatch(row -> topic.equals(row.getTopicKeyId()) && topic.equals(row.getGroupKeyId())
                    && Long.valueOf(2).equals(row.getValueConsumerOffset()) && Long.valueOf(1).equals(row.getValueOffsetLag())));
            long deadline = System.currentTimeMillis() + 15000;
            while (outstanding.get() != 0 && System.currentTimeMillis() < deadline) {
                Thread.sleep(20);
            }
            Assertions.assertEquals(0, outstanding.get(), "All callbacks must finish");
            for (int code : List.of(RequestCode.GET_ALL_TOPIC_CONFIG, RequestCode.GET_TOPIC_STATS_INFO,
                RequestCode.QUERY_TOPIC_CONSUME_BY_WHO, RequestCode.GET_CONSUME_STATS, RequestCode.GET_CONSUMER_CONNECTION_LIST)) {
                Assertions.assertTrue(successful.contains(code), "Missing successful request " + code);
            }
            for (String metric : List.of("queue_max_offset", "consumer_offset", "offset_lag", "consumer_connection_count")) {
                Assertions.assertTrue(logs.messages.stream().anyMatch(message -> message.contains("指标=" + metric)), "Missing metric " + metric);
            }
            Assertions.assertTrue(logs.messages.stream().anyMatch(message -> message.contains("指标=queue_max_offset")
                && message.contains("Topic=" + topic) && message.endsWith("数值=3")));
            Assertions.assertTrue(logs.messages.stream().anyMatch(message -> message.contains("指标=offset_lag")
                && message.contains("Topic=" + topic) && message.endsWith("数值=1")));
            Assertions.assertFalse(logs.messages.stream().anyMatch(message -> message.contains("collection failed")), "Callback decoding failed");
            if (verifyIotdb) {
                verifyIotdbTables(models, runtime, topic);
            }
        } finally {
            logger.detachAppender(logs);
            logs.stop();
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

    /** 使用真实报表引擎建表和写入，再通过独立 JDBC 连接读回本次采集的数据。 */
    private void verifyIotdbTables(java.util.Map<Class<?>, List<Object>> models, RuntimeMetadata runtime, String topic) throws Exception {
        var engine = new org.apache.eventmesh.dashboard.console.function.report.iotdb.IotDBReportEngine();
        Class.forName("org.apache.iotdb.jdbc.IoTDBDriver");
        String address = System.getProperty("rocketmq.collect.iotdb.address", "127.0.0.1:6667");
        String database = "collect_test_" + java.util.UUID.randomUUID().toString().replace("-", "");
        String url = "jdbc:iotdb://" + address + "/" + database + "?sql_dialect=table";
        var tables = java.util.Map.<Class<?>, String>of(
            RocketmqConsumerOffset.class, "rocketmq_consumer_offset",
            RocketmqConsumerConnectionNumber.class, "rocketmq_consumer_connection_number");
        engine.setClazzToTableName(tables);
        try {
            // 使用独立测试库，避免同名历史表结构影响测试；建表及写入仍调用现有报表引擎。
            try (var connection = java.sql.DriverManager.getConnection(
                "jdbc:iotdb://" + address + "/?sql_dialect=table", "root", "root");
                var statement = connection.createStatement()) {
                statement.execute("create database " + database);
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
                .filter(row -> topic.equals(((RocketmqConsumerOffset) row).getTopicKeyId())).toList());
            rows.put(RocketmqConsumerConnectionNumber.class, models.get(RocketmqConsumerConnectionNumber.class).stream()
                .filter(row -> topic.equals(((RocketmqConsumerConnectionNumber) row).getGroupKeyId())).toList());
            Assertions.assertFalse(rows.get(RocketmqConsumerOffset.class).isEmpty());
            Assertions.assertEquals(1, rows.get(RocketmqConsumerConnectionNumber.class).size());
            engine.batchInsertByClass(rows);
            try (var connection = java.sql.DriverManager.getConnection(
                url, "root", "root");
                var statement = connection.createStatement()) {
                for (var entry : tables.entrySet()) {
                    String sql = "select * from " + entry.getValue() + " where runtime_id = '" + runtime.getId() + "'";
                    try (var result = statement.executeQuery(sql)) {
                        int count = 0;
                        boolean expectedSample = false;
                        while (result.next()) {
                            count++;
                            Assertions.assertEquals("7", result.getString("organization_id"));
                            Assertions.assertEquals(topic, result.getString("group_key_id"));
                            Assertions.assertNotNull(result.getObject("time"));
                            if (entry.getKey() == RocketmqConsumerOffset.class) {
                                Assertions.assertEquals(topic, result.getString("topic_key_id"));
                                expectedSample |= result.getLong("value_consumer_offset") == 2L
                                    && result.getLong("value_broker_offset") == 3L && result.getLong("value_offset_lag") == 1L;
                            } else {
                                long expected = ((RocketmqConsumerConnectionNumber) rows.get(entry.getKey()).get(0)).getValueConnectionCount();
                                Assertions.assertEquals(expected, result.getLong("value_connection_count"));
                                expectedSample = expected > 0;
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
        }
    }

    private void assertSuccess(RemotingCommand response) {
        Assertions.assertEquals(ResponseCode.SUCCESS, response.getCode(), response.getRemark());
    }

    private void prepareBroker(DefaultRemotingClient client, String topic) throws Exception {
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

    private static class MetricLog extends AppenderBase<ILoggingEvent> {
        private final List<String> messages = new CopyOnWriteArrayList<>();

        @Override
        protected void append(ILoggingEvent event) {
            this.messages.add(event.getFormattedMessage());
        }
    }
}
