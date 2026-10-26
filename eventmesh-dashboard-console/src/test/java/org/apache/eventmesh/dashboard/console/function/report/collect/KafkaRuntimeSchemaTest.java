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

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class KafkaRuntimeSchemaTest {

    @Test
    public void allowsBrokersOnTheSameHostAndKeepsAddressesUnique() throws Exception {
        try (InputStream resource = getClass().getResourceAsStream("/eventmesh-dashboard.sql")) {
            assertNotNull(resource);
            String schema = new String(resource.readAllBytes(), StandardCharsets.UTF_8);
            String runtime = schema.substring(schema.indexOf("create table runtime"), schema.indexOf("drop table if exists `client`"));
            // H2 要求列级 UNIQUE 放在默认值之后；只调整语法顺序，保留原表约束以复现冲突。
            runtime = runtime.replace("bigint unique not null default 0", "bigint not null default 0 unique");
            try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:kafka-runtime-schema;MODE=MySQL");
                Statement statement = connection.createStatement()) {
                statement.execute(runtime);
                insert(connection, "broker-1", 9092);
                insert(connection, "broker-2", 9094);
                assertThrows(SQLException.class, () -> insert(connection, "duplicate-address", 9092));
                try (ResultSet rows = statement.executeQuery("select count(*) from runtime")) {
                    rows.next();
                    assertEquals(2, rows.getInt(1));
                }
            }
        }
    }

    private void insert(Connection connection, String name, int port) throws SQLException {
        String sql = "insert into runtime(organization_id, cluster_id, cluster_type, name, host, port, version, trusteeship_type, "
            + "first_to_whom, replication_type, deploy_status_type, create_script_content, resources_config_id, deploy_script_id) "
            + "values(1, 1, 'STORAGE_KAFKA_BROKER', ?, 2130706433, ?, '4.5.0', 'NOT', 'DB', 'NOT', 'NOT', '', '', 0)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            statement.setInt(2, port);
            statement.executeUpdate();
        }
    }
}
