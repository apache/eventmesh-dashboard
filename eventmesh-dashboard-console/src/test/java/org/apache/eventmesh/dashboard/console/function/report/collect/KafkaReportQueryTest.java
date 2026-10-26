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

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class KafkaReportQueryTest {

    @Test
    public void createsTheServletExceptionHandlerForTheReportHttpEndpoints() throws Exception {
        // 检查实际 MVC 上下文和请求行为，避免把测试绑定到供应商的具体实现类。
        try (ServletFixture fixture = new ServletFixture()) {
            var normal = fixture.report();
            assertTrue(fixture.json.readTree(normal).path("data").isArray(), normal);
            assertEquals(7, fixture.json.readTree(normal).path("data").get(0).path("value").asInt());
            var error = fixture.get("/servlet-probe/error", null);
            int errorCode = new com.lamp.decoration.core.exception.DecorationCustomExceptionResult().getDefaultExceptionResult().getCode();
            assertEquals(errorCode, fixture.json.readTree(error).path("code").asInt());
            assertEquals("servlet-probe-error", fixture.json.readTree(error).path("data").asText());
            var invalid = fixture.mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post("/servlet-probe/validate").contentType(org.springframework.http.MediaType.APPLICATION_JSON).content("{\"key\":\"\"}"))
                .andReturn().getResponse();
            assertEquals(200, invalid.getStatus());
            assertEquals(30100, fixture.json.readTree(invalid.getContentAsString()).path("code").asInt());
        }
    }

    @Test
    public void servletPaginationAndPreHandleFailureDoNotLeakIntoTheNextRequest() throws Exception {
        try (ServletFixture fixture = new ServletFixture()) {
            String page = fixture.get("/servlet-probe/page", "{\"limitPageNum\":2,\"limitSize\":3}");
            assertEquals(2, fixture.json.readTree(page).path("data").get(0).path("page").asInt());
            assertEquals(3, fixture.json.readTree(page).path("data").get(0).path("size").asInt());
            assertNull(com.github.pagehelper.PageHelper.getLocalPage());
            assertNull(com.lamp.decoration.core.DecorationContext.get().getQueryClause());
            var failing = new com.lamp.decoration.core.databases.queryClauseInte.handler.QueryClauseHandler() {
                @Override
                public void handler(String clause) {
                    com.lamp.decoration.core.DecorationContext.get().setQueryClause(
                        new com.lamp.decoration.core.databases.queryClauseInte.QueryClause());
                    com.github.pagehelper.PageHelper.startPage(1, 2);
                    throw new IllegalArgumentException("pagination-pre-handle-error");
                }

                @Override
                public void pageHandler(Object value) {
                    com.github.pagehelper.PageHelper.clearPage();
                }
            };
            com.lamp.decoration.core.databases.queryClauseInte.QueryClauseCentre.QUERY_CLAUSE_HANDLER_LIST.add(failing);
            int errorCode = new com.lamp.decoration.core.exception.DecorationCustomExceptionResult().getDefaultExceptionResult().getCode();
            assertEquals(errorCode, fixture.json.readTree(fixture.get("/servlet-probe/page",
                "{\"limitPageNum\":1,\"limitSize\":2}")).path("code").asInt());
            assertNull(com.github.pagehelper.PageHelper.getLocalPage());
            assertNull(com.lamp.decoration.core.DecorationContext.get().getQueryClause());
            com.lamp.decoration.core.databases.queryClauseInte.QueryClauseCentre.QUERY_CLAUSE_HANDLER_LIST.remove(failing);
            assertTrue(fixture.json.readTree(fixture.report())
                .path("data").get(0).path("cleanContext").asBoolean());
        }
    }

    @org.springframework.boot.test.context.TestConfiguration
    @org.springframework.web.servlet.config.annotation.EnableWebMvc
    @org.springframework.context.annotation.Import({org.apache.eventmesh.dashboard.console.spring.DecorationServletConfiguration.class,
        ReportController.class})
    public static class ServletTestConfiguration implements org.springframework.web.servlet.config.annotation.WebMvcConfigurer {
        @Override
        public void configurePathMatch(org.springframework.web.servlet.config.annotation.PathMatchConfigurer configurer) {
            // 与 Boot 2.7 默认匹配方式一致，过滤器运行时也必须能解析 RequestPath。
            configurer.setPatternParser(new org.springframework.web.util.pattern.PathPatternParser());
        }

        @org.springframework.context.annotation.Bean
        public ServletEndpoint servletEndpoint() {
            return new ServletEndpoint();
        }
    }

    @org.springframework.web.bind.annotation.RestController
    public static class ServletEndpoint {

        @org.springframework.web.bind.annotation.GetMapping("/servlet-probe/page")
        public List<Map<String, Object>> page() {
            var page = com.github.pagehelper.PageHelper.getLocalPage();
            return List.of(Map.of("page", page.getPageNum(), "size", page.getPageSize()));
        }

        @org.springframework.web.bind.annotation.GetMapping("/servlet-probe/error")
        public String error() {
            throw new IllegalArgumentException("servlet-probe-error");
        }

        @org.springframework.web.bind.annotation.PostMapping("/servlet-probe/validate")
        public String validate(@javax.validation.Valid @org.springframework.web.bind.annotation.RequestBody ServletInput input) {
            return input.getKey();
        }

    }

    public static class ServletInput {
        @javax.validation.constraints.NotBlank(message = "key required")
        private String key;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }
    }

    private static final class ServletFixture implements AutoCloseable {
        final org.springframework.web.context.support.AnnotationConfigWebApplicationContext context =
            new org.springframework.web.context.support.AnnotationConfigWebApplicationContext();
        final com.fasterxml.jackson.databind.ObjectMapper json = new com.fasterxml.jackson.databind.ObjectMapper();
        final List<com.lamp.decoration.core.databases.queryClauseInte.handler.QueryClauseHandler> previous =
            new ArrayList<>(com.lamp.decoration.core.databases.queryClauseInte.QueryClauseCentre.QUERY_CLAUSE_HANDLER_LIST);
        final org.springframework.test.web.servlet.MockMvc mvc;

        private ServletFixture() {
            try {
                ReportHandlerManage reports = mock(ReportHandlerManage.class);
                when(reports.queryResultIsMap(any())).thenAnswer(invocation -> Map.of("kafka_cluster_metrics", List.of(Map.of(
                    "value", 7, "cleanContext", com.lamp.decoration.core.DecorationContext.get().getQueryClause() == null))));
                context.addBeanFactoryPostProcessor(factory -> factory.registerSingleton("reportHandlerManage", reports));
                org.springframework.test.context.support.TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context,
                    "decoration.corsEnable=true");
                context.setServletContext(new org.springframework.mock.web.MockServletContext());
                context.register(ServletTestConfiguration.class);
                context.refresh();
                context.getBean(com.lamp.decoration.core.spring.DecorationProperties.class).getConstantConfig().setAll(true);
                mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(context).build();
            } catch (RuntimeException | Error e) {
                close();
                throw e;
            }
        }

        String get(String path, String clause) throws Exception {
            var request = org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(path);
            if (clause != null) {
                request.header("queryClause", clause);
            }
            var response = mvc.perform(request).andReturn().getResponse();
            assertEquals(200, response.getStatus(), response.getContentAsString());
            return response.getContentAsString();
        }

        String report() throws Exception {
            var response = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/report/reportBySingle")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .header("Origin", "https://dashboard-test.example")
                .content("{\"reportName\":\"kafka_cluster_metrics\",\"reportType\":\"gauge\"}"))
                .andReturn().getResponse();
            assertEquals(200, response.getStatus(), response.getContentAsString());
            assertNotNull(response.getHeader("Access-Control-Allow-Origin"));
            assertTrue(java.util.Set.of("*", "https://dashboard-test.example")
                .contains(response.getHeader("Access-Control-Allow-Origin")));
            assertNull(com.github.pagehelper.PageHelper.getLocalPage());
            assertNull(com.lamp.decoration.core.DecorationContext.get().getQueryClause());
            assertNull(com.lamp.decoration.core.DecorationContext.get().getResultObject());
            return response.getContentAsString();
        }

        @Override
        public void close() {
            context.close();
            com.github.pagehelper.PageHelper.clearPage();
            com.lamp.decoration.core.DecorationContext.get().close();
            var handlers = com.lamp.decoration.core.databases.queryClauseInte.QueryClauseCentre.QUERY_CLAUSE_HANDLER_LIST;
            handlers.clear();
            handlers.addAll(previous);
        }
    }

    @Test
    public void queryUsesTheReportNameExpectedByTheSingleEndpoint() {
        ReportEngine engine = mock(ReportEngine.class);
        List<Map<String, Object>> rows = List.of(Map.of("value_broker_count", 2L));
        when(engine.query(any())).thenReturn(CompletableFuture.completedFuture(rows));
        ReportHandlerManage manager = new ReportHandlerManage();
        manager.setReportEngine(engine);
        assertEquals(Map.of("kafka_cluster_metrics", rows), manager.queryResultIsMap(List.of(query("kafka_cluster_metrics"))));
    }

    @Test
    public void batchCopiesSharedFiltersToEachReportRequest() {
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

        // 同一展示类型的两张报表必须分别返回，不能把覆盖行为固化成正确结果。
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
    public void forwardsTheApplicationDataSourceWithoutStartingAnotherLifecycle() {
        ReportHandlerManage manager = new ReportHandlerManage();
        CollectManage collectors = mock(CollectManage.class);
        ReflectionTestUtils.setField(manager, "collectManage", collectors);
        com.alibaba.druid.pool.DruidDataSource source = mock(com.alibaba.druid.pool.DruidDataSource.class);
        manager.setDataSource(source);
        verify(collectors).setDataSource(source);
        verify(collectors, never()).syncData();
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
