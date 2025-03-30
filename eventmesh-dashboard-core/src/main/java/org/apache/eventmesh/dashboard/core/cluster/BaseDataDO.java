package org.apache.eventmesh.dashboard.core.cluster;

import org.apache.eventmesh.dashboard.common.model.metadata.ConfigMetadata;

import java.util.List;

public class BaseDataDO<RE, CM> {

    /**
     *  RE 资源信息
     */
    private Object resource;

    /**
     * CM 配置参数
     */
    private List<ConfigMetadata> configMetadata;



}
