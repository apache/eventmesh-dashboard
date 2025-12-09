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

import org.apache.eventmesh.dashboard.console.function.report.AbstractReportEngine;
import org.apache.eventmesh.dashboard.console.function.report.ReportViewType;
import org.apache.eventmesh.dashboard.console.function.report.annotation.AbstractReportMetaHandler;
import org.apache.eventmesh.dashboard.console.function.report.annotation.ReportMetaData;
import org.apache.eventmesh.dashboard.console.function.report.model.SingleGeneralReportDO;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import com.alibaba.druid.pool.DruidDataSource;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 组件类型，report名，数据类型，类名（因为采集模块），表名，默认展示类型，可以展示类型，
 */
@SuppressWarnings("SqlSourceToSinkFlow")
@Slf4j
public class IotDBReportEngine extends AbstractReportEngine {

    /**
     *
     */
    @Getter
    private final String sql = """
        select
           <trim prefix=" " suffixOverrides=",">
                <if test="organizationId != null and organizationId != 0">
                    organization_id,
                </if>
                <if test="clusterId != null and clusterId != 0">
                    cluster_id
                </if>
                <if test="runtimeId != null and runtimeId != 0">
                    runtime_id,
                </if>
                <if test="topicId != null and topicId != 0">
                    topic_id,
                </if>
                <if test="groupId != null and groupId != 0">
                    group_id,
                </if>
                <if test="queueId != null and queueId != 0">
                    queue_id,
                </if>
                <if test="subscriptionId != null and subscriptionId != 0">
                    subscription_id,
                </if>
            </trim>
            <if test="viewType.name == 'counter'">
                <if test="fun != null and fun != ''">
                    ${fun}( value )
                </if>
                <if test="fun == null">
                    value
                </if>
            </if>
            <if test="viewType.name == 'gauge'">
                <if test="fun != null and fun != ''">
                    ${fun}( value )
                </if>
                <if test="fun == null">
                    value
                </if>
            </if>
            <if test="viewType.name == 'histogram'">
                <if test="fun != null and fun != ''">
                    ${fun}( value )
                </if>
                <if test="fun == null">
                    value
                </if>
            </if>
            <if test="viewType.name == 'summary'">
        
            </if>
            <if test="viewType.name == 'histogram_counter'">
        
            </if>
        from ${table}
            where
                <if test="organizationId != null and organizationId != 0">
                    organization_id = ${organizationId} and
                </if>
                <if test="clusterId != null and clusterId != 0">
                    cluster_id = ${clusterId} and
                </if>
                <if test="runtimeId != null and runtimeId != 0">
                    runtime_id = ${runtimeId} and
                </if>
                <if test="topicId != null and topicId != 0">
                    topic_id = ${topicId} and
                </if>
                <if test="groupId != null and groupId != 0">
                    group_id = ${groupId} and
                </if>
                <if test="queueId != null and queueId != 0">
                    queue_id = ${queueId} and
                </if>
                <if test="subscriptionId != null and subscriptionId != 0">
                    subscription_id = ${subscriptionId} and
                </if>
            re >= ${startTime} and re < ${endTime}
        
            group by ([${startTime}, ${endTime}), ${interval})
        
                <if test="organizationId != null and organizationId != 0">
                    , organization_id
                </if>
                <if test="clusterId != null and clusterId != 0">
                    , cluster_id
                </if>
                <if test="runtimeId != null and runtimeId != 0">
                    , runtime_id
                </if>
                <if test="topicId != null and topicId != 0">
                    , topic_id
                </if>
                <if test="groupId != null and groupId != 0">
                    , group_id
                </if>
                <if test="queueId != null and queueId != 0">
                    , queue_id
                </if>
                <if test="subscriptionId != null and subscriptionId != 0">
                    , subscription_id
                </if>
        
        """;
    private DruidDataSource dataSource;
    private boolean isBatch = false;

    @Override
    protected void doInit() {
        try {
            this.initDatabase();
            this.dataSource =
                this.createDataSource("jdbc:iotdb://" + this.reportEngineConfig.getEngineAddress() + "/eventmesh_dashboard?sql_dialect=table");
        } catch (Exception e) {
            if (this.dataSource != null) {
                this.dataSource.close();
            }
            throw new RuntimeException(e);
        }
    }

    public void initDatabase() throws SQLException {
        try (DruidDataSource dataSource = this.createDataSource(
            "jdbc:iotdb://" + this.reportEngineConfig.getEngineAddress() + "/?sql_dialect=table")) {
            try (Connection connection = dataSource.getConnection()) {
                String sql = """
                        create database if not exists eventmesh_dashboard with(TTL=31536000000)
                    """;
                connection.setAutoCommit(true);
                connection.createStatement().execute(sql);
            }
        }
    }


    private DruidDataSource createDataSource(String url) throws SQLException {
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl(url);
        dataSource.setDriverClassName("org.apache.iotdb.jdbc.IoTDBDriver");
        dataSource.setUsername("root");
        dataSource.setPassword("root");
        dataSource.setMaxActive(200);
        dataSource.setInitialSize(50);
        dataSource.setMaxWait(1000 * 60 * 60 * 24);
        dataSource.init();
        return dataSource;
    }

    @Override
    protected AbstractReportMetaHandler doCreateReportHandler(ReportMetaData reportMetaData, List<Field> fieldList) {
        return new IotDBReportMetaHandler();
    }

    @Override
    public CompletableFuture<List<Map<String, Object>>> query(SingleGeneralReportDO singleGeneralReportDO) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = this.buildSql(singleGeneralReportDO.getReportName(), singleGeneralReportDO.getReportType(), singleGeneralReportDO);
            try (Connection connection = this.dataSource.getConnection();
                PreparedStatement ps = connection.prepareStatement(sql)) {
                connection.isReadOnly();
                try (ResultSet rs = ps.executeQuery()) {
                    List<Map<String, Object>> list = new ArrayList<>();
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();
                    while (rs.next()) {
                        Map<String, Object> row = new HashMap<>();
                        for (int i = 1; i <= columnCount; i++) {
                            row.put(metaData.getColumnName(i), rs.getObject(i));
                        }
                        list.add(row);
                    }
                    return list;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

    }

    @Override
    public void createReport(String tableName) {
        String sql;
        if (Objects.equals(tableName, "*")) {
            sql = this.getCreateTableSql();
            if (StringUtils.isEmpty(sql)) {
                return;
            }
        } else {
            sql = this.buildSql(tableName, ReportViewType.CREATE_TABLE.getName(), null);
        }
        this.execute(sql);
    }

    @Override
    public void batchInsert(String tableName, List<Object> data) {
        String sql = this.buildSql(tableName, ReportViewType.INSERT.getName(), (Object) data);
        this.execute(sql);
    }

    @Override
    public void batchInsert(Map<String, List<Object>> data) {
        StringBuilder sb = new StringBuilder();
        List<String> sqlList = new ArrayList<>();
        int count = 5000;
        for (Entry<String, List<Object>> entry : data.entrySet()) {
            String sql = this.buildSql(entry.getKey(), ReportViewType.INSERT.getName(), entry.getValue());
            count = count - entry.getValue().size();
            sb.append(sql);
            if (count < 0) {
                count = 5000;
                sqlList.add(sb.toString());
                sb.delete(0, sb.length());
            }
        }
        if (!sb.isEmpty()) {
            sqlList.add(sb.toString());
        }
        this.execute(sqlList);
    }

    /**
     * 创建 表时，可以直接定义数据存储时间。字段是： TLL
     */
    @Override
    public void deleteData() {

    }


    public String getCreateTableSql() {
        StringBuilder stringBuilder = new StringBuilder();
        Set<String> tableNameSet = new HashSet<>();
        String showTablesSql = " show tables";
        this.executeQuery(showTablesSql, (rs) -> {
            try {
                while (rs.next()) {
                    tableNameSet.add(rs.getString("TableName"));
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        this.getReportMetaHandlerMap().keySet().forEach(key -> {
            if (!tableNameSet.contains(key)) {
                String sql = this.buildSql(key, ReportViewType.CREATE_TABLE.getName(), null);
                if (this.isBatch) {
                    stringBuilder.append(sql).append("\r\nwith \r\n");
                } else {
                    this.execute(sql);
                }
            }
        });
        String sql = stringBuilder.toString();
        return StringUtils.isEmpty(sql) ? sql : sql.substring(0, sql.length() - 9);
    }


    public void execute(String sql) {
        try (Connection conn = this.dataSource.getConnection();
            Statement statement = conn.createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException(sql, e);
        }
    }

    public void execute(List<String> sql) {
        sql.forEach(this::execute);
    }

    public void executeQuery(String sql, Consumer<ResultSet> consumer) {
        this.executeQuery(sql, null, consumer);
    }

    public void executeQuery(String sql, List<Object> objectList, Consumer<ResultSet> consumer) {
        try (Connection conn = this.dataSource.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            if (CollectionUtils.isNotEmpty(objectList)) {
                for (int i = 0; i < objectList.size(); i++) {
                    ps.setObject(i, objectList.get(i));
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                consumer.accept(rs);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
