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
import org.apache.eventmesh.dashboard.console.function.report.annotation.ReportMeta;
import org.apache.eventmesh.dashboard.console.function.report.model.SingleGeneralReportDO;

import org.apache.commons.lang3.reflect.FieldUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import lombok.Setter;

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
public class ReportHandlerManage {


    @Setter
    private ReportEngine reportEngine;

    public void init() {
        ClasspathScanner classpathScanner =
            ClasspathScanner.builder().base(ReportHandlerManage.class).subPath("/model/**").build();
        try {
            List<Class<?>> classList = classpathScanner.getClazz();
            classList.forEach(this::handler);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void handler(Class<?> clazz) {
        ReportMeta reportMeta = clazz.getAnnotation(ReportMeta.class);
        if (Objects.isNull(reportMeta)) {
            return;
        }
        List<Field> list = new ArrayList<>(FieldUtils.getAllFieldsList(clazz));
        Collections.reverse(list);
        this.reportEngine.createReportHandler(reportMeta, list);
    }

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
