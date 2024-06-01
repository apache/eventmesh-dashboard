package org.apache.eventmesh.dashboard.core.remoting.rocketmq;

import org.apache.eventmesh.dashboard.common.model.remoting.BaseGlobalResult;
import org.apache.eventmesh.dashboard.common.model.remoting.config.AddConfigRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.config.GetConfigRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.topic.GetTopicsRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.topic.GetTopicsResult;
import org.apache.eventmesh.dashboard.service.remoting.ConfigRemotingService;

public class RocketMQConfigRemotingService extends AbstractRocketMQRemotingService implements ConfigRemotingService {
    @Override
    public BaseGlobalResult addConfig(AddConfigRequest addConfigRequest) {
        return null;
    }

    @Override
    public GetTopicsResult getConfig(GetConfigRequest getConfigRequest) {
        return null;
    }

    @Override
    public GetTopicsResult getAllTopics(GetTopicsRequest getTopicsRequest) {
        return null;
    }
}
