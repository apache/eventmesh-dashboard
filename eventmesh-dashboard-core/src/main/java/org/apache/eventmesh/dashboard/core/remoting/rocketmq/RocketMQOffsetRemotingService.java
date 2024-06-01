package org.apache.eventmesh.dashboard.core.remoting.rocketmq;

import org.apache.eventmesh.dashboard.common.model.remoting.offset.GetOffsetRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.offset.GetOffsetResult;
import org.apache.eventmesh.dashboard.common.model.remoting.offset.ResetOffsetRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.offset.ResetOffsetResult;
import org.apache.eventmesh.dashboard.service.remoting.OffsetRemotingService;

public class RocketMQOffsetRemotingService extends AbstractRocketMQRemotingService implements OffsetRemotingService {
    @Override
    public GetOffsetResult getOffset(GetOffsetRequest getOffsetRequest) {
        return null;
    }

    @Override
    public ResetOffsetResult resetOffset(ResetOffsetRequest resetOffsetRequest) {
        return null;
    }
}
