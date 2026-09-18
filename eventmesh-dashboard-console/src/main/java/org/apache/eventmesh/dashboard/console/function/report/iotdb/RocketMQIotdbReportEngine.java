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

package org.apache.eventmesh.dashboard.console.function.report.iotdb;

import org.apache.eventmesh.dashboard.console.function.report.model.SingleGeneralReportDO;
import org.apache.eventmesh.dashboard.console.function.report.model.rocketmq.RocketmqBrokerSample;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.alibaba.druid.pool.DruidDataSource;

/** Opt-in sample engine. The existing IoTDB engine and report framework are unchanged. */
public class RocketMQIotdbReportEngine extends IotDBReportEngine {
    private DruidDataSource sampleDataSource;

    @Override
    protected void doInit() {
        this.sampleDataSource = new DruidDataSource();
        this.sampleDataSource.setUrl("jdbc:iotdb://" + this.reportEngineConfig.getEngineAddress() + "/?sql_dialect=table&network_timeout=5000");
        this.sampleDataSource.setDriverClassName("org.apache.iotdb.jdbc.IoTDBDriver");
        this.sampleDataSource.setUsername(System.getProperty("iotdb.username", "root"));
        this.sampleDataSource.setPassword(System.getProperty("iotdb.password", "root"));
        this.sampleDataSource.setInitialSize(1);
        this.sampleDataSource.setMaxActive(4);
        this.sampleDataSource.setMaxWait(3000);
        this.sampleDataSource.setTestWhileIdle(false);
        try {
            this.sampleDataSource.init();
            this.execute("create database if not exists eventmesh_dashboard with(TTL=31536000000)");
        } catch (Exception e) {
            this.sampleDataSource.close();
            throw new IllegalStateException("Cannot initialize metric storage", e);
        }
        this.setClazzToTableName(Map.of(RocketmqBrokerSample.class, "rocketmq_broker_sample"));
    }

    @Override
    public void execute(String sql) {
        try (Connection connection = this.sampleDataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            throw new IllegalStateException("IoTDB operation failed", e);
        }
    }

    @Override
    public void createReport(String tableName) {
        requireSampleTable(tableName);
        this.execute("create table if not exists eventmesh_dashboard.rocketmq_broker_sample ("
            + "organization_id string tag, clusters_id string tag, runtime_id string tag, metric_id string tag, "
            + "topic_key_id string tag, group_key_id string tag, queue_key_id string tag, window_id string tag, family_id string tag, "
            + "value double field, value_long int64 field)");
    }

    @Override
    public void batchInsert(String tableName, List<Object> rows) {
        requireSampleTable(tableName);
        insertBrokerSamples(rows);
    }

    @Override
    public CompletableFuture<List<Map<String, Object>>> query(SingleGeneralReportDO query) {
        requireSampleTable(query.getReportName());
        try {
            return CompletableFuture.supplyAsync(() -> queryBrokerSamples(query), sampleQueryExecutor);
        } catch (RuntimeException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private void requireSampleTable(String tableName) {
        if (!"rocketmq_broker_sample".equals(tableName)) {
            throw new IllegalArgumentException("This engine stores RocketMQ Broker samples only");
        }
    }


    private final ThreadPoolExecutor sampleQueryExecutor = new ThreadPoolExecutor(
        2, 2, 0, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(32), task -> {
            Thread thread = new Thread(task, "iotdb-sample-query");
            thread.setDaemon(true);
            return thread;
        }, new ThreadPoolExecutor.AbortPolicy());

    private void insertBrokerSamples(List<Object> rows) {
        // IoTDB 2.0.3 table writes use executeStatementV2; legacy JDBC executeBatch can acknowledge without inserting.
        for (int start = 0; start < rows.size(); start += 256) {
            List<Object> batch = rows.subList(start, Math.min(rows.size(), start + 256));
            String placeholders = String.join(",", Collections.nCopies(batch.size(), "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"));
            String statementSql = "insert into eventmesh_dashboard.rocketmq_broker_sample (time, organization_id, clusters_id, runtime_id, "
                + "metric_id, topic_key_id, group_key_id, queue_key_id, window_id, family_id, value, value_long) values " + placeholders;
            try (Connection connection = this.sampleDataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(statementSql)) {
                int index = 0;
                for (Object object : batch) {
                    RocketmqBrokerSample row = (RocketmqBrokerSample) object;
                    statement.setLong(++index, row.getTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
                    statement.setString(++index, String.valueOf(row.getOrganizationId()));
                    statement.setString(++index, String.valueOf(row.getClustersId()));
                    statement.setString(++index, String.valueOf(row.getRuntimeId()));
                    statement.setString(++index, row.getMetricId().replace("'", "''"));
                    statement.setString(++index, row.getTopicKeyId().replace("'", "''"));
                    statement.setString(++index, row.getGroupKeyId().replace("'", "''"));
                    statement.setString(++index, row.getQueueKeyId().replace("'", "''"));
                    statement.setString(++index, row.getWindowId().replace("'", "''"));
                    statement.setString(++index, row.getFamilyId().replace("'", "''"));
                    statement.setDouble(++index, row.getValue());
                    if (row.getValueLong() == null) {
                        statement.setNull(++index, Types.BIGINT);
                    } else {
                        statement.setLong(++index, row.getValueLong());
                    }
                }
                statement.execute();
            } catch (SQLException e) {
                throw new IllegalStateException("IoTDB metric batch failed", e);
            }
        }
    }

    private List<Map<String, Object>> queryBrokerSamples(SingleGeneralReportDO query) {
        if (query.getStartTime() == null || query.getEndTime() == null || !query.getStartTime().isBefore(query.getEndTime())) {
            throw new IllegalArgumentException("A valid metric time range is required");
        }
        if (query.getSelectFun() != null || query.getInterval() != null) {
            throw new UnsupportedOperationException("Broker samples currently support raw observations only");
        }
        StringBuilder sqlBuilder = new StringBuilder("select * from eventmesh_dashboard.rocketmq_broker_sample where time >= ? and time < ?");
        List<Object> args = new ArrayList<>();
        args.add(query.getStartTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        args.add(query.getEndTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        if (query.getOrganizationId() != null) {
            sqlBuilder.append(" and organization_id = ?");
            args.add(query.getOrganizationId().toString());
        }
        if (query.getClustersId() != null) {
            sqlBuilder.append(" and clusters_id = ?");
            args.add(query.getClustersId().toString());
        }
        if (query.getRuntimeId() != null) {
            sqlBuilder.append(" and runtime_id = ?");
            args.add(query.getRuntimeId().toString());
        }
        sqlBuilder.append(" order by time limit ?");
        int limit = query.getLimit() == null ? 10000 : query.getLimit();
        if (limit < 1 || limit > 100000) {
            throw new IllegalArgumentException("Metric query limit must be between 1 and 100000");
        }
        args.add(limit);
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Connection connection = this.sampleDataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement(sqlBuilder.toString())) {
            for (int i = 0; i < args.size(); i++) {
                statement.setObject(i + 1, args.get(i));
            }
            try (ResultSet rs = statement.executeQuery()) {
                ResultSetMetaData metadata = rs.getMetaData();
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= metadata.getColumnCount(); i++) {
                        row.put(metadata.getColumnName(i), rs.getObject(i));
                    }
                    rows.add(row);
                }
            }
            return rows;
        } catch (SQLException e) {
            throw new IllegalStateException("IoTDB metric query failed", e);
        }
    }

    public void close() {
        sampleQueryExecutor.shutdown();
        if (sampleDataSource != null) {
            sampleDataSource.close();
        }
    }
}
