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

package org.apache.eventmesh.dashboard.console.function.report;

import org.apache.eventmesh.dashboard.common.util.ClasspathScanner;
import org.apache.eventmesh.dashboard.console.function.report.ReportConfig.ReportEngineConfig;
import org.apache.eventmesh.dashboard.console.function.report.annotation.Aggregation;
import org.apache.eventmesh.dashboard.console.function.report.annotation.ReportMeta;
import org.apache.eventmesh.dashboard.console.function.report.annotation.ReportMetaData;
import org.apache.eventmesh.dashboard.console.function.report.collect.CollectManage;
import org.apache.eventmesh.dashboard.console.function.report.collect.MetadataDataManage;
import org.apache.eventmesh.dashboard.console.function.report.iotdb.IotDBReportEngine;
import org.apache.eventmesh.dashboard.console.function.report.model.SingleGeneralReportDO;
import org.apache.eventmesh.dashboard.console.function.report.model.base.OrganizationId;
import org.apache.eventmesh.dashboard.console.function.report.model.base.Time;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.google.common.base.CaseFormat;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * TODO 功能点
 * 1. 同时提供总量 与 分组的统计
 * 2. 同时提供 量与百分比, value 后面自带百分比？
 * 3. 提供排序
 * 4. 过滤。默认过滤掉。0。 可以指定过滤掉值的
 *  1. 比如 topic ，消费组
 *  2. 给 过滤总量值
 * 5. 指定维度
 *      1. message_type
 *      2. protocol_type
 *      3. request_code
 *      4. response_code
 *      5. invocation_status
 *      6. name
 * 6. 时间维度 15秒，分，15分，30分，1小时，3小时，6小时，12小时，天，月，
 * 7. 分组：时间分组
 *  2. 按照 id 与。 id 维度是固定的
 *  3. 按照 类型维度
 *  4.
 * 8. 支持批量获得，两个获得
 *  1. 批量与单个是否共用一个 接口？
 *  2. 如果同时支持批量与单个，那么 前端需要提供连个
 */
@Slf4j
public class ReportHandlerManage {

    private static final Map<String, Class<?>> engineClasses = new HashMap<>();

    static {
        engineClasses.put("iot", IotDBReportEngine.class);
    }

    private final ScheduledExecutorService scheduledExecutorService = new ScheduledThreadPoolExecutor(10);

    private Map<String, ReportEngine> reportEngineMap = new HashMap<>();

    @Setter
    private ReportEngine reportEngine;

    @Getter
    private Map<String, ReportMetaData> reportMetaDataMap = new HashMap<>();

    @Getter
    private Map<String,ReportMetaData> aggregationMetaDataMap = new HashMap<>();

    private final Map<Class<?>, String> clazzToTableName = new HashMap<>();

    private final CollectManage collectManage = new CollectManage();

    private final MetadataDataManage metadataDataManage = new MetadataDataManage();


    @Setter
    private ReportConfig reportConfig;

    public void init() {
        ClasspathScanner classpathScanner =
            ClasspathScanner.builder().base(ReportHandlerManage.class).subPath("/model/**").build();
        try {
            List<Class<?>> classList = classpathScanner.getClazz();
            classList.forEach(this::handler);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        this.handlerConfig();

        scheduledExecutorService.scheduleAtFixedRate(this.collectManage::request, 5, 5, TimeUnit.SECONDS);

        scheduledExecutorService.scheduleAtFixedRate(this::handlerData, 5, 5, TimeUnit.SECONDS);

        scheduledExecutorService.scheduleAtFixedRate(this.metadataDataManage::syncData, 5, 100, TimeUnit.MINUTES);

        this.buildDeleteDataTask();
        this.ddlHandler();
    }

    private void buildDeleteDataTask() {
        LocalDate current = LocalDate.now().plusDays(1);
        LocalDateTime nextTime = LocalDateTime.of(current, LocalTime.of(3, 0));
        long executeTime = nextTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        scheduledExecutorService.scheduleAtFixedRate(this::deleteDataTask, executeTime, 1000 * 60 * 60 * 24, TimeUnit.MINUTES);
    }

    private void deleteDataTask() {
        this.reportEngineMap.forEach((k, v) -> {
            try {
                long start = System.currentTimeMillis();
                v.deleteData();
                long end = System.currentTimeMillis();
                log.info("{} delete data cost {} ms", k, end - start);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        });
    }

    private void handlerConfig() {
        this.reportEngine = this.createEngine(this.reportConfig.getDefaultConfig());
        if (Objects.isNull(this.reportConfig.getReportEngineConfigList())) {
            return;
        }
        this.reportConfig.getReportEngineConfigList().forEach(this::createEngine);
    }

    private AbstractReportEngine createEngine(ReportEngineConfig reportEngineConfig) {
        if (Objects.isNull(reportEngineConfig)) {
            return null;
        }
        Class<?> clazz = engineClasses.get(reportEngineConfig.getEngineType());
        if (Objects.isNull(clazz)) {

        }
        try {
            AbstractReportEngine reportEngine = (AbstractReportEngine) clazz.newInstance();
            reportEngine.setReportEngineConfig(reportEngineConfig);
            reportEngineMap.put(reportEngineConfig.getName(), reportEngine);
            return reportEngine;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void ddlHandler() {
        if (!this.reportConfig.isInitAllTables()) {
            return;
        }
        this.reportEngineMap.forEach((k, v) -> {
            v.createReport("*");
        });
    }

    /**
     * 后期再支持多存储与多数据
     */
    private void handlerData() {
        List<Time> timeData = this.collectManage.getData();
        if (CollectionUtils.isEmpty(timeData)) {
            return;
        }
        Map<String, List<Object>> dataMap = new HashMap<>(timeData.size());
        timeData.forEach(t -> {
            metadataDataManage.supplement((OrganizationId) t);
            List<Object> objectList = dataMap.get(t.getClass());
            if (Objects.isNull(objectList)) {
                objectList = new ArrayList<>();
                dataMap.put(this.clazzToTableName.get(t.getClass()), objectList);
            }
            objectList.add(t.getTime());
        });
        this.reportEngine.batchInsert(dataMap);
    }

    private void handler(Class<?> clazz) {
        ReportMeta reportMeta = clazz.getAnnotation(ReportMeta.class);
        if (Objects.isNull(reportMeta)) {
            return;
        }

        String className = clazz.getSimpleName();
        String caseName = CaseFormat.LOWER_CAMEL.to(com.google.common.base.CaseFormat.LOWER_UNDERSCORE, className);
        ReportMetaData reportMetaData = new ReportMetaData();

        reportMetaData.setReportName(Objects.isNull(reportMeta.reportName()) ? caseName : reportMeta.reportName());
        reportMetaData.setClusterType(reportMeta.clusterType());
        reportMetaData.setReportViewType(reportMeta.defaultViewType());
        reportMetaData.setReportViewTypes(List.of(reportMeta.reportViewTypes()));
        reportMetaData.setTableName(Objects.isNull(reportMeta.tableName()) ? caseName : reportMeta.tableName());
        reportMetaData.setComment(reportMeta.comment());
        reportMetaData.setAlias(Set.of(reportMeta.alias()));
        if (StringUtils.isEmpty(reportMetaData.getTableName())) {
            reportMetaData.setTableName(reportMeta.reportName());
        }
        reportMetaData.setValueType(clazz.getSimpleName().contains("Long") ? "Long" : "Float");
        Aggregation[] aggregationArrays = clazz.getAnnotationsByType(Aggregation.class);
        if(ArrayUtils.isNotEmpty(aggregationArrays)) {
            Map<String,Field> aggregationFieldMap = new HashMap<>(aggregationArrays.length);
            Arrays.stream(aggregationArrays).forEach(aggregation -> {
                Field field = aggregationFieldMap.get(aggregation.fieldName());
                field.setAccessible(true);
                aggregationFieldMap.put(aggregation.reportName(),field);
                this.aggregationMetaDataMap.put(aggregation.reportName(), reportMetaData);

            });
            reportMetaData.setAggregationFieldMap(aggregationFieldMap);
        }
//        List<Field> aggregationList = FieldUtils.getFieldsListWithAnnotation(clazz, Aggregation.class);
//        if (!aggregationList.isEmpty()) {
//            reportMetaData.setAggregation(true);
//            Map<String, AggregationClass> aggregationClassMap = new HashMap<>();
//            reportMetaData.setAggregationClasses(aggregationClassMap);
//            aggregationList.forEach(aggregation -> {
//                Aggregation aggregationAnnotation = aggregation.getAnnotation(Aggregation.class);
//
//                AggregationClass aggregationClass = new AggregationClass();
//                aggregationClass.setAlias(aggregationAnnotation.value());
//                aggregationClass.setField(aggregation);
//                aggregationClassMap.put(aggregationClass.getAlias(), aggregationClass);
//            });
//        }

        List<Field> list = new ArrayList<>(FieldUtils.getAllFieldsList(clazz));
        Collections.reverse(list);
        reportMetaData.setFieldList(list);
        reportMetaData.setClazz(clazz);
        this.reportEngine.createReportHandler(reportMetaData, list);
        this.reportMetaDataMap.put(reportMetaData.getReportName(), reportMetaData);
        this.clazzToTableName.put(clazz, reportMetaData.getTableName());
    }

    /**
     * @return Map<String viewName, List < Map < String, Object>>> dataList  Map<String, Object> data
     */
    public Map<String, List<Map<String, Object>>> queryResultIsMap(List<SingleGeneralReportDO> singleGeneralReportDOList) {
        Map<String, CompletableFuture<List<Map<String, Object>>>> completableFutures = new HashMap<>(singleGeneralReportDOList.size());
        singleGeneralReportDOList.forEach(reportDO -> {
            CompletableFuture<List<Map<String, Object>>> completableFuture = reportEngine.query(reportDO);
            completableFutures.put(reportDO.getReportType(), completableFuture);
        });
        Map<String, List<Map<String, Object>>> resultMap = new HashMap<>(singleGeneralReportDOList.size());
        completableFutures.forEach((key, completableFuture) -> {
            try {
                List<Map<String, Object>> singleData = completableFuture.get();
                resultMap.put(key, singleData);
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
        return resultMap;
    }


}
