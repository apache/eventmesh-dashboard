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

package org.apache.eventmesh.dashboard.console.function.report.collect.exporter;

import org.apache.eventmesh.dashboard.common.enums.ClusterSyncMetadataEnum;
import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.model.metadata.ClusterMetadata;
import org.apache.eventmesh.dashboard.console.function.report.annotation.ReportMetaData;
import org.apache.eventmesh.dashboard.console.function.report.annotation.ReportMetaData.AggregationClass;
import org.apache.eventmesh.dashboard.console.function.report.model.base.ClusterId;
import org.apache.eventmesh.dashboard.console.function.report.model.base.OrganizationId;
import org.apache.eventmesh.dashboard.console.function.report.model.base.RuntimeId;
import org.apache.eventmesh.dashboard.console.function.report.model.base.Time;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.alibaba.fastjson.JSONObject;

import lombok.Data;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Setter
@Slf4j
public class CollectExporter {


    protected ClusterType clusterType;
    protected ClusterMetadata clusterMetadata;
    protected List<Time> times = new ArrayList<>();
    protected List<Time> standby = new ArrayList<>();
    private Map<String, ReportMetaData> reportMetaDataMap = new HashMap<>();
    private Map<String, ReportMetaData> aggregationMetaDataMap = new HashMap<>();
    private Map<String, AggregationWrapper> aggregationWrapperMap = new HashMap<>();
    private Map<String, String> fieldMapper = new HashMap<>();

    private Map<String, Map<String, Field>> objectFieldMapper = new HashMap<>();

    private HttpClient client = HttpClient.newHttpClient();

    private ClusterSyncMetadataEnum clusterSyncMetadataEnum;

    @Setter
    private String url;

    public static LocalDateTime millisToLocalDateTime(long millis) {
        return Instant.ofEpochMilli(millis)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime();
    }

    public void init() {
        reportMetaDataMap.forEach((clusterMetadata, reportMetaData) -> {
            if (!reportMetaData.isAggregation()) {
                return;
            }
            AggregationWrapper aggregationWrapper = new AggregationWrapper();
            aggregationWrapper.setReportMetaData(reportMetaData);
            aggregationWrapper.setName(clusterMetadata);
            this.aggregationWrapperMap.put(clusterMetadata, aggregationWrapper);

            reportMetaData.getAggregationClasses().forEach((key, value) -> {
                this.aggregationWrapperMap.put(key, aggregationWrapper);
            });
        });
        this.aggregationMetaDataMap.forEach((reportMetaName, reportMetaData) -> {
            this.aggregationWrapperMap.computeIfAbsent(reportMetaData.getReportName(), (k) -> {
                AggregationWrapper aggregationWrapper = new AggregationWrapper();
                aggregationWrapper.setReportMetaData(reportMetaData);
                aggregationWrapper.setName(reportMetaData.getReportName());
                return aggregationWrapper;
            });
        });
        this.clusterSyncMetadataEnum = ClusterSyncMetadataEnum.valueOf(this.clusterMetadata.getClusterType().name());
    }

    public List<Time> collect() {
        List<Time> list;
        this.standby.clear();
        synchronized (this) {
            list = this.times;
            this.times = this.standby;
            this.standby = list;
        }
        return list;
    }

    public void request() {
        try {
            this.aggregationWrapperMap.values().forEach(aggregationWrapper -> {
                aggregationWrapper.organization = null;
            });
            String data = this.exporterRequest();
            for (String data1 : StringUtils.split(data, (char) 10)) {
                if (data1.startsWith("#")) {
                    continue;
                }
                this.handler(data1);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }


    }

    public String exporterRequest() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(this.url)).build();
        HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
        return response.body();
    }

    public void handler(String data) {
        String[] datas = StringUtils.split(data, (char) 32);
        int index = 0;

        String data1 = datas[index++];
        String key;
        JSONObject jsonObject = null;
        if (data1.indexOf(123) > -1) {
            key = data1.substring(0, data1.indexOf(123));
            final JSONObject finalJsonObject = new JSONObject();
            String metaDataString = data1.substring(data1.indexOf(123) + 1, data1.length() - 1);
            List.of(StringUtils.split(metaDataString, ',')).forEach((value) -> {
                String newValue = value.trim();
                if (Objects.equals(newValue, ",")) {
                    return;
                }
                int stringIndex = newValue.indexOf("=");
                if (stringIndex == -1) {
                    return;
                }
                finalJsonObject.put(newValue.substring(0, stringIndex).trim(), newValue.substring(stringIndex + 1).trim());
            });
            jsonObject = finalJsonObject;
        } else {
            key = data1;
        }
        String value = datas[index++];
        LocalDateTime time = datas.length == 2 ? LocalDateTime.now() : millisToLocalDateTime(Long.parseLong(datas[index++]));
        // 通过 ClusterType ， key 得到 object
        //
        OrganizationId object = this.buildObject(key, jsonObject, value, time);
        if (Objects.isNull(object)) {
            return;
        }
        this.times.add(object);

    }

    public ClusterId buildObject(String key, JSONObject jsonObject, String value, LocalDateTime time) {
        ReportMetaData reportMetaData = this.reportMetaDataMap.get(key);

        try {
            AggregationWrapper aggregationWrapper = null;
            if (Objects.isNull(reportMetaData)) {
                aggregationWrapper = this.aggregationWrapperMap.get(key);
                if (Objects.isNull(aggregationWrapper)) {
                    log.error("reportMetaData does not exist。 key:{},value:{},time:{}", key, value, time);
                    return null;
                }
                if (Objects.nonNull(aggregationWrapper.getOrganization())) {
                    Field field = aggregationWrapper.getReportMetaData().getAggregationFieldMap().get(key);
                    field.set(aggregationWrapper.getOrganization(), value);
                    return null;
                }
            }

            Map<String, Field> stringFieldMapper = this.createStringFieldMapper(reportMetaData);

            ClusterId object = this.createObject(stringFieldMapper, reportMetaData, time, jsonObject);
            if (Objects.nonNull(aggregationWrapper)) {
                aggregationWrapper.organization = object;
            }
            return object;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }

    }

    private ClusterId createObject(Map<String, Field> stringFieldMapper, ReportMetaData reportMetaData, LocalDateTime time, JSONObject jsonObject)
        throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Class<?> clazz = reportMetaData.getClazz();
        ClusterId object = (ClusterId) clazz.getDeclaredConstructor().newInstance();
        object.setOrganizationId(this.clusterMetadata.getOrganizationId());
        /*
            TODO 目前只有 rocketmq 架构 的 普罗米修斯 采集 多 cluster 信息
                 无法通过架构去决定  普罗米修斯 exporter 是采集单集群，还是多集群
                 硬编码
                 硬编码
         */
        if (Objects.equals(this.clusterMetadata.getClusterType(), ClusterType.STORAGE_ROCKETMQ_CLUSTER)
            || Objects.equals(this.clusterMetadata.getClusterType(), ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE)
            || Objects.equals(this.clusterMetadata.getClusterType(), ClusterType.STORAGE_ROCKETMQ_BROKER_RAFT)
            || Objects.equals(this.clusterMetadata.getClusterType(), ClusterType.STORAGE_ROCKETMQ_NAMESERVER)
        ) {
            object.setClustersName(jsonObject.getString("cluster"));
            if (object instanceof RuntimeId runtimeId) {
                if (jsonObject.containsKey("broker")) {
                    runtimeId.setRuntimeName(jsonObject.getString("broker"));
                } else if (jsonObject.containsKey("brokerIP")) {
                    runtimeId.setRuntimeName(jsonObject.getString("brokerIP"));
                }
            }
        } else {
            object.setClustersId(this.clusterMetadata.getClusterId());
            object.setClustersName(this.clusterMetadata.getClusterName());
        }
        object.setTime(time);
        stringFieldMapper.forEach((fieldName, field) -> {
            String fieldValue = jsonObject.getString(fieldName);
            if (Objects.isNull(fieldValue)) {
                return;
            }
            try {
                field.set(object, fieldValue);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        });
        return object;
    }

    private Map<String, Field> createStringFieldMapper(ReportMetaData reportMetaData) {
        Map<String, Field> stringFieldMapper = this.objectFieldMapper.get(reportMetaData.getReportName());
        if (Objects.isNull(stringFieldMapper)) {
            stringFieldMapper = new HashMap<>();
            this.objectFieldMapper.put(reportMetaData.getReportName(), stringFieldMapper);
            Map<String, Field> finalStringFieldMapper = stringFieldMapper;
            reportMetaData.getFieldList().forEach(field -> {
                field.setAccessible(true);
                String fieldKey = this.fieldMapper.get(field.getName());
                if (Objects.isNull(fieldKey)) {
                    finalStringFieldMapper.put(field.getName(), field);
                } else {
                    finalStringFieldMapper.put(fieldKey, field);
                }
            });
        }
        return stringFieldMapper;
    }

    @Data
    static class AggregationWrapper {

        private String name;

        private ReportMetaData reportMetaData;

        private Map<String, AggregationClass> aggregationClassMap;

        private Map<String, Field> fieldMap;

        private OrganizationId organization;
    }

}
