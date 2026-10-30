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

import java.util.List;
import java.util.Map;

/** Kafka 写入复用公共批次处理，保留 IoTDB 参数绑定的时间和值语义。 */
final class KafkaDataSyncHandler extends DataSyncHandler {

    KafkaDataSyncHandler(PaddingService paddingService, ReportEngine reportEngine) {
        this.setPaddingService(paddingService);
        this.setReportEngine(reportEngine);
    }

    @Override
    protected void write(ReportEngine engine, Map<Class<?>, List<Object>> samples) {
        if (engine instanceof IotDBReportEngine iotdb) {
            new KafkaReportWriter(iotdb).insert(samples);
        } else {
            super.write(engine, samples);
        }
    }
}
