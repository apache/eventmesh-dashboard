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
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;
import org.apache.rocketmq.remoting.protocol.RequestCode;
import org.apache.rocketmq.remoting.protocol.ResponseCode;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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

/**
 * Manual test that extends {@link RocketMQCollectManualTest} by adding producer and consumer activity
 * before collection, so that consumer-side metrics (connection count, consume rate, offset lag, etc.)
 * can be collected.
 *
 * <p>Prerequisites: start RocketMQ via {@code docker compose up -d} with broker listening on
 * {@code 127.0.0.1:20911} and NameServer on {@code 127.0.0.1:9876}. No VM options required.
 *
 * <p>Note: the consumer may not consume messages when running against a Docker-based broker due to
 * network addressing, but this does not affect collection — the consumer registers with the Broker,
 * so connection count, offset lag, and other consumer-side metrics are still available for collection.
 */
@Slf4j
public class RocketMQCollectWithClientManualTest {

    static {
        System.setProperty("rocketmq.remoting.vip.channel.enabled", "false");
    }

    private static final String HOST = "127.0.0.1";
    private static final int BROKER_PORT = 20911;
    private static final String NAMESERVER = HOST + ":9876";
    private static final String TEST_TOPIC = "CollectClientTestTopic";
    private static final String PRODUCER_GROUP = "CollectClientTestProducerGroup";
    private static final String CONSUMER_GROUP = "CollectClientTestGroup";

    @Test
    @DisplayName("带生产者消费者的采集测试")
    @SuppressWarnings("unchecked")
    public void collectWithClientActivityAndPrint() throws Exception {

        DefaultMQProducer producer = new DefaultMQProducer(PRODUCER_GROUP);
        producer.setNamesrvAddr(NAMESERVER);
        producer.setSendMsgTimeout(10000);
        producer.setRetryTimesWhenSendFailed(3);
        producer.start();
        log.info("生产者已启动, namesrv={}", NAMESERVER);

        boolean initSent = false;
        for (int retry = 0; retry < 10; retry++) {
            try {
                SendResult result = producer.send(new Message(TEST_TOPIC, "TagA",
                    "InitMessage".getBytes(StandardCharsets.UTF_8)));
                log.info("初始化消息发送成功: msgId={}, status={}", result.getMsgId(), result.getSendStatus());
                initSent = true;
                break;
            } catch (Exception e) {
                log.warn("发送初始化消息失败(第{}次): {}", retry + 1, e.getMessage());
                Thread.sleep(3000);
            }
        }
        Assertions.assertTrue(initSent, "无法发送初始化消息, 请检查 RocketMQ 是否正常启动");
        log.info("Topic={} 已创建", TEST_TOPIC);

        Thread.sleep(3000);

        AtomicInteger consumed = new AtomicInteger();
        CountDownLatch consumeLatch = new CountDownLatch(10);

        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(CONSUMER_GROUP);
        consumer.setNamesrvAddr(NAMESERVER);
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);
        consumer.subscribe(TEST_TOPIC, "*");
        consumer.registerMessageListener((MessageListenerConcurrently) (List<MessageExt> msgs, ConsumeConcurrentlyContext context) -> {
            for (MessageExt msg : msgs) {
                int count = consumed.incrementAndGet();
                consumeLatch.countDown();
                log.info("消费消息 #{}: msgId={}", count, msg.getMsgId());
            }
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        });
        consumer.start();
        log.info("消费者已启动, group={}", CONSUMER_GROUP);
        Thread.sleep(5000);
        log.info("消费者已就绪");

        log.info("开始发送 10 条消息...");
        for (int i = 0; i < 10; i++) {
            SendResult result = producer.send(new Message(TEST_TOPIC, "TagA",
                ("TestMessage_" + i).getBytes(StandardCharsets.UTF_8)));
            log.info("发送 #{}: msgId={}, status={}", i, result.getMsgId(), result.getSendStatus());
        }
        log.info("已发送 10 条消息到 Topic={}", TEST_TOPIC);

        consumeLatch.await(15, TimeUnit.SECONDS);
        log.info("消费结果: 已消费={} 条", consumed.get());

        Thread.sleep(3000);

        RuntimeMetadata runtime = new RuntimeMetadata();
        runtime.setId(System.nanoTime());
        runtime.setClusterId(1L);
        runtime.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE);
        ClusterMetadata cluster = new ClusterMetadata();
        cluster.setId(runtime.getClusterId());
        cluster.setOrganizationId(7L);
        cluster.setClusterType(runtime.getClusterType());

        AtomicInteger printed = new AtomicInteger();
        Map<String, AtomicInteger> metricCounts = new ConcurrentHashMap<>();

        RocketMQCollect exporter = new RocketMQCollect() {
            @Override
            protected void setData(OrganizationId data) {
                super.setData(data);
                String name = data.getClass().getSimpleName();
                metricCounts.computeIfAbsent(name, k -> new AtomicInteger()).incrementAndGet();
                log.info("采集指标={} 数据={}", name, JSON.toJSONString(data));
                printed.incrementAndGet();
            }
        };
        exporter.setRuntimeMetadata(runtime);
        exporter.setClusterMetadata(cluster);

        AbstractSimpleCreateSDKConfig config = ConfigManage.getInstance()
            .getSimpleCreateSdkConfig(runtime.getClusterType(), SDKTypeEnum.ADMIN);
        NetAddress address = new NetAddress();
        address.setAddress(HOST);
        address.setPort(BROKER_PORT);
        config.setNetAddress(address);
        DefaultRemotingClient client = SDKManage.getInstance()
            .createClient(SDKTypeEnum.ADMIN, runtime, config, runtime.getClusterType());

        CollectManage collectManage = new CollectManage();
        Map<String, Collect> collectors = (Map<String, Collect>) FieldUtils.readField(collectManage, "executeingCollectMap", true);
        collectors.put(runtime.getUnique(), exporter);

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

            collectManage.collect();
            Assertions.assertTrue(completed.await(15, TimeUnit.SECONDS), "采集未在 15 秒内完成");
            Assertions.assertTrue(printed.get() > 0, "采集结果为空");
            log.info("采集完成, 共采集到 {} 条指标数据", printed.get());

            log.info("====== 指标采集汇总 ======");
            metricCounts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> log.info("  {} : {} 条", e.getKey(), e.getValue().get()));
            log.info("==========================");

        } finally {
            workers.shutdownNow();
            client.shutdown();
            SDKManage.getInstance().deleteClient(null, runtime.getUnique());
            consumer.shutdown();
            producer.shutdown();
            log.info("生产者、消费者已关闭");
        }
    }
}
