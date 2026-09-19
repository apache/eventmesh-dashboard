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

import org.apache.eventmesh.dashboard.console.function.report.annotation.ReportMeta;
import org.apache.eventmesh.dashboard.console.function.report.annotation.ReportMetaData;
import org.apache.eventmesh.dashboard.console.function.report.collect.RocketMQMetricModels;
import org.apache.eventmesh.dashboard.console.function.report.model.SingleGeneralReportDO;

import org.apache.commons.lang3.reflect.FieldUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.alibaba.druid.pool.DruidDataSource;
import com.google.common.base.CaseFormat;

/** Annotated report models use their own tables and native value types. */
public class RocketMQIotdbReportEngine extends IotDBReportEngine {
    private DruidDataSource sampleDataSource;
    private final Map<String, List<Field>> tableFields = new HashMap<>();
    private final Map<String, Class<?>> tableModels = new HashMap<>();

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
        Map<Class<?>, String> modelTables = new HashMap<>();
        RocketMQMetricModels.models().forEach(factory -> {
            Class<?> model = factory.get().getClass();
            ReportMeta annotation = model.getAnnotation(ReportMeta.class);
            String table = annotation.tableName();
            List<Field> fields = FieldUtils.getAllFieldsList(model).stream()
                .filter(field -> !Modifier.isStatic(field.getModifiers()) && !field.isSynthetic()).toList();
            fields.forEach(field -> field.setAccessible(true));
            tableFields.put(table, fields);
            tableModels.put(table, model);
            modelTables.put(model, table);
            ReportMetaData metadata = new ReportMetaData();
            metadata.setClazz(model);
            metadata.setClusterType(annotation.clusterType());
            metadata.setReportName(annotation.reportName());
            metadata.setTableName("eventmesh_dashboard." + table);
            metadata.setComment(annotation.comment());
            metadata.setReportViewType(annotation.defaultViewType());
            this.createReportHandler(metadata, fields);
        });
        this.setClazzToTableName(modelTables);
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
        if ("*".equals(tableName)) {
            tableFields.keySet().forEach(this::createReport);
            return;
        }
        requireModelTable(tableName);
        this.execute(this.getReportMetaHandlerMap().get(tableName).createTable());
        // Existing metric tables may predate the added measurements; add columns without changing historical rows.
        try (Connection connection = this.sampleDataSource.getConnection(); Statement statement = connection.createStatement();
            ResultSet columns = statement.executeQuery("describe eventmesh_dashboard." + tableName)) {
            Set<String> existing = new HashSet<>();
            while (columns.next()) {
                existing.add(columns.getString(1).toLowerCase(java.util.Locale.ROOT));
            }
            for (Field field : tableFields.get(tableName)) {
                String column = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, field.getName());
                if (!existing.contains(column)) {
                    String type = field.getName().endsWith("Id") || field.getType() == String.class ? "string"
                        : field.getType() == Float.class ? "float" : "int64";
                    String category = field.getName().endsWith("Id") ? "tag"
                        : field.getName().startsWith("value") ? "field" : "attribute";
                    this.execute("alter table eventmesh_dashboard." + tableName + " add column if not exists "
                        + column + " " + type + " " + category);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot extend report table " + tableName, e);
        }
    }

    @Override
    public void batchInsert(String tableName, List<Object> rows) {
        requireModelTable(tableName);
        insertReports(tableName, rows);
    }

    @Override
    public CompletableFuture<List<Map<String, Object>>> query(SingleGeneralReportDO query) {
        requireModelTable(query.getReportName());
        try {
            return CompletableFuture.supplyAsync(() -> queryReports(query), sampleQueryExecutor);
        } catch (RuntimeException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private void requireModelTable(String tableName) {
        if (!tableFields.containsKey(tableName)) {
            throw new IllegalArgumentException("Unknown RocketMQ report table: " + tableName);
        }
    }


    private final ThreadPoolExecutor sampleQueryExecutor = new ThreadPoolExecutor(
        2, 2, 0, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(32), task -> {
            Thread thread = new Thread(task, "iotdb-sample-query");
            thread.setDaemon(true);
            return thread;
        }, new ThreadPoolExecutor.AbortPolicy());

    private void insertReports(String tableName, List<Object> rows) {
        List<Field> fields = tableFields.get(tableName);
        String columns = String.join(",", fields.stream()
            .map(field -> CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, field.getName())).toList());
        String tuple = "(" + String.join(",", Collections.nCopies(fields.size(), "?")) + ")";
        // IoTDB table-aware execute is required; legacy JDBC executeBatch may acknowledge without storing rows.
        for (int start = 0; start < rows.size(); start += 256) {
            List<Object> batch = rows.subList(start, Math.min(rows.size(), start + 256));
            String statementSql = "insert into eventmesh_dashboard." + tableName + " (" + columns + ") values "
                + String.join(",", Collections.nCopies(batch.size(), tuple));
            try (Connection connection = this.sampleDataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(statementSql)) {
                int index = 0;
                for (Object row : batch) {
                    if (!tableModels.get(tableName).isInstance(row)) {
                        throw new IllegalArgumentException("Report model does not match table " + tableName);
                    }
                    for (Field field : fields) {
                        Object value = field.get(row);
                        ++index;
                        if (value == null) {
                            statement.setNull(index, field.getType() == Long.class && !field.getName().endsWith("Id")
                                ? Types.BIGINT : field.getType() == Float.class ? Types.FLOAT : Types.VARCHAR);
                        } else if (value instanceof LocalDateTime time) {
                            statement.setLong(index, time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
                        } else if (field.getName().endsWith("Id") || value instanceof String) {
                            statement.setString(index, value.toString().replace("'", "''"));
                        } else {
                            statement.setObject(index, value);
                        }
                    }
                }
                statement.execute();
            } catch (SQLException | IllegalAccessException e) {
                throw new IllegalStateException("IoTDB report batch failed: " + tableName, e);
            }
        }
    }

    private List<Map<String, Object>> queryReports(SingleGeneralReportDO query) {
        if (query.getStartTime() == null || query.getEndTime() == null || !query.getStartTime().isBefore(query.getEndTime())) {
            throw new IllegalArgumentException("A valid metric time range is required");
        }
        if (query.getSelectFun() != null || query.getInterval() != null) {
            throw new UnsupportedOperationException("Reports currently support raw observations only");
        }
        StringBuilder sqlBuilder = new StringBuilder("select * from eventmesh_dashboard.")
            .append(query.getReportName()).append(" where time >= ? and time < ?");
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
