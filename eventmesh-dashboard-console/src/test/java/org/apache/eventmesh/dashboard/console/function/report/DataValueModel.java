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

import org.apache.eventmesh.dashboard.console.function.report.elasticsearch.DataModel;
import org.apache.eventmesh.dashboard.console.function.report.elasticsearch.DataModel.DataModelBuilder;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 峰值点，峰值开始时间，结束时间，上升开始时间，上升结束时间，上升坡度，下降坡度，下降开始时间，下降结束时间 几次峰 value的类型，累加型，有规律随机性 先实现累加型
 */
public class DataValueModel {


    public Map<String, Object> value(List<DataModel> list) {
        LocalDateTime localDateTime = LocalDateTime.now();
        LocalDateTime minusDays = localDateTime.minusDays(1);
        long seconds = ChronoUnit.SECONDS.between(minusDays, localDateTime);
        int cont = (int) (seconds / 5);
        Map<String, Object> builders = new HashMap<>(list.size() * cont);
        long key = minusDays.atZone(ZoneOffset.UTC).toInstant().toEpochMilli();
        list.forEach((value) -> {
            Random random = new Random();
            int count = 0;
            for (int i = 0; i < cont; i++) {
                DataModelBuilder dataModelBuilder = DataModel.builder();
                dataModelBuilder.organizationId(value.getOrganizationId())
                    .organizationName(value.getOrganizationName())
                    .clusterId(value.getClusterId())
                    .clusterName(value.getClusterName())
                    .runtimeId(value.getRuntimeId())
                    .runtimeName(value.getRuntimeName())
                    .runtimeType(value.getRuntimeType())
                    .topicId(value.getTopicId())
                    .topicName(value.getTopicName())
                    .topicType(value.getTopicType())
                    .groupId(value.getGroupId())
                    .groupName(value.getGroupName())
                    .subscribeId(value.getSubscribeId());
                dataModelBuilder.recordTime(minusDays.plusSeconds(i));
                dataModelBuilder.value("" + count++);
                builders.put((key + i) + "", dataModelBuilder.build());
            }
        });
        return builders;
    }
}
