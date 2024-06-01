package org.apache.eventmesh.dashboard.core.remoting.rocketmq;

import org.apache.eventmesh.dashboard.common.model.metadata.ClientMetadata;
import org.apache.eventmesh.dashboard.service.remoting.ClientRemotingService;

import java.util.List;

public class RocketMQClientRemotingService extends AbstractRocketMQRemotingService implements ClientRemotingService {
    @Override
    public List<ClientMetadata> getClientList() {
        return null;
    }
}
