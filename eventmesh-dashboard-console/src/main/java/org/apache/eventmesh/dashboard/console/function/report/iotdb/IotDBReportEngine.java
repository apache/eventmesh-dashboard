/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import org.apache.eventmesh.dashboard.console.function.report.annotation.ReportMeta;
import org.apache.eventmesh.dashboard.console.function.report.model.SingleGeneralReportDO;

import org.apache.iotdb.isession.ITableSession;
import org.apache.iotdb.isession.SessionDataSet;
import org.apache.iotdb.isession.pool.ITableSessionPool;
import org.apache.iotdb.session.pool.TableSessionPoolBuilder;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import com.alibaba.druid.pool.DruidDataSource;

import lombok.Getter;

/**
 * 组件类型，report名，数据类型，类名（因为采集模块），表名，默认展示类型，可以展示类型，
 */
public class IotDBReportEngine extends AbstractReportEngine {

    private ITableSessionPool iTableSessionPool;

    private DruidDataSource dataSource;

    /**
     *
     */
    @Getter
    private String sql = """
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

    @Override
    protected void doInit() {
        TableSessionPoolBuilder tableSessionPoolBuilder = new TableSessionPoolBuilder();
        tableSessionPoolBuilder.nodeUrls(Arrays.stream(this.reportConfig.getEngineAddress().split(";")).toList());
        tableSessionPoolBuilder.maxSize(100);
        tableSessionPoolBuilder.user("root");
        tableSessionPoolBuilder.password("root");
        tableSessionPoolBuilder.database("eventmesh_dashboard");

        this.iTableSessionPool = tableSessionPoolBuilder.build();


        try{
            DruidDataSource dataSource = new DruidDataSource();
            dataSource.setUrl("jdbc:iotdb://127.0.0.1:6667/eventmesh_dashboard?sql_dialect=table");
            dataSource.setDriverClassName("org.apache.iotdb.jdbc.IoTDBDriver");
            dataSource.setUsername("root");
            dataSource.setPassword("root");
            dataSource.setMaxActive(200);
            dataSource.setInitialSize(50);
            dataSource.setMaxWait(1000*60*60*24);
            dataSource.init();
            this.dataSource = dataSource;
        }catch (Exception e) {
            if(this.dataSource != null) {
                this.dataSource.close();
            }
            throw new RuntimeException(e);
        }
    }

    @Override
    protected AbstractReportMetaHandler doCreateReportHandler(ReportMeta reportMeta, List<Field> fieldList) {
        IotDBReportMetaHandler iotDBReportMetaHandler = new IotDBReportMetaHandler();
        iotDBReportMetaHandler.setReportMeta(reportMeta);
        iotDBReportMetaHandler.setFieldList(fieldList);
        return iotDBReportMetaHandler;
    }

    @Override
    public CompletableFuture<List<Map<String, Object>>> query(SingleGeneralReportDO singleGeneralReportDO) {
        return CompletableFuture.supplyAsync(() -> {
            try (ITableSession session = this.iTableSessionPool.getSession()) {
                String sql = this.buildSql(singleGeneralReportDO.getReportName(), singleGeneralReportDO.getReportType(), singleGeneralReportDO);

                try (SessionDataSet sds = session.executeQueryStatement(sql, 3000)) {
                    sds.getColumnNames();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return null;
        });

    }

    @Override
    public void createReport(String tableName) {
        if(Objects.equals(tableName , "*")){
            this.getReportMetaHandlerMap().keySet().forEach(key -> {
                String sql = this.buildSql(key, ReportViewType.CREATE_TABLE.getName(), null);
                try (ITableSession session = this.iTableSessionPool.getSession()) {
                    Connection connection = this.dataSource.getConnection();
                    connection.setAutoCommit(true);
                    connection.createStatement().execute(sql);
                    //session.executeNonQueryStatement(sql);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            return;
        }
        String sql = this.buildSql(tableName, ReportViewType.CREATE_TABLE.getName(), null);
        try (ITableSession session = this.iTableSessionPool.getSession()) {
            Connection connection = this.dataSource.getConnection();
            connection.setAutoCommit(true);
            connection.createStatement().execute(sql);
            //session.executeNonQueryStatement(sql);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void batchInsert(String tableName, List<Object> data) {
        String sql = this.buildSql(tableName, ReportViewType.INSERT.getName(), (Object) data);
        try (ITableSession session = this.iTableSessionPool.getSession()) {
            Connection connection = this.dataSource.getConnection();
            connection.setAutoCommit(true);
            connection.prepareStatement(sql).execute();
            //session.executeNonQueryStatement(sql);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
