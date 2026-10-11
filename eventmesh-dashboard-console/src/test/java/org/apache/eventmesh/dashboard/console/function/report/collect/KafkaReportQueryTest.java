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

import org.apache.eventmesh.dashboard.console.controller.ReportController;
import org.apache.eventmesh.dashboard.console.function.report.ReportEngine;
import org.apache.eventmesh.dashboard.console.function.report.ReportHandlerManage;
import org.apache.eventmesh.dashboard.console.function.report.annotation.ReportMeta;
import org.apache.eventmesh.dashboard.console.function.report.annotation.ReportMetaData;
import org.apache.eventmesh.dashboard.console.function.report.iotdb.IotDBReportEngine;
import org.apache.eventmesh.dashboard.console.function.report.iotdb.IotDBReportMetaHandler;
import org.apache.eventmesh.dashboard.console.function.report.model.MultiGeneralReportDO;
import org.apache.eventmesh.dashboard.console.function.report.model.SingleGeneralReportDO;
import org.apache.eventmesh.dashboard.console.function.report.model.kafka.KafkaConsumerOffsetMetrics;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.session.Configuration;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class KafkaReportQueryTest {

    @Test
    public void singleQueryReturnsRowsUnderTheReportName() {
        ReportEngine engine = mock(ReportEngine.class);
        List<Map<String, Object>> rows = List.of(Map.of("value_broker_count", 2L));
        when(engine.query(any())).thenReturn(CompletableFuture.completedFuture(rows));
        ReportHandlerManage manager = new ReportHandlerManage();
        manager.setReportEngine(engine);
        ReportController controller = new ReportController();
        ReflectionTestUtils.setField(controller, "reportHandlerManage", manager);

        assertEquals(rows, controller.reportBySingle(query("kafka_cluster_metrics")));
    }

    @Test
    public void batchKeepsBothGaugeReportsAndCopiesSharedFilters() {
        ReportEngine engine = mock(ReportEngine.class);
        when(engine.query(any())).thenAnswer(invocation -> {
            SingleGeneralReportDO request = invocation.getArgument(0);
            return CompletableFuture.completedFuture(List.of(Map.of("source", request.getReportName())));
        });
        ReportHandlerManage manager = new ReportHandlerManage();
        manager.setReportEngine(engine);
        ReportController controller = new ReportController();
        ReflectionTestUtils.setField(controller, "reportHandlerManage", manager);
        MultiGeneralReportDO request = new MultiGeneralReportDO();
        request.setReportNameList(List.of("kafka_cluster_metrics", "kafka_topic_metrics"));
        request.setOrganizationId(1L);
        request.setClustersId(2L);
        request.setRuntimeId(3L);
        request.setRuntimeType("STORAGE_KAFKA_BROKER");
        request.setReportType("gauge");
        request.setStartTime(LocalDateTime.of(2026, 10, 11, 12, 0));
        request.setEndTime(request.getStartTime().plusMinutes(1));
        request.setInterval("10s");
        request.setSelectFun("sum");

        Map<String, List<Map<String, Object>>> result = controller.reportByHome(request);

        assertEquals(2, result.size());
        assertEquals("kafka_cluster_metrics", result.get("kafka_cluster_metrics").get(0).get("source"));
        assertEquals("kafka_topic_metrics", result.get("kafka_topic_metrics").get(0).get("source"));
        ArgumentCaptor<SingleGeneralReportDO> queries = ArgumentCaptor.forClass(SingleGeneralReportDO.class);
        verify(engine, times(2)).query(queries.capture());
        for (SingleGeneralReportDO query : queries.getAllValues()) {
            assertEquals(request.getOrganizationId(), query.getOrganizationId());
            assertEquals(request.getClustersId(), query.getClustersId());
            assertEquals(request.getRuntimeId(), query.getRuntimeId());
            assertEquals(request.getRuntimeType(), query.getRuntimeType());
            assertEquals(request.getReportType(), query.getReportType());
            assertEquals(request.getStartTime(), query.getStartTime());
            assertEquals(request.getEndTime(), query.getEndTime());
            assertEquals(request.getInterval(), query.getInterval());
            assertEquals(request.getSelectFun(), query.getSelectFun());
        }
    }

    @Test
    public void rawQueryPreservesKafkaDimensionsWithoutAggregateFunctionsOrDuplicateCommas() {
        String sql = sql(query("kafka_consumer_offset_metrics"));
        assertFalse(sql.matches("(?s).*,\\s*,.*"), sql);
        assertFalse(sql.contains("last_by"), sql);
        assertTrue(sql.contains("group_name"), sql);
        assertTrue(sql.contains("topic_name"), sql);
        assertTrue(sql.contains("partition_index"), sql);
        assertTrue(sql.contains("organization_id=?"), sql);
        assertTrue(sql.contains("clusters_id=?"), sql);
        assertFalse(sql.contains("2026-10-11"), sql);
    }

    @Test
    public void aggregateQueryKeepsEveryKafkaTagInTheGrouping() {
        SingleGeneralReportDO query = query("kafka_consumer_offset_metrics");
        query.setSelectFun("sum");
        query.setInterval("10s");
        String sql = sql(query);
        String group = sql.substring(sql.indexOf("group by"));
        assertFalse(sql.matches("(?s).*,\\s*,.*"), sql);
        for (String tag : List.of("organization_id", "clusters_id", "group_name", "topic_name", "partition_index")) {
            assertTrue(group.contains(tag), sql);
        }
        assertTrue(sql.contains("last_by(clusters_name, time) as clusters_name"), sql);
        assertTrue(sql.contains("sum(value_lag) as value_lag"), sql);
    }

    @Test
    public void loadsConfigurationBeforeSchedulingAndRefreshesItInSeconds() {
        ReportHandlerManage manager = new ReportHandlerManage();
        CollectManage collectors = mock(CollectManage.class);
        ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
        ReflectionTestUtils.setField(manager, "collectManage", collectors);
        ReflectionTestUtils.setField(manager, "scheduledExecutorService", scheduler);

        ReflectionTestUtils.invokeMethod(manager, "startCollection");

        InOrder order = inOrder(collectors, scheduler);
        order.verify(collectors).syncData();
        order.verify(scheduler).scheduleAtFixedRate(any(Runnable.class), eq(5L), eq(5L), eq(TimeUnit.SECONDS));
        ArgumentCaptor<Runnable> refresh = ArgumentCaptor.forClass(Runnable.class);
        order.verify(scheduler).scheduleWithFixedDelay(refresh.capture(), eq(5L), eq(5L), eq(TimeUnit.SECONDS));
        refresh.getValue().run();
        verify(collectors, times(2)).syncData();
    }

    @Test
    public void rejectsSqlFragmentsInFunctionNamesAndIntervalsBeforeConnecting() {
        SingleGeneralReportDO query = query("kafka_cluster_metrics");
        query.setSelectFun("sum) from kafka_cluster_metrics --");
        CompletionException function = assertThrows(CompletionException.class, () -> new IotDBReportEngine().query(query).join());
        assertTrue(function.getCause() instanceof IllegalArgumentException);

        query.setSelectFun("avg");
        query.setInterval("10s); drop table kafka_cluster_metrics");
        CompletionException interval = assertThrows(CompletionException.class, () -> new IotDBReportEngine().query(query).join());
        assertTrue(interval.getCause() instanceof IllegalArgumentException);
    }

    private SingleGeneralReportDO query(String name) {
        SingleGeneralReportDO query = new SingleGeneralReportDO();
        query.setReportName(name);
        query.setReportType("gauge");
        query.setOrganizationId(1L);
        query.setClustersId(2L);
        query.setStartTime(LocalDateTime.of(2026, 10, 11, 12, 0));
        query.setEndTime(query.getStartTime().plusMinutes(1));
        return query;
    }

    private String sql(SingleGeneralReportDO query) {
        Class<?> model = KafkaConsumerOffsetMetrics.class;
        ReportMeta annotation = model.getAnnotation(ReportMeta.class);
        ReportMetaData metadata = new ReportMetaData();
        metadata.setClazz(model);
        metadata.setTableName(annotation.reportName());
        List<Field> fields = new ArrayList<>(FieldUtils.getAllFieldsList(model));
        Collections.reverse(fields);
        IotDBReportMetaHandler handler = new IotDBReportMetaHandler();
        handler.setReportMeta(metadata);
        handler.setFieldList(fields);
        return new XMLLanguageDriver().createSqlSource(new Configuration(),
            "<script>" + handler.query("gauge") + "</script>", SingleGeneralReportDO.class)
            .getBoundSql(query).getSql();
    }
}
