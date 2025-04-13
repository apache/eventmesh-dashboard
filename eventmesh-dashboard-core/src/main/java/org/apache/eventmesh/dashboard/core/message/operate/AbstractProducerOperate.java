package org.apache.eventmesh.dashboard.core.message.operate;

import org.apache.eventmesh.dashboard.core.function.SDK.config.CreateSDKConfig;
import org.apache.eventmesh.dashboard.core.message.model.ProducerDTO;


public abstract class AbstractProducerOperate<CL, C extends CreateSDKConfig>
    extends AbstractMessageOperate<CL, C, ProducerDTO> implements ProducerOperate {

    /**
     * 是否模拟数据
     */
    private boolean simulate;


}
