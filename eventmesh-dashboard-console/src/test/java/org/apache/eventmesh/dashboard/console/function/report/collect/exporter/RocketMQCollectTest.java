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
            var consumers = models.get(org.apache.eventmesh.dashboard.console.function.report.model.rocketmq.RocketmqMessagesOutTotal.class);
            Assertions.assertTrue(consumers.stream().map(row ->
                (org.apache.eventmesh.dashboard.console.function.report.model.rocketmq.RocketmqMessagesOutTotal) row)
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
