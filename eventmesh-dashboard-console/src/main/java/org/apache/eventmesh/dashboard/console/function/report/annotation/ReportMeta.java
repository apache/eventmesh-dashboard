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

package org.apache.eventmesh.dashboard.console.function.report.annotation;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.console.function.report.ReportViewType;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 *  id 为 tag
 *  time 为 time
 *  value 开头为 数据
 *
 *
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ReportMeta {

    ClusterType clusterType();

    String reportName();

    ReportViewType defaultViewType();

    ReportViewType[] reportViewTypes() default {};

    String tableName();

    String comment();

}
