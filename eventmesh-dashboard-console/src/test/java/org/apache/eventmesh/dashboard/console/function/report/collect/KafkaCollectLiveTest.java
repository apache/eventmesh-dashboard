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
import org.apache.eventmesh.dashboard.console.function.report.ReportHandlerManage;
import org.apache.eventmesh.dashboard.console.function.report.annotation.ReportMeta;
import org.apache.eventmesh.dashboard.console.function.report.iotdb.IotDBReportEngine;
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

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.alibaba.druid.pool.DruidDataSource;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 真实 Kafka、JMX 与 IoTDB 的采集及入库回归。 */
public class KafkaCollectLiveTest {

    private static final List<Class<?>> MODELS = List.of(KafkaClusterMetrics.class, KafkaTopicMetrics.class,
        KafkaPartitionOffsetMetrics.class, KafkaConsumerGroupMetrics.class, KafkaConsumerOffsetMetrics.class,
        KafkaBrokerTrafficMetrics.class, KafkaBrokerTopicTrafficMetrics.class, KafkaBrokerHealthMetrics.class);

    @Test
    public void collectsAndPersistsAllKafkaModels() throws Exception {
        String token = UUID.randomUUID().toString().replace("-", "");
        String topic = "dashboard-collect-test-" + token;
        String group = topic + "-buyer's-group";
        String database = "collect_test_kafka_" + token;
        Properties kafka = new Properties();
        kafka.put("bootstrap.servers", System.getProperty("kafka.collect.bootstrap", "127.0.0.1:19092"));
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
            broker.setPort(Integer.getInteger("kafka.collect.port", 19092));
            broker.setJmxPort(Integer.getInteger("kafka.collect.jmx.port", 19999));
            broker.setStatus(1L);
            broker.setIsDelete(0);
            CollectMetadata configuration = new CollectMetadata();
            configuration.setId(3L);
            configuration.setClusterId(1L);
            configuration.setClusterType(ClusterType.STORAGE_KAFKA_CLUSTER);
            configuration.setCollectType(CollectType.EVENTMESH);
            configuration.setStatus(1L);
            configuration.setEnable(true);
            collect.setReportHandlerManage(reports);
            collect.init();
            collect.handlerData(List.of(cluster), List.of(broker), List.of(configuration));
            org.apache.tomcat.util.threads.ThreadPoolExecutor pool =
                (org.apache.tomcat.util.threads.ThreadPoolExecutor) ReflectionTestUtils.getField(collect, "threadPoolExecutor");
            AtomicReference<Throwable> failure = new AtomicReference<>();
            Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
            Thread.setDefaultUncaughtExceptionHandler((thread, error) -> failure.compareAndSet(null, error));
            try {
                collect.collect();
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(25);
                while (pool.getCompletedTaskCount() < 2 && System.nanoTime() < deadline) {
                    TimeUnit.MILLISECONDS.sleep(25);
                }
                assertEquals(2, pool.getCompletedTaskCount());
                assertNull(failure.get(), () -> String.valueOf(failure.get()));
            } finally {
                Thread.setDefaultUncaughtExceptionHandler(previous);
            }
            Map<String, List<Map<String, Object>>> all = new HashMap<>();
            for (Class<?> type : MODELS) {
                String name = type.getAnnotation(ReportMeta.class).reportName();
                List<Map<String, Object>> values = rows(source, name);
                assertFalse(values.isEmpty(), name);
                for (Field field : type.getDeclaredFields()) {
                    if (field.getName().startsWith("value")) {
                        String column = field.getName().replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
                        assertTrue(values.stream().allMatch(row -> row.get(column) != null), column);
                    }
                }
                all.put(name, values);
            }
            List<Map<String, Object>> offsets = all.get("kafka_consumer_offset_metrics").stream()
                .filter(row -> topic.equals(row.get("topic_name")) && group.equals(row.get("group_name"))).toList();
            assertEquals(2, offsets.size());
            for (Map<String, Object> row : offsets) {
                assertEquals(1L, ((Number) row.get("value_committed_offset")).longValue());
                assertEquals(3L, ((Number) row.get("value_end_offset")).longValue());
                assertEquals(2L, ((Number) row.get("value_lag")).longValue());
            }
        } finally {
            ((ExecutorService) ReflectionTestUtils.getField(collect, "threadPoolExecutor")).shutdownNow();
            ((ExecutorService) ReflectionTestUtils.getField(reports, "scheduledExecutorService")).shutdownNow();
        }
    }

    private List<Map<String, Object>> rows(DruidDataSource source, String table) throws Exception {
        List<Map<String, Object>> values = new ArrayList<>();
        try (Connection connection = source.getConnection(); Statement statement = connection.createStatement();
            ResultSet rows = statement.executeQuery("select * from " + table)) {
            ResultSetMetaData metadata = rows.getMetaData();
            while (rows.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int index = 1; index <= metadata.getColumnCount(); index++) {
                    row.put(metadata.getColumnName(index), rows.getObject(index));
                }
                values.add(row);
            }
        }
        return values;
    }
}
