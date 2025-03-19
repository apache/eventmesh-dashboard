package org.apache.eventmesh.dashboard.common.enums;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 比如： 通过 topic id 修改配置。 Kafka 只需要 操作 cluster 就可以了。 RocketMQ 需要操作 broker集群，复制集群，
 */
public class OperationRange {

    private static final OperationRange operationRange = new OperationRange();

    private static Map<ClusterType, List<OperationRangeType>> operationRangeListHashMap = new HashMap<>();

    public static OperationRange getInstance() {
        return operationRange;
    }

    private OperationRange() {

    }


    private void setOperationRange(ClusterType clusterType, OperationRangeType operationRangeType) {
        operationRangeListHashMap.computeIfAbsent(clusterType, k -> new ArrayList<>()).add(operationRangeType);
    }

    public List<OperationRangeType> getOperationRangeTypeList(ClusterType clusterType) {
        return operationRangeListHashMap.get(clusterType);
    }

    /**
     * 依据范围查询数据
     */
    public enum OperationRangeType {

        ALL(null),

        CLUSTER(null),

        ALL_RUNTIME(null),

        ONCE_CLUSTER(null),

        RANGE_CLUSTER_CAP(CLUSTER),

        MAIN_SLAVE(CLUSTER),

        RANGE_RUNTIME(MAIN_SLAVE),


        GROUP(RANGE_RUNTIME),

        TOPIC(RANGE_RUNTIME),

        SUBSCRIBER(RANGE_RUNTIME),

        SUBSCRIBER_QUEUE(RANGE_RUNTIME),

        QUEUE(TOPIC),

        ;

        private Map<ClusterType, List<OperationRangeType>> operationRangeListHashMap;

        private int index;

        OperationRangeType() {

        }

        OperationRangeType(OperationRangeType operationRangeType) {
            this.index = Objects.isNull(operationRangeType) ? 0 : operationRangeType.index + 1;
        }
    }


}
