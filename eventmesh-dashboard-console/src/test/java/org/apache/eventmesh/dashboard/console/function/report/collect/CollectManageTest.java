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
import org.apache.eventmesh.dashboard.common.model.metadata.GroupMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.TopicMetadata;
import org.apache.eventmesh.dashboard.console.function.report.ReportHandlerManage;
import org.apache.eventmesh.dashboard.console.function.report.collect.padding.PaddingService;

import org.apache.commons.lang3.reflect.FieldUtils;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.alibaba.druid.pool.DruidDataSource;

@RunWith(MockitoJUnitRunner.class)
public class CollectManageTest {

    private final CollectManage collectManage = new CollectManage();

    private final boolean unit = false;

    private PaddingService paddingService = null;

    @Mock
    private ReportHandlerManage reportHandlerManage;

    @Before
    public void init() throws IOException, SQLException, IllegalAccessException {
        this.collectManage.setReportHandlerManage(reportHandlerManage);
        this.collectManage.init();

        this.paddingService = (PaddingService) FieldUtils.readField(this.collectManage, "paddingService", true);
    }


    @Test
    public void test_syncData() {
        if (this.unit) {
            return;
        }
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("""
            jdbc:mysql://127.0.0.1:3306/eventmesh_dashboard?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true""");
        dataSource.setUsername("root");
        dataSource.setPassword("Ab123123@");
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");

        // 连接池核心参数（生产推荐）
        dataSource.setInitialSize(5);          // 初始连接数
        dataSource.setMinIdle(5);              // 最小空闲连接
        dataSource.setMaxActive(20);           // 最大活跃连接
        dataSource.setMaxWait(60000);          // 获取连接等待超时 ms

        // 空闲连接检测
        dataSource.setTestWhileIdle(true);
        dataSource.setTimeBetweenEvictionRunsMillis(60000);
        dataSource.setMinEvictableIdleTimeMillis(300000);
        dataSource.setTestOnBorrow(false);
        dataSource.setTestOnReturn(false);
        dataSource.setValidationQuery("SELECT 1");
        this.collectManage.setDataSource(dataSource);
        this.collectManage.syncData();
    }

    @Test
    public void test_collect_cluster_jvm() throws InterruptedException {
        List<ClusterMetadata> clusterMetadataList = new ArrayList<>();
        ClusterMetadata clusterMetadata = new ClusterMetadata();
        clusterMetadata.setId(1L);
        clusterMetadata.setName("test");
        clusterMetadata.setClusterType(ClusterType.STORAGE_JVM_BROKER);
        clusterMetadata.setStatus(1L);
        clusterMetadataList.add(clusterMetadata);

        List<CollectMetadata> collectMetadataList = new ArrayList<>();
        CollectMetadata collectMetadata = new CollectMetadata();
        collectMetadataList.add(collectMetadata);
        collectMetadata.setId(1L);
        collectMetadata.setClusterId(1L);
        collectMetadata.setCollectType(CollectType.EVENTMESH);
        collectMetadata.setEnable(true);
        collectMetadata.setDefaultStorage(true);
        collectMetadata.setStatus(1L);
        collectMetadata.setClusterType(clusterMetadata.getClusterType());

        List<RuntimeMetadata> runtimeMetadataList = new ArrayList<>();
        RuntimeMetadata runtimeMetadata = new RuntimeMetadata();
        runtimeMetadata.setId(1L);
        runtimeMetadata.setName("test");
        runtimeMetadata.setClusterId(1L);
        runtimeMetadata.setStatus(1L);
        runtimeMetadata.setClusterType(clusterMetadata.getClusterType());
        runtimeMetadataList.add(runtimeMetadata);

        List<TopicMetadata> topicMetadataList = new ArrayList<>();
        TopicMetadata topicMetadata = new TopicMetadata();
        topicMetadataList.add(topicMetadata);
        topicMetadata.setId(1L);
        topicMetadata.setClusterId(1L);
        topicMetadata.setRuntimeId(1L);
        topicMetadata.setName("1");
        topicMetadata.setStatus(1L);
        topicMetadata.setIsDelete(1);
        topicMetadata.setClusterType(clusterMetadata.getClusterType());
        this.paddingService.setTopicMetadata(topicMetadataList);

        List<GroupMetadata> groupMetadataList = new ArrayList<>();
        GroupMetadata groupMetadata = new GroupMetadata();
        groupMetadataList.add(groupMetadata);
        groupMetadata.setId(1L);
        groupMetadata.setName("1");
        groupMetadata.setClusterId(1L);
        groupMetadata.setRuntimeId(1L);
        groupMetadata.setStatus(1L);
        groupMetadata.setIsDelete(1);
        groupMetadata.setClusterType(clusterMetadata.getClusterType());
        this.paddingService.setGroupMetadata(groupMetadataList);

        collectManage.handlerData(clusterMetadataList, runtimeMetadataList, collectMetadataList);

        collectManage.collect();
        Thread.sleep(50);
    }

    @Test
    public void addMetadata() {
        this.doAddMetadata();
    }

    private void doAddMetadata() {

    }


}
