package org.apache.eventmesh.dashboard.core.remoting.rocketmq;

import org.apache.eventmesh.dashboard.common.model.remoting.subscription.GetSubscriptionRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.subscription.GetSubscriptionResult;
import org.apache.eventmesh.dashboard.service.remoting.SubscriptionRemotingService;

public class RocketMQSubscriptionRemotingService extends AbstractRocketMQRemotingService implements SubscriptionRemotingService {
    @Override
    public GetSubscriptionResult getSubscription(GetSubscriptionRequest request) {
        return null;
    }
}
