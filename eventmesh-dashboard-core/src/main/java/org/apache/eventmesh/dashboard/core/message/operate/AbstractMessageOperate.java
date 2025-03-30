package org.apache.eventmesh.dashboard.core.message.operate;

import org.apache.eventmesh.dashboard.common.model.base.BaseSyncBase;
import org.apache.eventmesh.dashboard.core.function.SDK.config.CreateSDKConfig;
import org.apache.eventmesh.dashboard.core.message.model.AbstractMessageDTO;

import lombok.Setter;


@Setter
public abstract class AbstractMessageOperate<CL, C extends CreateSDKConfig, DTO extends AbstractMessageDTO> {

    private DTO abstractMessageDTO;


    /**
     * TODO  不负责， 节点的改变造成的问题
     */
    private CL client;

    public abstract CreateSDKConfig createSDKConfig(C createSDKConfig, BaseSyncBase baseSyncBase);


}
