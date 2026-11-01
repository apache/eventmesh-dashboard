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

import org.apache.eventmesh.dashboard.console.function.report.annotation.ReportMeta;
import org.apache.eventmesh.dashboard.console.function.report.iotdb.IotDBReportEngine;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.iotdb.jdbc.IoTDBConnection;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.base.CaseFormat;

/** Kafka 样本单独绑定参数，沿用既有 IoTDB 连接与表结构。 */
final class KafkaReportWriter {

    private final IotDBReportEngine engine;

    KafkaReportWriter(IotDBReportEngine engine) {
        this.engine = engine;
    }

    void insert(Map<Class<?>, List<Object>> samples) {
        for (Map.Entry<Class<?>, List<Object>> entry : samples.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                insert(entry.getKey(), entry.getValue());
            }
        }
    }

    private void insert(Class<?> type, List<Object> samples) {
        ReportMeta metadata = type.getAnnotation(ReportMeta.class);
        String table = identifier(metadata.tableName());
        List<Field> fields = new ArrayList<>(FieldUtils.getAllFieldsList(type));
        fields.removeIf(field -> Modifier.isStatic(field.getModifiers()));
        Collections.reverse(fields);
        String columns = fields.stream().map(field -> identifier(CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, field.getName())))
            .collect(Collectors.joining(","));
        String row = "(" + String.join(",", Collections.nCopies(fields.size(), "?")) + ")";
        String sql = "insert into " + table + " (" + columns + ") values " + String.join(",", Collections.nCopies(samples.size(), row));
        try (Connection connection = engine.openConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = 1;
            for (Object sample : samples) {
                for (Field field : fields) {
                    field.setAccessible(true);
                    bind(statement, index++, field, field.get(sample));
                }
            }
            statement.execute();
        } catch (SQLException | IllegalAccessException e) {
            throw new IllegalStateException("Failed to insert Kafka report " + table, e);
        }
    }

    private void bind(PreparedStatement statement, int index, Field field, Object value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.NULL);
        } else if (value instanceof LocalDateTime time) {
            // IoTDB 2.0.3 不支持 LocalDateTime 的 setObject；先复刻 setTimestamp 的时区与毫秒语义，再使用表模型字符串绑定。
            IoTDBConnection connection = statement.getConnection().unwrap(IoTDBConnection.class);
            ZoneId connectionZone = ZoneId.of(connection.getTimeZone());
            Instant instant = time.atZone(ZoneId.systemDefault()).withLaterOffsetAtOverlap().toInstant().truncatedTo(ChronoUnit.MILLIS);
            LocalDateTime normalizedTime = LocalDateTime.ofInstant(instant, connectionZone);
            statement.setString(index, normalizedTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        } else if (value instanceof String text) {
            // IoTDB JDBC 的字符串绑定需要转义单引号；存储值仍为原始名称。
            statement.setString(index, text.replace("'", "''"));
        } else if (field.getName().endsWith("Id")) {
            statement.setString(index, value.toString());
        } else {
            statement.setObject(index, value);
        }
    }

    private String identifier(String name) {
        if (!name.matches("[a-z][a-z0-9_]*")) {
            throw new IllegalArgumentException("Invalid Kafka report identifier");
        }
        return name;
    }
}
