package org.apache.eventmesh.dashboard.console.function.health.check.impl.storage.rocketmq4;

import org.apache.eventmesh.dashboard.common.constant.health.HealthConstant;
import org.apache.eventmesh.dashboard.console.function.health.callback.HealthCheckCallback;
import org.apache.eventmesh.dashboard.console.function.health.check.AbstractTopicHealthCheckService;
import org.apache.eventmesh.dashboard.console.function.health.check.ClusterHealthCheckService;
import org.apache.eventmesh.dashboard.console.function.health.check.config.HealthCheckObjectConfig;

import org.apache.rocketmq.client.consumer.PullCallback;
import org.apache.rocketmq.client.consumer.PullResult;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.impl.CommunicationMode;
import org.apache.rocketmq.client.impl.MQClientAPIImpl;
import org.apache.rocketmq.common.TopicFilterType;
import org.apache.rocketmq.common.constant.PermName;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.InvokeCallback;
import org.apache.rocketmq.remoting.RemotingClient;
import org.apache.rocketmq.remoting.exception.RemotingConnectException;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.apache.rocketmq.remoting.exception.RemotingSendRequestException;
import org.apache.rocketmq.remoting.exception.RemotingTimeoutException;
import org.apache.rocketmq.remoting.netty.ResponseFuture;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;
import org.apache.rocketmq.remoting.protocol.RemotingSerializable;
import org.apache.rocketmq.remoting.protocol.RequestCode;
import org.apache.rocketmq.remoting.protocol.ResponseCode;
import org.apache.rocketmq.remoting.protocol.header.CreateTopicRequestHeader;
import org.apache.rocketmq.remoting.protocol.header.GetTopicConfigRequestHeader;
import org.apache.rocketmq.remoting.protocol.header.PullMessageRequestHeader;
import org.apache.rocketmq.remoting.protocol.header.SendMessageRequestHeader;
import org.apache.rocketmq.remoting.protocol.statictopic.TopicConfigAndQueueMapping;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class RocketMQClusterHealthCheckService implements ClusterHealthCheckService {


    private volatile boolean checkTopic = false;


    private Map<Long, MainHealthCheckService> healthCheckCallbacks = new ConcurrentHashMap<>();

    public AbstractTopicHealthCheckService register(HealthCheckObjectConfig healthCheckObjectConfig) {
        MainHealthCheckService healthCheckService = new MainHealthCheckService(healthCheckObjectConfig);
        healthCheckCallbacks.put(healthCheckObjectConfig.getInstanceId(), healthCheckService);
        return healthCheckService;
    }

    public void unRegister(HealthCheckObjectConfig healthCheckObjectConfig) {
        healthCheckCallbacks.remove(healthCheckObjectConfig.getInstanceId());
    }


    class MainHealthCheckService extends AbstractTopicHealthCheckService {

        private MQClientAPIImpl clientAPIImpl;

        private RemotingClient remotingClient;


        private String brokerAdd = "";

        public MainHealthCheckService(HealthCheckObjectConfig healthCheckObjectConfig) {
            super(healthCheckObjectConfig);
        }


        public void doCheck(HealthCheckCallback callback) throws Exception {
            if (!RocketMQClusterHealthCheckService.this.checkTopic) {
                this.getTopicConfig();
                return;
            }
            try {
                if (this.getConfig().getNodeByCopyInType().isSlave()) {
                    this.sendMessage(callback);
                } else {
                    this.consume(callback);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private void sendMessage(HealthCheckCallback callback) throws RemotingException, InterruptedException, MQClientException {
            if (this.getConfig().getNodeByCopyInType().isSlave()) {
                return;
            }
            SendMessageRequestHeader requestHeader = new SendMessageRequestHeader();
            requestHeader.setProducerGroup(HealthConstant.ROCKETMQ_CHECK_PRODUCER_GROUP);
            requestHeader.setTopic(HealthConstant.ROCKETMQ_CHECK_TOPIC);
            requestHeader.setDefaultTopic(HealthConstant.ROCKETMQ_CHECK_TOPIC));
            requestHeader.setDefaultTopicQueueNums(1);
            requestHeader.setQueueId(1);
            //requestHeader.setSysFlag(1);
            requestHeader.setBornTimestamp(System.currentTimeMillis());
            //requestHeader.setFlag(msg.getFlag());
            requestHeader.setReconsumeTimes(0);
            requestHeader.setUnitMode(false);
            requestHeader.setBatch(false);
            requestHeader.setBrokerName("127.0.0.1:9876");
            Message message = new Message();
            message.setTags(HealthConstant.ROCKETMQ_CHECK_TOPIC);
            message.setBody(this.messageContext());
            RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.SEND_MESSAGE, requestHeader);
            request.setBody(message.getBody());
            this.clientAPIImpl.getRemotingClient().invokeAsync(this.brokerAdd,
                request,
                1000,
                new InvokeCallback() {

                    @Override
                    public void operationComplete(ResponseFuture responseFuture) {

                    }

                    public void operationSucceed(RemotingCommand response) {
                        if (Objects.equals(response.getCode(), ResponseCode.SUCCESS)) {
                            try {
                                consume(callback);
                            } catch (Exception e) {
                                callback.onFail(new RuntimeException(e));
                            }

                        } else {
                            callback.onFail(new RuntimeException(""));
                        }
                    }

                    public void operationFail(Throwable throwable) {
                        callback.onFail(new RuntimeException(throwable));
                    }
                });

        }

        private void consume(HealthCheckCallback callback) throws MQBrokerException, RemotingException, InterruptedException {
            PullMessageRequestHeader requestHeader = new PullMessageRequestHeader();
            requestHeader.setConsumerGroup(HealthConstant.GROUP);
            requestHeader.setTopic(HealthConstant.ROCKETMQ_CHECK_TOPIC);
            this.clientAPIImpl.pullMessage(this.brokerAdd, requestHeader, 1000, CommunicationMode.ASYNC, new PullCallback() {
                @Override
                public void onSuccess(PullResult pullResult) {
                    AtomicBoolean isSuccess = new AtomicBoolean(false);
                    pullResult.getMsgFoundList().forEach(m -> {
                        if (MainHealthCheckService.this.isCurrentValue(m.getBody())) {
                            callback.onSuccess();
                            isSuccess.set(true);
                        }
                    });
                    if (isSuccess.get()) {
                        return;
                    }
                    try {
                        consume(callback);
                    } catch (Exception ex) {
                        callback.onFail(new RuntimeException(ex));
                    }
                }

                @Override
                public void onException(Throwable e) {
                    if (!isEndCheck()) {
                        try {
                            consume(callback);
                        } catch (Exception ex) {
                            callback.onFail(new RuntimeException(ex));
                        }
                    } else {
                        callback.onFail(new RuntimeException(e));
                    }
                }
            });
        }

        @Override
        public void init() throws Exception {
            this.getTopicConfig();
        }

        private void getTopicConfig() throws InterruptedException,
            RemotingTimeoutException, RemotingSendRequestException, RemotingConnectException, MQBrokerException {
            GetTopicConfigRequestHeader header = new GetTopicConfigRequestHeader();
            header.setTopic(HealthConstant.ROCKETMQ_CHECK_TOPIC);
            header.setLo(true);
            RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.GET_TOPIC_CONFIG, header);
            RemotingCommand response = this.remotingClient.invokeSync(this.brokerAdd, request, 3000);
            assert response != null;
            if (Objects.equals(response.getCode(), ResponseCode.SUCCESS)) {
                TopicConfigAndQueueMapping topicConfigAndQueueMapping =
                    RemotingSerializable.decode(response.getBody(), TopicConfigAndQueueMapping.class);
                ConcurrentMap<Integer, Integer> queue = topicConfigAndQueueMapping.getMappingDetail().getCurrIdMap();
                if (queue.size() == 1) {
                    queue.forEach((k, value) -> {
                        this.setOffset(value);
                        this.setQueue(k);
                    });
                    return;
                }
                RocketMQClusterHealthCheckService.this.checkTopic = true;
                // 删除 topic
            }
            if (this.getConfig().getNodeByCopyInType().isSlave()) {
                return;
            }
            this.createTopic();
        }

        private void createTopic() throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, InterruptedException {
            CreateTopicRequestHeader requestHeader = new CreateTopicRequestHeader();
            requestHeader.setTopic(HealthConstant.ROCKETMQ_CHECK_TOPIC);
            requestHeader.setTopicFilterType(TopicFilterType.SINGLE_TAG.name());
            requestHeader.setReadQueueNums(1);
            requestHeader.setWriteQueueNums(1);
            requestHeader.setPerm(PermName.PERM_READ | PermName.PERM_WRITE);
            RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.UPDATE_AND_CREATE_TOPIC, requestHeader);
            RemotingCommand response =
                remotingClient.invokeSync(getConfig().getRocketmqConfig().getBrokerUrl(), request, getConfig().getRequestTimeoutMillis());
            if (!Objects.equals(response.getCode(), ResponseCode.SUCCESS)) {
                RocketMQClusterHealthCheckService.this.checkTopic = true;
            }
        }


        @Override
        public void destroy() {
            producer.shutdown();
        }
    }

    ;

}
