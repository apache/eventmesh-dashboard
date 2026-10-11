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
import org.apache.eventmesh.dashboard.console.function.report.annotation.ReportMetaData;
import org.apache.eventmesh.dashboard.console.function.report.iotdb.IotDBReportMetaHandler;
import org.apache.eventmesh.dashboard.console.function.report.model.kafka.KafkaBrokerHealthMetrics;
import org.apache.eventmesh.dashboard.console.function.report.model.kafka.KafkaBrokerTopicTrafficMetrics;
import org.apache.eventmesh.dashboard.console.function.report.model.kafka.KafkaBrokerTrafficMetrics;
import org.apache.eventmesh.dashboard.console.function.report.model.kafka.KafkaClusterMetrics;
import org.apache.eventmesh.dashboard.console.function.report.model.kafka.KafkaConsumerGroupMetrics;
import org.apache.eventmesh.dashboard.console.function.report.model.kafka.KafkaConsumerOffsetMetrics;
import org.apache.eventmesh.dashboard.console.function.report.model.kafka.KafkaPartitionOffsetMetrics;
import org.apache.eventmesh.dashboard.console.function.report.model.kafka.KafkaTopicMetrics;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.session.Configuration;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class KafkaReportModelTest {

    @Test
    public void missingMetricProducesSqlNullInsteadOfAnEmptyNumericSlot() {
        KafkaBrokerTrafficMetrics sample = new KafkaBrokerTrafficMetrics();
        sample.setTime(LocalDateTime.of(2026, 10, 7, 12, 0));
        sample.setValueMessagesInTotal(42L);
        String sql = new XMLLanguageDriver().createSqlSource(new Configuration(),
            "<script>" + handler(KafkaBrokerTrafficMetrics.class).insert() + "</script>", List.class)
            .getBoundSql(List.of(sample)).getSql();
        assertTrue(sql.contains("NULL"));
        assertFalse(sql.contains(",,"));
    }

    @Test
    public void groupedMetricsProduceDistinctIoTDBFields() {
        assertFields(KafkaClusterMetrics.class, "value_broker_count", "value_topic_count", "value_partition_count",
            "value_offline_partition_count");
        assertFields(KafkaBrokerTrafficMetrics.class, "value_messages_in_total", "value_bytes_in_total");
        assertFields(KafkaBrokerHealthMetrics.class, "value_under_replicated_partitions");
        assertTrue(handler(KafkaBrokerHealthMetrics.class).createTable().contains("value_produce_request_mean_ms float"));
        assertFields(KafkaBrokerTopicTrafficMetrics.class, "value_messages_in_total", "value_bytes_in_total", "value_bytes_out_total");
        assertFields(KafkaTopicMetrics.class, "value_partition_count", "value_replica_count", "value_under_replicated_partition_count");
        assertFields(KafkaPartitionOffsetMetrics.class, "value_earliest_offset", "value_latest_offset");
        assertFields(KafkaConsumerGroupMetrics.class, "value_member_count");
        assertFields(KafkaConsumerOffsetMetrics.class, "value_committed_offset", "value_end_offset", "value_lag");
        assertTag(KafkaTopicMetrics.class, "topic_name");
        assertTag(KafkaBrokerTopicTrafficMetrics.class, "topic_name");
        assertTag(KafkaPartitionOffsetMetrics.class, "topic_name");
        assertTag(KafkaConsumerOffsetMetrics.class, "group_name");
    }

    private void assertFields(Class<?> model, String... expectedFields) {
        IotDBReportMetaHandler handler = handler(model);
        String create = handler.createTable();
        String insert = handler.insert();
        String query = handler.query("gauge");
        for (String field : expectedFields) {
            assertTrue(create.contains(field + " int64"), field + " missing from table");
            assertTrue(insert.contains(field), field + " missing from insert");
            assertTrue(query.contains(field), field + " missing from query");
        }
    }

    private void assertTag(Class<?> model, String field) {
        IotDBReportMetaHandler handler = handler(model);
        assertTrue(handler.createTable().contains(field + " string  tag"));
        assertTrue(handler.insert().contains(field));
    }

    private IotDBReportMetaHandler handler(Class<?> model) {
        ReportMeta reportMeta = model.getAnnotation(ReportMeta.class);
        ReportMetaData metadata = new ReportMetaData();
        metadata.setClazz(model);
        metadata.setTableName(reportMeta.tableName());
        metadata.setComment(reportMeta.comment());
        List<Field> fields = new ArrayList<>(FieldUtils.getAllFieldsList(model));
        Collections.reverse(fields);
        IotDBReportMetaHandler handler = new IotDBReportMetaHandler();
        handler.setReportMeta(metadata);
        handler.setFieldList(fields);
        return handler;
    }
}
