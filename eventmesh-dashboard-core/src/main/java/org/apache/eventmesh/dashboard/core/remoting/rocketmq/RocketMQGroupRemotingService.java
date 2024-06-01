package org.apache.eventmesh.dashboard.core.remoting.rocketmq;

import org.apache.eventmesh.dashboard.common.model.remoting.group.GetGroupResult;
import org.apache.eventmesh.dashboard.common.model.remoting.group.GetGroupsRequest;
import org.apache.eventmesh.dashboard.service.remoting.GroupRemotingService;

public class RocketMQGroupRemotingService extends AbstractRocketMQRemotingService implements GroupRemotingService {
    @Override
    public GetGroupResult getAllGroups(GetGroupsRequest getGroupsRequest) {
        return null;
    }
}
