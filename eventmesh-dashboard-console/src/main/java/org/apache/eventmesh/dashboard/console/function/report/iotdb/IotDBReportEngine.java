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
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.reflection.SystemMetaObject;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import com.alibaba.druid.pool.DruidDataSource;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 组件类型，report名，数据类型，类名（因为采集模块），表名，默认展示类型，可以展示类型，
 *
 */
@SuppressWarnings("SqlSourceToSinkFlow")
@Slf4j
public class IotDBReportEngine extends AbstractReportEngine {

    private static final Set<String> SELECT_FUNCTIONS = Set.of("sum", "avg", "min", "max", "count");

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
            // 初始化 DDL 同样归还 Statement，避免初始化失败时泄漏 JDBC 资源。
            try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
                String sql = """
                        create database if not exists eventmesh_dashboard with(TTL=31536000000)
                    """;
                connection.setAutoCommit(true);
                statement.execute(sql);
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
            validateQuery(singleGeneralReportDO);
            BoundSql boundSql = this.buildBoundSql(singleGeneralReportDO.getReportName(),
                singleGeneralReportDO.getReportType(), singleGeneralReportDO);
            try (Connection connection = this.dataSource.getConnection();
                PreparedStatement ps = connection.prepareStatement(boundSql.getSql())) {
                bind(ps, boundSql);
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
        if (data.isEmpty()) {
            return;
        }
        // INSERT 与查询共享 MyBatis 的参数绑定，避免消费组名含引号时破坏 SQL。
        BoundSql sql = this.buildBoundSql(tableName, ReportViewType.INSERT.getName(), data);
        try (Connection connection = this.dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(sql.getSql())) {
            bind(statement, sql);
            statement.execute();
        } catch (SQLException e) {
            throw new RuntimeException("Unable to insert report " + tableName, e);
        }
    }

    @Override
    public void batchInsert(Map<String, List<Object>> data) {
        // 各模型独立执行，不能拼接多个无分隔的 INSERT 或丢弃绑定值。
        data.forEach(this::batchInsert);
    }

    private void validateQuery(SingleGeneralReportDO query) {
        if (query.getSelectFun() != null && !SELECT_FUNCTIONS.contains(query.getSelectFun())) {
            throw new IllegalArgumentException("Unsupported report function");
        }
        if (query.getInterval() != null && !query.getInterval().matches("[1-9][0-9]{0,8}(ms|s|m|h|d)")) {
            throw new IllegalArgumentException("Invalid report interval");
        }
        if (query.getStartTime() == null || query.getEndTime() == null || !query.getStartTime().isBefore(query.getEndTime())) {
            throw new IllegalArgumentException("A valid report time range is required");
        }
        if (!this.getReportMetaHandlerMap().containsKey(query.getReportName())) {
            throw new IllegalArgumentException("Unknown report name");
        }
        IotDBReportMetaHandler handler = (IotDBReportMetaHandler) this.getReportMetaHandlerMap().get(query.getReportName());
        // Kafka 使用远端名称标签，不能静默忽略只适用于其他模型的 topicId/groupId 过滤。
        for (String filter : List.of("organizationId", "clustersId", "runtimeId", "runtimeType", "topicId", "groupId",
            "topicName", "groupName", "partitionIndex")) {
            if (SystemMetaObject.forObject(query).getValue(filter) != null && !handler.supportsFilter(filter)) {
                throw new IllegalArgumentException("Unsupported report filter: " + filter);
            }
        }
    }

    private void bind(PreparedStatement statement, BoundSql sql) throws SQLException {
        for (int i = 0; i < sql.getParameterMappings().size(); i++) {
            ParameterMapping mapping = sql.getParameterMappings().get(i);
            String property = mapping.getProperty();
            Object value = sql.hasAdditionalParameter(property) ? sql.getAdditionalParameter(property)
                : SystemMetaObject.forObject(sql.getParameterObject()).getValue(property);
            if (value == null) {
                statement.setNull(i + 1, Types.NULL);
            } else if (value instanceof LocalDateTime time) {
                statement.setTimestamp(i + 1, java.sql.Timestamp.valueOf(time));
            } else if (property.endsWith("Id")) {
                // IoTDB 的资源 ID 是 STRING TAG，不能按 JDBC 数值类型写入。
                statement.setString(i + 1, value.toString());
            } else if (value instanceof String text) {
                // IoTDB JDBC 2.0.3 的 setString 只包裹引号，表方言仍需在绑定边界转义单引号。
                statement.setString(i + 1, text.replace("'", "''"));
            } else {
                statement.setObject(i + 1, value);
            }
        }
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
                    ps.setObject(i + 1, objectList.get(i));
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
