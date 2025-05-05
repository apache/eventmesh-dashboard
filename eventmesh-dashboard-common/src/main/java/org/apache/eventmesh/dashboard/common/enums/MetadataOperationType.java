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


package org.apache.eventmesh.dashboard.common.enums;

import static org.apache.eventmesh.dashboard.common.enums.MetadataType.CONFIG;
import static org.apache.eventmesh.dashboard.common.enums.MetadataType.GROUP;
import static org.apache.eventmesh.dashboard.common.enums.MetadataType.OFFSET;
import static org.apache.eventmesh.dashboard.common.enums.MetadataType.RUNTIME;
import static org.apache.eventmesh.dashboard.common.enums.MetadataType.TOPIC;
import static org.apache.eventmesh.dashboard.common.enums.OperationRange.OperationRangeType.ALL;
import static org.apache.eventmesh.dashboard.common.enums.OperationRange.OperationRangeType.ALL_RUNTIME;
import static org.apache.eventmesh.dashboard.common.enums.OperationRange.OperationRangeType.CLUSTER;

import org.apache.eventmesh.dashboard.common.enums.OperationRange.OperationRangeType;

import java.util.HashMap;
import java.util.Map;

/**
 * 操作在不同的集群，有着不同的行为. 只是规范了，需要把操作时，需要定义操作范围。 在集群修改的时候，需要把整个集群中所有节点的配置显示出来，才修改。 所以查看维度。
 */
@Deprecated
public enum MetadataOperationType {

    RUNTIME_CONFIG_UPDATE(RUNTIME, CONFIG,
        MapWrapper.build().set(ClusterType.EVENTMESH_RUNTIME, CLUSTER, ALL_RUNTIME)
            .set(ClusterType.STORAGE_ROCKETMQ_BROKER, CLUSTER, ALL_RUNTIME)
            .set(ClusterType.STORAGE_KAFKA_BROKER, CLUSTER, ALL_RUNTIME)),

    RUNTIME_CONFIG_QUEUE(RUNTIME, CONFIG, RUNTIME_CONFIG_UPDATE.mapWrapper),

    GROUP_CONFIG_UPDATE(GROUP, CONFIG, RUNTIME_CONFIG_UPDATE.toClone().setClusterType(ClusterType.STORAGE_KAFKA_BROKER)
        .set(ALL, OperationRangeType.ONCE_CLUSTER)),

    GROUP_CONFIG_QUEUE(GROUP, CONFIG, GROUP_CONFIG_UPDATE.mapWrapper),

    TOPIC_CONFIG_UPDATE(TOPIC, CONFIG, GROUP_CONFIG_UPDATE.mapWrapper),

    TOPIC_CONFIG_QUEUE(TOPIC, CONFIG, GROUP_CONFIG_UPDATE.mapWrapper),

    TOPIC_CREATE(TOPIC, GROUP_CONFIG_UPDATE.mapWrapper),

    TOPIC_DELETE(TOPIC, GROUP_CONFIG_UPDATE.mapWrapper),

    TOPIC_QUEUE(TOPIC, GROUP_CONFIG_UPDATE.mapWrapper),

    OFFSET_RESET(OFFSET, GROUP_CONFIG_UPDATE.mapWrapper);


    private MapWrapper mapWrapper;

    private MetadataType target;

    /**
     *
     */
    private MetadataType dataType;


    private String operationType;

    MetadataOperationType(MetadataType target, MapWrapper mapWrapper) {
        this(target, target, mapWrapper);
    }

    MetadataOperationType(MetadataType target, MetadataType dataType, MapWrapper mapWrapper) {
        this.mapWrapper = mapWrapper;
        this.target = target;
        this.dataType = dataType;
        this.operationType = name();
    }

    public MapWrapper toClone() {
        return new MapWrapper(mapWrapper);
    }

    public OperationRangeType getOperationRangeType(ClusterType clusterType, OperationRangeType operationRangeType) {
        return this.mapWrapper.operationRangeTypeListHashMap.get(clusterType).get(operationRangeType);
    }

    static class MapWrapper {

        static MapWrapper build() {
            return new MapWrapper();
        }

        private Map<ClusterType, Map<OperationRangeType, OperationRangeType>> operationRangeTypeListHashMap = new HashMap<>();

        private Map<OperationRangeType, OperationRangeType> currentMap = new HashMap<>();

        MapWrapper() {
        }

        MapWrapper(MapWrapper mapWrapper) {
            operationRangeTypeListHashMap.putAll(mapWrapper.operationRangeTypeListHashMap);
        }

        MapWrapper set(ClusterType clusterType, OperationRangeType... operationRangeTypes) {
            this.setClusterType(clusterType);
            for (OperationRangeType operationRangeType : operationRangeTypes) {
                this.set(operationRangeType, operationRangeType);
            }
            return this;
        }

        MapWrapper set(OperationRangeType operationRangeType, OperationRangeType operationRangeTypes) {
            this.currentMap.put(operationRangeType, operationRangeType);
            return this;
        }

        MapWrapper setClusterType(ClusterType clusterType) {
            this.currentMap = this.operationRangeTypeListHashMap.computeIfAbsent(clusterType, k -> new HashMap<>());
            return this;
        }


    }

}
