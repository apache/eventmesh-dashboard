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
import org.apache.eventmesh.dashboard.common.model.metadata.ClusterMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.CollectMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;
import org.apache.eventmesh.dashboard.common.utils.RowMapperUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/** 在现有同步周期内读取 Kafka 元数据，不改变其他集群的映射。 */
final class KafkaMetadataReader {

    private KafkaMetadataReader() {
    }

    static List<ClusterMetadata> readClusters(Connection connection, LocalDateTime since, Set<Long> knownIds) throws SQLException {
        final String sql = "select * from cluster where update_time >= ? and (cluster_type = ?" + knownIds("id", knownIds) + ")";
        List<Object> args = new ArrayList<>();
        args.add(since);
        args.add(ClusterType.STORAGE_KAFKA_CLUSTER.name());
        args.addAll(knownIds);
        return RowMapperUtil.executeQuery(connection, sql, args, ClusterMetadata.class);
    }

    static List<RuntimeMetadata> readRuntimes(Connection connection, LocalDateTime since, Set<Long> knownIds) throws SQLException {
        String sql = "select id, organization_id, cluster_id, cluster_type, name, INET_NTOA(host) as host, port, "
            + "jmx_port as kafka_jmx_port, status, is_delete from runtime where update_time >= ? and (cluster_type in (?, ?)"
            + knownIds("cluster_id", knownIds) + ")";
        List<Object> args = new ArrayList<>();
        args.add(since);
        args.add(ClusterType.STORAGE_KAFKA_BROKER.name());
        args.add(ClusterType.STORAGE_KAFKA_RAFT.name());
        args.addAll(knownIds);
        List<RuntimeMetadata> result = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < args.size(); index++) {
                statement.setObject(index + 1, args.get(index));
            }
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    RuntimeMetadata runtime = RowMapperUtil.mapRow(rows, RuntimeMetadata.class);
                    if (runtime.getClusterType() == ClusterType.STORAGE_KAFKA_BROKER
                        || runtime.getClusterType() == ClusterType.STORAGE_KAFKA_RAFT) {
                        String port = rows.getString("kafka_jmx_port");
                        runtime.setJmxPort(port == null || port.isBlank() ? null : Integer.valueOf(port));
                    }
                    result.add(runtime);
                }
            }
        } catch (Exception e) {
            throw new SQLException("Invalid Kafka runtime metadata", e);
        }
        return result;
    }

    static List<CollectMetadata> readConfigurations(Connection connection, LocalDateTime since, Set<Long> knownIds) throws SQLException {
        String sql = "select c.id, c.cluster_id, c.collect_type, c.save_time, c.collect_interval, c.storage_cluster_id, "
            + "c.enable as kafka_enable, c.status, c.is_delete, k.cluster_type as cluster_type from collect c "
            + "left join cluster k on k.id = c.cluster_id where c.update_time >= ? and (k.cluster_type = ?"
            + knownIds("c.cluster_id", knownIds) + ")";
        List<Object> args = new ArrayList<>();
        args.add(since);
        args.add(ClusterType.STORAGE_KAFKA_CLUSTER.name());
        args.addAll(knownIds);
        List<CollectMetadata> result = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < args.size(); index++) {
                statement.setObject(index + 1, args.get(index));
            }
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    CollectMetadata configuration = RowMapperUtil.mapRow(rows, CollectMetadata.class);
                    // 现有通用映射不能将数据库整数映射到 Boolean，仅补充该字段。
                    configuration.setEnable(rows.getBoolean("kafka_enable"));
                    result.add(configuration);
                }
            }
        } catch (Exception e) {
            throw new SQLException("Invalid Kafka collector configuration", e);
        }
        return result;
    }

    private static String knownIds(String column, Set<Long> ids) {
        return ids.isEmpty() ? "" : " or " + column + " in (" + String.join(",", Collections.nCopies(ids.size(), "?")) + ")";
    }
}
