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

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.Data;


/**
 * 组件类型，report名，数据类型，类名（因为采集模块），表名，默认展示类型，可以展示类型，
 *
 */
@Data
public class ReportMetaData {


    private ClusterType clusterType;

    /**
     *
     */
    private String reportName;


    private String dataType;


    private Class<?> clazz;


    private String tableName;

    private String comment;

    /**
     * counter histogram gauge
     */
    private ReportViewType reportViewType;


    private List<ReportViewType> reportViewTypes;


    private Set<String> alias;


    private String valueType;


    private boolean aggregation = false;


    private Map<String,AggregationClass> aggregationClasses;


    private List<Field> fieldList;

    private Map<String,Field> aggregationFieldMap;


    @Data
    public static class AggregationClass{

        private String alias;

        private Field field;

    }

}
