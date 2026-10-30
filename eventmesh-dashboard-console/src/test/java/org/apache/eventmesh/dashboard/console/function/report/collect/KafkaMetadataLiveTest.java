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

import org.apache.eventmesh.dashboard.console.function.report.ReportEngine;
import org.apache.eventmesh.dashboard.console.function.report.ReportHandlerManage;
import org.apache.eventmesh.dashboard.console.function.report.collect.exporter.KafkaAdminCollect;
import org.apache.eventmesh.dashboard.console.function.report.collect.exporter.KafkaJmxCollect;
import org.apache.eventmesh.dashboard.console.function.report.model.kafka.KafkaBrokerTrafficMetrics;
import org.apache.eventmesh.dashboard.console.function.report.model.kafka.KafkaClusterMetrics;

import org.apache.commons.lang3.reflect.FieldUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import com.alibaba.druid.pool.DruidDataSource;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

/** 使用参考版本的真实表结构验证 Kafka 配置读取与任务生命周期。 */
public class KafkaMetadataLiveTest {

    @Test
    public void mysqlMetadataRegistersAndDisablesKafkaCollectors() throws Exception {
        String address = System.getProperty("kafka.collect.mysql.address", "127.0.0.1:13306");
        String user = System.getProperty("kafka.collect.mysql.user", "root");
        String password = System.getProperty("kafka.collect.mysql.password", "codex_test_pw");
        String database = "kafka_metadata_test_" + UUID.randomUUID().toString().replace("-", "");
        String baseUrl = "jdbc:mysql://" + address + "/?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
        Class.forName("com.mysql.cj.jdbc.Driver");
        try (Connection root = DriverManager.getConnection(baseUrl, user, password); Statement statement = root.createStatement()) {
            statement.execute("create database " + database);
            try (DruidDataSource source = new DruidDataSource()) {
                source.setUrl("jdbc:mysql://" + address + "/" + database + "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true");
                source.setDriverClassName("com.mysql.cj.jdbc.Driver");
                source.setUsername(user);
                source.setPassword(password);
                source.setInitialSize(1);
                source.setMaxActive(2);
                source.init();
                createMetadata(source);
                verifyRegistration(source);
            } finally {
                statement.execute("drop database " + database);
            }
        }
    }

    private void createMetadata(DruidDataSource source) throws Exception {
        String schema = Files.readString(Path.of("src/main/resources/eventmesh-dashboard.sql"));
        try (Connection connection = source.getConnection(); Statement statement = connection.createStatement()) {
            for (String table : new String[] {"cluster", "runtime", "`collect`"}) {
                String startToken = "create table " + table + "\n(";
                int start = schema.indexOf(startToken);
                if (start < 0) {
                    throw new IllegalStateException("Missing reference table " + table);
                }
                statement.execute(schema.substring(start, schema.indexOf(';', start) + 1));
            }
            statement.execute("insert into cluster (id, organization_id, cluster_type, name, version, trusteeship_type, "
                + "cluster_own_type, first_to_whom, replication_type, deploy_status_type, resources_config_id, deploy_script_id, config) "
                + "values (11, 1, 'STORAGE_KAFKA_CLUSTER', 'kafka-test', '3.9', 'SELF', 'PRIVATE', 'NOT', 'NOT', 'NOT', '', 0, ''), "
                + "(21, 1, 'STORAGE_ROCKETMQ_CLUSTER', 'rocket-test', '5', 'SELF', 'PRIVATE', 'NOT', 'NOT', 'NOT', '', 0, '')");
            try (PreparedStatement runtime = connection.prepareStatement("insert into runtime "
                + "(id, organization_id, cluster_id, cluster_type, name, host, port, version, jmx_port, trusteeship_type, first_to_whom, "
                + "replication_type, deploy_status_type, kubernetes_cluster_id, create_script_content, resources_config_id, deploy_script_id) "
                + "values (12, 1, 11, ?, 'broker-a', INET_ATON('127.0.0.1'), ?, '3.9', ?, 'SELF', 'NOT', 'NOT', 'NOT', 101, '', '', 0), "
                + "(13, 1, 11, 'STORAGE_KAFKA_BROKER', 'broker-b', INET_ATON('127.0.0.2'), ?, '3.9', '', "
                + "'SELF', 'NOT', 'NOT', 'NOT', 102, '', '', 0)")) {
                runtime.setString(1, System.getProperty("kafka.collect.node.type", "STORAGE_KAFKA_RAFT"));
                runtime.setInt(2, Integer.getInteger("kafka.collect.port", 19092));
                runtime.setString(3, Integer.toString(Integer.getInteger("kafka.collect.jmx.port", 19999)));
                runtime.setInt(4, Integer.getInteger("kafka.collect.port", 19092));
                runtime.executeUpdate();
            }
            statement.execute("insert into collect (id, cluster_id, collect_type, save_time, collect_interval, storage_cluster_id, "
                + "storage_cluster_name, enable) values (14, 11, 'EVENTMESH', 3600, 5, 1, 'iotdb', 1), "
                + "(24, 21, 'EVENTMESH', 3600, 5, 1, '1', 1)");
        }
    }

    private void verifyRegistration(DruidDataSource source) throws Exception {
        CollectManage manager = new CollectManage();
        ReportHandlerManage reports = new ReportHandlerManage();
        ReportEngine engine = mock(ReportEngine.class);
        CountDownLatch delivered = new CountDownLatch(2);
        Set<Class<?>> samples = ConcurrentHashMap.newKeySet();
        doAnswer(invocation -> {
            Map<Class<?>, ?> values = invocation.getArgument(0);
            samples.addAll(values.keySet());
            delivered.countDown();
            return null;
        }).when(engine).batchInsertByClass(anyMap());
        reports.setReportEngine(engine);
        manager.setReportHandlerManage(reports);
        manager.setKafkaDataSource(source);
        assertNull(ReflectionTestUtils.getField(manager, "dataSource"));
        manager.init();
        try {
            manager.syncData();
            assertTrue(tasks(manager).get("ClusterMetadata-11") instanceof KafkaAdminCollect);
            assertTrue(tasks(manager).get("RuntimeMetadata-12") instanceof KafkaJmxCollect);
            assertFalse(tasks(manager).containsKey("RuntimeMetadata-13"));
            try (Connection connection = source.getConnection(); Statement statement = connection.createStatement()) {
                statement.executeUpdate("update collect set enable = 0 where id = 14");
            }
            manager.syncData();
            assertEquals(0, tasks(manager).size());
            try (Connection connection = source.getConnection(); Statement statement = connection.createStatement()) {
                statement.executeUpdate("update collect set enable = 1 where id = 14");
            }
            manager.syncData();
            assertTrue(tasks(manager).get("ClusterMetadata-11") instanceof KafkaAdminCollect);
            manager.collect();
            assertTrue(delivered.await(20, TimeUnit.SECONDS));
            assertTrue(samples.contains(KafkaClusterMetrics.class));
            assertTrue(samples.contains(KafkaBrokerTrafficMetrics.class));
        } finally {
            ((ExecutorService) ReflectionTestUtils.getField(manager, "threadPoolExecutor")).shutdownNow();
            ((ExecutorService) ReflectionTestUtils.getField(reports, "scheduledExecutorService")).shutdownNow();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Collect> tasks(CollectManage manager) throws IllegalAccessException {
        Map<String, Collect> all = (Map<String, Collect>) FieldUtils.readField(manager, "executeingCollectMap", true);
        Map<String, Collect> kafka = new HashMap<>();
        all.forEach((key, value) -> {
            if (value instanceof KafkaAdminCollect || value instanceof KafkaJmxCollect) {
                kafka.put(key, value);
            }
        });
        return kafka;
    }
}
