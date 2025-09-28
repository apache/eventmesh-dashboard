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

package org.apache.eventmesh.dashboard.console.function.report.collect;

import org.apache.eventmesh.dashboard.common.enums.MetadataType;
import org.apache.eventmesh.dashboard.common.model.base.BaseClusterIdBase;
import org.apache.eventmesh.dashboard.console.function.report.model.base.ClusterId;
import org.apache.eventmesh.dashboard.console.function.report.model.base.GroupId;
import org.apache.eventmesh.dashboard.console.function.report.model.base.OrganizationId;
import org.apache.eventmesh.dashboard.console.function.report.model.base.RuntimeId;
import org.apache.eventmesh.dashboard.console.function.report.model.base.SubscribeId;
import org.apache.eventmesh.dashboard.console.function.report.model.base.TopicId;
import org.apache.eventmesh.dashboard.core.metadata.DataMetadataHandler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.helpers.MessageFormatter;

import com.alibaba.druid.pool.DruidDataSource;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MetadataDataManage {

    private String sql;

    private DruidDataSource dataSource;

    private final List<NameAndId> nameAndIdList = new ArrayList<>();

    private LocalDateTime localDateTime = LocalDateTime.of(2000, 1, 1, 0, 0);

    private final Map<NameAndId, NameAndId> nameAndIdMap = new ConcurrentHashMap<>();

    private final NameAndId nameAndId = new NameAndId();


    public boolean supplement(OrganizationId organizationId) {
        Long superId = organizationId.getOrganizationId();
        if (organizationId instanceof ClusterId clusterId) {
            superId = this.getId(MetadataType.CLUSTER, superId, clusterId.getClustersName());
            clusterId.setClustersId(superId);
        }
        if (organizationId instanceof RuntimeId runtimeId) {
            superId = this.getId(MetadataType.RUNTIME, superId, runtimeId.getRuntimeName());
            runtimeId.setRuntimeId(superId);
        }
        if (organizationId instanceof TopicId topicId) {
            Long topic = this.getId(MetadataType.TOPIC, superId, topicId.getTopicName());
            topicId.setTopicId(topic);
        } else if (organizationId instanceof GroupId groupId) {
            Long group = this.getId(MetadataType.GROUP, superId, groupId.getGroupName());
            groupId.setGroupId(group);
        } else if (organizationId instanceof SubscribeId subscribeId) {
            Long topic = this.getId(MetadataType.TOPIC, superId, subscribeId.getTopicName());
            subscribeId.setTopicId(topic);
            Long group = this.getId(MetadataType.GROUP, superId, subscribeId.getGroupName());
            subscribeId.setGroupId(group);
        }

        return Boolean.FALSE;
    }

    public void init(String url, String user, String password) {
        this.createSql();
        this.createConnection(url, user, password);
    }

    public void syncData() {
        this.executeSql();
        this.handlerMetadata();
    }

    private void executeSql() {
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(this.sql);
            int selectObjectCount = 4;
            for (int i = 1; i <= selectObjectCount; i++) {
                preparedStatement.setObject(i, localDateTime);
            }

            this.localDateTime = LocalDateTime.now();
            try (ResultSet re = preparedStatement.executeQuery()) {
                if (re.next()) {
                    NameAndId nameAndId = new NameAndId();
                    nameAndId.setId(re.getLong("id"));
                    nameAndId.setName(re.getString("name"));
                    nameAndId.setSuperId(re.getLong("super_id"));
                    nameAndId.setIsDelete(re.getInt("is_delete"));
                    nameAndId.setStatus(re.getLong("status"));
                    nameAndId.setCreateTime(Instant.ofEpochMilli(re.getLong("create_time")).atZone(ZoneId.systemDefault()).toLocalDateTime());
                    nameAndId.setUpdateTime(Instant.ofEpochMilli(re.getLong("update_time")).atZone(ZoneId.systemDefault()).toLocalDateTime());
                    this.nameAndIdList.add(nameAndId);
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public void handlerMetadata() {
        this.nameAndIdList.forEach((value) -> {
            if (value.isDelete()) {
                nameAndIdMap.remove(value);
            } else if (Objects.equals(value.getStatus(), 1L)) {
                nameAndIdMap.remove(value);
            } else if (Objects.equals(value.getCreateTime(), value.getUpdateTime())) {
                nameAndIdMap.put(value, value);
            } else {
                nameAndIdMap.put(value, value);
            }
        });
    }


    private void createConnection(String url, String user, String password) {
        try (DruidDataSource dataSource = new DruidDataSource()) {
            dataSource.setUrl(url);
            dataSource.setUsername(user);
            dataSource.setPassword(password);
            dataSource.setInitialSize(5);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     *  TODO 需要重构，因为 普罗米修斯 与 eventmesh 的 采集架构冲突了 </p>
     *       保留，</p>
     *       1. 两个之间不重提，</p>
     *       2. 没时间 ，</p>
     *       3. 不确定，两者时间是否兼容 </p>
     *       4. 没有多少性能损伤
     */
    private void createSql() {
        StringBuilder stringBuilder = new StringBuilder();
        String sql = this.createSql(MetadataType.CLUSTER, "cluster", "organization_id", "name");
        stringBuilder.append(sql).append("\r\n union all \r\n");
        sql = this.createSql(MetadataType.RUNTIME, "runtime", "cluster_id", "name");
        stringBuilder.append(sql).append("\r\n union all \r\n");
        sql = this.createSql(MetadataType.GROUP, "group", "runtime_id", "name");
        stringBuilder.append(sql).append("\r\n union all \r\n");
        sql = this.createSql(MetadataType.TOPIC, "topic", "runtime_id", "name");
        stringBuilder.append(sql);

        this.sql = stringBuilder.toString();
    }


    public String createSql(MetadataType metadataType, String table, String superId, String name) {
        String sql = """
                select {} , id , {} , update_time,create_time,is_delete,status from {} where update_time > ?
            """;
        return MessageFormatter.arrayFormat(sql, List.of(metadataType,superId, table, name).toArray()).getMessage();
    }

    public DataMetadataHandler<BaseClusterIdBase> createDataMetadataHandler(MetadataType metadataType) {
        return new DataMetadataHandler<BaseClusterIdBase>() {

            @Override
            public List<BaseClusterIdBase> getData() {
                return List.of();
            }

            @Override
            public void handleAll(Collection<BaseClusterIdBase> allData, List<BaseClusterIdBase> addData, List<BaseClusterIdBase> updateData,
                List<BaseClusterIdBase> deleteData) {

            }
        };
    }

    public Long getId(MetadataType metadataType, Long superId, String name) {
        nameAndId.setMetadataType(metadataType);
        nameAndId.setSuperId(superId);
        nameAndId.setName(name);
        NameAndId nameAndId = this.nameAndIdMap.get(this.nameAndId);
        return Objects.isNull(nameAndId) ? null : nameAndId.getId();
    }


    @Data
    private class NameAndId extends BaseClusterIdBase {

        private MetadataType metadataType;

        private Long superId;

        private String name;

        @Override
        public int hashCode() {
            return metadataType.hashCode() + superId.hashCode() + name.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof NameAndId nameAndId) {
                return Objects.equals(metadataType, nameAndId.metadataType)
                       && Objects.equals(superId, nameAndId.superId)
                       && Objects.equals(name, nameAndId.name);
            }
            return false;
        }

        @Override
        public String nodeUnique() {
            return "";
        }
    }

    interface QueryOperator {


        String querySql();

        String s(ResultSet re);

    }


}
