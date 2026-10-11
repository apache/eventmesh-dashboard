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

package org.apache.eventmesh.dashboard.console.function.report.model.rocketmq;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.console.function.report.ReportViewType;
import org.apache.eventmesh.dashboard.console.function.report.annotation.ReportMeta;
import org.apache.eventmesh.dashboard.console.function.report.annotation.ReportTag;
import org.apache.eventmesh.dashboard.console.function.report.model.base.RuntimeId.RuntimeFloatValue;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** 存储文件系统使用率，范围 0 到 1；commitlog 汇总为各路径最小值。 */
@Data
@EqualsAndHashCode(callSuper = true)
@ReportMeta(clusterType = ClusterType.STORAGE_ROCKETMQ, reportName = "rocketmq_storage_disk_usage",
    defaultViewType = ReportViewType.GAUGE, tableName = "rocketmq_storage_disk_usage",
    comment = "存储文件系统使用率，范围 0 到 1；commitlog 汇总为各路径最小值")
public class RocketmqStorageDiskUsage extends RuntimeFloatValue {

    @ReportTag
    private String storeType;

    @ReportTag
    private String path;

}
