package org.apache.eventmesh.dashboard.core.message.model;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.model.metadata.QueueMetadata;

import java.util.List;

import lombok.Data;


@Data
public class AbstractMessageDTO {

    private Long clusterId;

    private ClusterType clusterType;

    private String consumerGroup;

    private String topic;

    private String group;

    private boolean simulate;

    private List<QueueMetadata> queueMetadataList;

}
