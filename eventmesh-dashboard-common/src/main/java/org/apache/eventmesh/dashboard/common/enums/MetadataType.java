package org.apache.eventmesh.dashboard.common.enums;

import org.apache.eventmesh.dashboard.common.model.metadata.AclMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.ClientMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.ConfigMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.ConnectionMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.GroupMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.TopicMetadata;

public enum MetadataType {


    CONFIG(ConfigMetadata.class),

    RUNTIME(RuntimeMetadata.class),

    TOPIC(TopicMetadata.class),

    GROUP(GroupMetadata.class),

    OFFSET(TopicMetadata.class),

    SUBSCRIBER(GroupMetadata.class),

    CLIENT(ClientMetadata.class, true),

    NET_CONNECT(ConnectionMetadata.class, true),

    USER(ConnectionMetadata.class),

    ACL(AclMetadata.class)
    ;

    private Class<?> metadataClass;

    private boolean readOnly = false;

    MetadataType(Class<?> metadataClass) {
        this.metadataClass = metadataClass;
    }

    MetadataType(Class<?> metadataClass, boolean readOnly) {
        this.metadataClass = metadataClass;
        this.readOnly = readOnly;
    }
}
