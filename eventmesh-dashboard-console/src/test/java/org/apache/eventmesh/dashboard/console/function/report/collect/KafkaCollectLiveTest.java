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
import org.apache.eventmesh.dashboard.common.enums.CollectType;
import org.apache.eventmesh.dashboard.common.model.metadata.ClusterMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.CollectMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;
import org.apache.eventmesh.dashboard.console.controller.ReportController;
import org.apache.eventmesh.dashboard.console.function.report.ReportHandlerManage;
import org.apache.eventmesh.dashboard.console.function.report.annotation.ReportMeta;
import org.apache.eventmesh.dashboard.console.function.report.iotdb.IotDBReportEngine;
import org.apache.eventmesh.dashboard.console.function.report.model.MultiGeneralReportDO;
import org.apache.eventmesh.dashboard.console.function.report.model.SingleGeneralReportDO;
import org.apache.eventmesh.dashboard.console.function.report.model.kafka.KafkaBrokerHealthMetrics;
import org.apache.eventmesh.dashboard.console.function.report.model.kafka.KafkaBrokerTopicTrafficMetrics;
import org.apache.eventmesh.dashboard.console.function.report.model.kafka.KafkaBrokerTrafficMetrics;
import org.apache.eventmesh.dashboard.console.function.report.model.kafka.KafkaClusterMetrics;
import org.apache.eventmesh.dashboard.console.function.report.model.kafka.KafkaConsumerGroupMetrics;
import org.apache.eventmesh.dashboard.console.function.report.model.kafka.KafkaConsumerOffsetMetrics;
import org.apache.eventmesh.dashboard.console.function.report.model.kafka.KafkaPartitionOffsetMetrics;
import org.apache.eventmesh.dashboard.console.function.report.model.kafka.KafkaTopicMetrics;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.tomcat.util.threads.ThreadPoolExecutor;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.alibaba.druid.pool.DruidDataSource;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 由 kafka-collect-live-tests 显式执行，真实 Kafka、JMX 与 IoTDB 缺失时直接失败。 */
public class KafkaCollectLiveTest {

    private static final List<Class<?>> MODELS = List.of(KafkaClusterMetrics.class, KafkaTopicMetrics.class,
        KafkaPartitionOffsetMetrics.class, KafkaConsumerGroupMetrics.class, KafkaConsumerOffsetMetrics.class,
        KafkaBrokerTrafficMetrics.class, KafkaBrokerTopicTrafficMetrics.class, KafkaBrokerHealthMetrics.class);

    @Test
    public void concurrentCollectionPersistsEveryModelAndServesFilteredHttpReports() throws Exception {
        String token = UUID.randomUUID().toString().replace("-", "");
        String topic = "dashboard-collect-test-" + token;
        String group = topic + "-buyer's-group";
        String database = "collect_test_kafka_" + token;
        Properties kafka = new Properties();
        kafka.put("bootstrap.servers", System.getProperty("kafka.collect.bootstrap", "127.0.0.1:9092"));
        kafka.put("request.timeout.ms", "5000");
        kafka.put("default.api.timeout.ms", "10000");
        Class.forName("org.apache.iotdb.jdbc.IoTDBDriver");
        String address = System.getProperty("kafka.collect.iotdb.address", "127.0.0.1:6667");
        try (AdminClient admin = AdminClient.create(kafka);
            Connection root = DriverManager.getConnection("jdbc:iotdb://" + address + "/?sql_dialect=table", "root", "root")) {
            try (Statement statement = root.createStatement()) {
                statement.execute("create database " + database);
            }
            boolean createdTopic = false;
            boolean createdGroup = false;
            try (DruidDataSource source = new DruidDataSource()) {
                admin.createTopics(List.of(new NewTopic(topic, 2, (short) 1))).all().get(15, TimeUnit.SECONDS);
                createdTopic = true;
                Properties producerConfig = new Properties();
                producerConfig.putAll(kafka);
                producerConfig.put("key.serializer", StringSerializer.class.getName());
                producerConfig.put("value.serializer", StringSerializer.class.getName());
                try (KafkaProducer<String, String> producer = new KafkaProducer<>(producerConfig)) {
                    for (int partition = 0; partition < 2; partition++) {
                        for (int index = 0; index < 3; index++) {
                            producer.send(new ProducerRecord<>(topic, partition, "key", "value-" + index)).get(10, TimeUnit.SECONDS);
                        }
                    }
                }
                Properties consumerConfig = new Properties();
                consumerConfig.putAll(kafka);
                consumerConfig.put("group.id", group);
                consumerConfig.put("key.deserializer", StringDeserializer.class.getName());
                consumerConfig.put("value.deserializer", StringDeserializer.class.getName());
                consumerConfig.put("enable.auto.commit", "false");
                try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerConfig)) {
                    TopicPartition first = new TopicPartition(topic, 0);
                    TopicPartition second = new TopicPartition(topic, 1);
                    consumer.assign(List.of(first, second));
                    consumer.commitSync(Map.of(first, new OffsetAndMetadata(1L), second, new OffsetAndMetadata(1L)), Duration.ofSeconds(10));
                    createdGroup = true;
                }
                source.setUrl("jdbc:iotdb://" + address + "/" + database + "?sql_dialect=table");
                source.setDriverClassName("org.apache.iotdb.jdbc.IoTDBDriver");
                source.setUsername("root");
                source.setPassword("root");
                source.setInitialSize(1);
                source.setMaxActive(4);
                source.setMaxWait(5000);
                source.init();
                verifyPipeline(source, topic, group);
            } finally {
                try {
                    if (createdGroup) {
                        admin.deleteConsumerGroups(List.of(group)).all().get(10, TimeUnit.SECONDS);
                    }
                } finally {
                    try {
                        if (createdTopic) {
                            admin.deleteTopics(List.of(topic)).all().get(10, TimeUnit.SECONDS);
                        }
                    } finally {
                        try (Statement statement = root.createStatement()) {
                            statement.execute("drop database " + database);
                        }
                    }
                }
            }
            assertFalse(admin.listTopics().names().get(10, TimeUnit.SECONDS).contains(topic));
            assertTrue(admin.listConsumerGroups().all().get(10, TimeUnit.SECONDS).stream().noneMatch(row -> row.groupId().equals(group)));
        }
    }

    @SuppressWarnings("unchecked")
    private void verifyPipeline(DruidDataSource source, String topic, String group) throws Exception {
        IotDBReportEngine engine = new IotDBReportEngine();
        ReflectionTestUtils.setField(engine, "dataSource", source);
        ReportHandlerManage reports = new ReportHandlerManage();
        reports.setReportEngine(engine);
        engine.setClazzToTableName((Map<Class<?>, String>) ReflectionTestUtils.getField(reports, "clazzToTableName"));
        CollectManage collect = (CollectManage) ReflectionTestUtils.getField(reports, "collectManage");
        try {
            for (Class<?> type : MODELS) {
                ReflectionTestUtils.invokeMethod(reports, "handler", type);
                engine.createReport(type.getAnnotation(ReportMeta.class).reportName());
            }
            ClusterMetadata cluster = new ClusterMetadata();
            cluster.setId(1L);
            cluster.setOrganizationId(1L);
            cluster.setName("live-test-cluster");
            cluster.setClusterType(ClusterType.STORAGE_KAFKA_CLUSTER);
            cluster.setStatus(1L);
            cluster.setIsDelete(0);
            RuntimeMetadata broker = new RuntimeMetadata();
            broker.setId(2L);
            broker.setClusterId(1L);
            broker.setName("live-test-broker");
            broker.setClusterType(ClusterType.valueOf(System.getProperty("kafka.collect.node.type", "STORAGE_KAFKA_RAFT")));
            broker.setHost(System.getProperty("kafka.collect.host", "127.0.0.1"));
            broker.setPort(Integer.getInteger("kafka.collect.port", 9092));
            broker.setJmxPort(Integer.getInteger("kafka.collect.jmx.port", 9999));
            broker.setStatus(1L);
            broker.setIsDelete(0);
            CollectMetadata configuration = new CollectMetadata();
            configuration.setId(3L);
            configuration.setClusterId(1L);
            configuration.setCollectType(CollectType.EVENTMESH);
            configuration.setStatus(1L);
            configuration.setEnable(true);
            collect.setReportHandlerManage(reports);
            collect.init();
            collect.handlerData(List.of(cluster), List.of(broker), List.of(configuration));
            ThreadPoolExecutor pool = (ThreadPoolExecutor) ReflectionTestUtils.getField(collect, "threadPoolExecutor");
            AtomicReference<Throwable> failure = new AtomicReference<>();
            Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
            Thread.setDefaultUncaughtExceptionHandler((thread, error) -> failure.compareAndSet(null, error));
            try {
                collect.collect();
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
                while (pool.getCompletedTaskCount() < 2 && System.nanoTime() < deadline) {
                    Thread.sleep(25);
                }
                assertEquals(2, pool.getCompletedTaskCount());
                assertNull(failure.get(), () -> String.valueOf(failure.get()));
            } finally {
                Thread.setDefaultUncaughtExceptionHandler(previous);
            }
            for (Class<?> type : MODELS) {
                String name = type.getAnnotation(ReportMeta.class).reportName();
                List<Map<String, Object>> rows = engine.query(query(name)).get(10, TimeUnit.SECONDS);
                assertFalse(rows.isEmpty(), name);
                // 对每个实际模型字段检查 JDBC 返回值，确保完整链路没有丢字段或数值。
                for (Field field : type.getDeclaredFields()) {
                    if (field.getName().startsWith("value")) {
                        String column = field.getName().replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(java.util.Locale.ROOT);
                        assertTrue(rows.stream().allMatch(row -> row.get(column) != null), column);
                    }
                }
                SingleGeneralReportDO aggregated = query(name);
                aggregated.setInterval("10s");
                aggregated.setSelectFun("sum");
                assertFalse(engine.query(aggregated).get(10, TimeUnit.SECONDS).isEmpty(), name);
            }
            SingleGeneralReportDO offsets = query("kafka_consumer_offset_metrics");
            offsets.setTopicName(topic);
            offsets.setGroupName(group);
            List<Map<String, Object>> rows = engine.query(offsets).get(10, TimeUnit.SECONDS);
            assertEquals(2, rows.size());
            for (Map<String, Object> row : rows) {
                assertEquals(1L, ((Number) row.get("value_committed_offset")).longValue());
                assertEquals(3L, ((Number) row.get("value_end_offset")).longValue());
                assertEquals(2L, ((Number) row.get("value_lag")).longValue());
                assertEquals(group, row.get("group_name"));
            }
            offsets.setTopicName(topic + "-absent");
            assertTrue(engine.query(offsets).get(10, TimeUnit.SECONDS).isEmpty());
            SingleGeneralReportDO foreign = query("kafka_cluster_metrics");
            foreign.setOrganizationId(999L);
            assertTrue(engine.query(foreign).get(10, TimeUnit.SECONDS).isEmpty());
            verifyAggregateValues(engine);
            verifyHttp(reports, topic, group);
        } finally {
            ((ExecutorService) ReflectionTestUtils.getField(collect, "threadPoolExecutor")).shutdownNow();
            ((ExecutorService) ReflectionTestUtils.getField(reports, "scheduledExecutorService")).shutdownNow();
        }
    }

    private void verifyAggregateValues(IotDBReportEngine engine) throws Exception {
        LocalDateTime base = LocalDateTime.now().minusMinutes(1).withNano(0);
        base = base.withSecond(base.getSecond() / 10 * 10);
        for (Class<?> type : MODELS) {
            List<Object> samples = new java.util.ArrayList<>();
            boolean runtime = org.apache.eventmesh.dashboard.console.function.report.model.base.RuntimeId.class.isAssignableFrom(type);
            boolean partitioned = type == KafkaPartitionOffsetMetrics.class || type == KafkaConsumerOffsetMetrics.class;
            boolean named = java.util.Arrays.stream(type.getDeclaredFields())
                .anyMatch(field -> field.isAnnotationPresent(org.apache.eventmesh.dashboard.console.function.report.annotation.ReportTag.class));
            for (int variant = 0; variant < 2; variant++) {
                for (int index = 0; index < 2; index++) {
                    Object sample = type.getDeclaredConstructor().newInstance();
                    for (Field field : org.apache.commons.lang3.reflect.FieldUtils.getAllFieldsList(type)) {
                        field.setAccessible(true);
                        String name = field.getName();
                        if ("time".equals(name)) {
                            field.set(sample, base.plusSeconds(index + 1));
                        } else if (name.startsWith("value")) {
                            if (field.getType() == Float.class) {
                                field.set(sample, index == 0 ? 3F : 9F);
                            } else {
                                field.set(sample, index == 0 ? 3L : 9L);
                            }
                        } else if (field.getType() == Long.class) {
                            field.set(sample, "organizationId".equals(name) ? 1L : "runtimeId".equals(name) ? 20L + variant
                                : "clustersId".equals(name) ? 30L + (runtime || named ? 0 : variant) : 1L);
                        } else if (field.getType() == String.class) {
                            field.set(sample, "partitionIndex".equals(name) ? String.valueOf(variant)
                                : "aggregate-" + (partitioned ? 0 : variant));
                        }
                    }
                    samples.add(sample);
                }
            }
            String name = type.getAnnotation(ReportMeta.class).reportName();
            engine.batchInsert(name, samples);
            // 同一时间桶中的两次采样应正确聚合，同时保留节点或 Kafka 标签对应的两组资源。
            for (String function : List.of("sum", "avg", "min", "max", "count")) {
                SingleGeneralReportDO request = query(name);
                request.setClustersId(null);
                request.setStartTime(base);
                request.setEndTime(base.plusSeconds(10));
                request.setInterval("10s");
                request.setSelectFun(function);
                List<Map<String, Object>> rows = engine.query(request).get(10, TimeUnit.SECONDS);
                assertEquals(2, rows.size(), name + ":" + function);
                double expected = Map.of("sum", 12D, "avg", 6D, "min", 3D, "max", 9D, "count", 2D).get(function);
                for (Field field : type.getDeclaredFields()) {
                    if (field.getName().startsWith("value")) {
                        String column = field.getName().replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(java.util.Locale.ROOT);
                        for (Map<String, Object> row : rows) {
                            assertEquals(expected, ((Number) row.get(column)).doubleValue(), name + ":" + column + ":" + function);
                        }
                    }
                }
            }
        }
    }

    private void verifyHttp(ReportHandlerManage reports, String topic, String group) throws Exception {
        try (var context = new org.springframework.web.context.support.AnnotationConfigWebApplicationContext()) {
            context.setServletContext(new org.springframework.mock.web.MockServletContext());
            context.addBeanFactoryPostProcessor(factory -> factory.registerSingleton("reportHandlerManage", reports));
            org.springframework.test.context.support.TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context,
                "decoration.constantConfig.all=true");
            context.register(LiveHttpConfiguration.class);
            context.refresh();
            ObjectMapper json = Jackson2ObjectMapperBuilder.json()
                .featuresToDisable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).build();
            MockMvc http = MockMvcBuilders.webAppContextSetup(context).build();
            String single = http.perform(post("/report/reportBySingle").contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsBytes(query("kafka_cluster_metrics"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            assertEquals(200, json.readTree(single).path("code").asInt(), single);
            assertTrue(json.readTree(single).path("data").isArray(), single);
            assertFalse(json.readTree(single).path("data").isEmpty());
            MultiGeneralReportDO multiple = new MultiGeneralReportDO();
            BeanUtils.copyProperties(query("kafka_cluster_metrics"), multiple);
            multiple.setReportNameList(List.of("kafka_cluster_metrics", "kafka_topic_metrics"));
            String response = http.perform(post("/report/reportByHome").contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsBytes(multiple)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            assertEquals(200, json.readTree(response).path("code").asInt(), response);
            assertEquals(2, json.readTree(response).path("data").size());
            assertNotNull(json.readTree(response).path("data").get("kafka_cluster_metrics"));
            assertNotNull(json.readTree(response).path("data").get("kafka_topic_metrics"));
            SingleGeneralReportDO offsets = query("kafka_consumer_offset_metrics");
            offsets.setTopicName(topic);
            offsets.setGroupName(group);
            offsets.setPartitionIndex("0");
            var matching = postReport(http, json, offsets);
            assertEquals(1, matching.size());
            assertEquals(topic, matching.get(0).path("topic_name").asText());
            assertEquals(group, matching.get(0).path("group_name").asText());
            assertEquals("0", matching.get(0).path("partition_index").asText());
            assertEquals(2L, matching.get(0).path("value_lag").asLong());
            offsets.setGroupName(group + "-absent");
            assertTrue(postReport(http, json, offsets).isEmpty());
            SingleGeneralReportDO foreign = query("kafka_cluster_metrics");
            foreign.setOrganizationId(999L);
            assertTrue(postReport(http, json, foreign).isEmpty());
            SingleGeneralReportDO broker = query("kafka_broker_health_metrics");
            broker.setRuntimeId(2L);
            broker.setRuntimeType(System.getProperty("kafka.collect.node.type", "STORAGE_KAFKA_RAFT"));
            var runtime = postReport(http, json, broker);
            assertEquals(1, runtime.size());
            assertEquals("2", runtime.get(0).path("runtime_id").asText());
            assertEquals(broker.getRuntimeType(), runtime.get(0).path("runtime_type").asText());
            SingleGeneralReportDO invalid = query("kafka_cluster_metrics");
            invalid.setSelectFun("sum) from invalid --");
            String error = http.perform(post("/report/reportBySingle").contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsBytes(invalid)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            int errorCode = new com.lamp.decoration.core.exception.DecorationCustomExceptionResult().getDefaultExceptionResult().getCode();
            assertEquals(errorCode, json.readTree(error).path("code").asInt(), error);
            assertTrue(json.readTree(error).path("data").asText().contains("Unsupported report function"), error);
            assertNull(com.github.pagehelper.PageHelper.getLocalPage());
            assertNull(com.lamp.decoration.core.DecorationContext.get().getQueryClause());
            assertNull(com.lamp.decoration.core.DecorationContext.get().getResultObject());
        }
    }

    private com.fasterxml.jackson.databind.JsonNode postReport(MockMvc http, ObjectMapper json, SingleGeneralReportDO request) throws Exception {
        String response = http.perform(post("/report/reportBySingle").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(request)))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var result = json.readTree(response);
        assertEquals(200, result.path("code").asInt(), response);
        assertTrue(result.path("data").isArray(), response);
        return result.path("data");
    }

    @org.springframework.boot.test.context.TestConfiguration
    @org.springframework.web.servlet.config.annotation.EnableWebMvc
    @org.springframework.context.annotation.Import({org.apache.eventmesh.dashboard.console.spring.DecorationServletConfiguration.class,
        ReportController.class})
    public static class LiveHttpConfiguration implements org.springframework.web.servlet.config.annotation.WebMvcConfigurer {
        @Override
        public void configurePathMatch(org.springframework.web.servlet.config.annotation.PathMatchConfigurer configurer) {
            configurer.setPatternParser(new org.springframework.web.util.pattern.PathPatternParser());
        }
    }

    private SingleGeneralReportDO query(String name) {
        SingleGeneralReportDO query = new SingleGeneralReportDO();
        query.setReportName(name);
        query.setReportType("gauge");
        query.setOrganizationId(1L);
        query.setClustersId(1L);
        query.setStartTime(LocalDateTime.now().minusMinutes(5));
        query.setEndTime(LocalDateTime.now().plusMinutes(1));
        return query;
    }
}
