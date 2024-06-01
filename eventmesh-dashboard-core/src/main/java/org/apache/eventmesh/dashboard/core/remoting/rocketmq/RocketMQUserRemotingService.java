package org.apache.eventmesh.dashboard.core.remoting.rocketmq;

import org.apache.eventmesh.dashboard.common.model.remoting.user.CreateUserRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.user.DeleterUserRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.user.GetUserRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.user.GetUserResult;
import org.apache.eventmesh.dashboard.service.remoting.UserRemotingService;

public class RocketMQUserRemotingService extends AbstractRocketMQRemotingService implements UserRemotingService {
    @Override
    public CreateUserRequest createInstanceUser(CreateUserRequest request) {
        return null;
    }

    @Override
    public DeleterUserRequest deleteInstanceUser(DeleterUserRequest request) {
        return null;
    }

    @Override
    public GetUserResult getInstanceUser(GetUserRequest request) {
        return null;
    }
}
