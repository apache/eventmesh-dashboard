package org.apache.eventmesh.dashboard.core.cluster;

import org.apache.eventmesh.dashboard.common.model.metadata.ConfigMetadata;

import java.util.List;

public class BaseDataDO<RE, CM> {

    private Object resource;

    private List<ConfigMetadata> configMetadata;

}
