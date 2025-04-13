package org.apache.eventmesh.dashboard.common.model.metadata;

import org.apache.eventmesh.dashboard.common.model.base.BaseRuntimeIdBase;

public class QueueMetadata extends BaseRuntimeIdBase {

    private String topicName;

    private String queueName;


    @Override
    public String nodeUnique() {
        return this.topicName + "-" + this.queueName;
    }

}
