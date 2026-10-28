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

import org.apache.eventmesh.dashboard.console.function.report.ReportEngine;
import org.apache.eventmesh.dashboard.console.function.report.collect.padding.PaddingService;
import org.apache.eventmesh.dashboard.console.function.report.iotdb.IotDBReportEngine;
import org.apache.eventmesh.dashboard.console.function.report.model.base.ClusterId;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Kafka 样本复制后再归还采集缓冲，沿用现有报表引擎入库。 */
final class KafkaDataSyncHandler extends DataSyncHandler {

    private final PaddingService paddingService;

    private final ReportEngine reportEngine;

    KafkaDataSyncHandler(PaddingService paddingService, ReportEngine reportEngine) {
        this.paddingService = paddingService;
        this.reportEngine = reportEngine;
    }

    @Override
    public void padding(DataSyncHandlerWrapper wrapper) {
        Map<Class<?>, List<Object>> samples = new HashMap<>();
        for (RestoreData result : wrapper.restoreDataList) {
            result.getDataMap().forEach((type, values) -> samples.computeIfAbsent(type, key -> new ArrayList<>()).addAll(values));
            result.restore();
        }
        samples.values().forEach(values -> values.forEach(value -> paddingService.padding((ClusterId) value)));
        if (reportEngine instanceof IotDBReportEngine iotdb) {
            new KafkaReportWriter(iotdb).insert(samples);
        } else {
            reportEngine.batchInsertByClass(samples);
        }
    }
}
