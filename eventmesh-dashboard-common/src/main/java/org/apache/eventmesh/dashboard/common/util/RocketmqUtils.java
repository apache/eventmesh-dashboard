package org.apache.eventmesh.dashboard.common.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.protocol.RequestCode;
import org.apache.rocketmq.common.protocol.header.CreateTopicRequestHeader;
import org.apache.rocketmq.common.protocol.header.DeleteTopicRequestHeader;
import org.apache.rocketmq.common.protocol.header.GetTopicStatsInfoRequestHeader;
import org.apache.rocketmq.remoting.RemotingClient;
import org.apache.rocketmq.remoting.netty.NettyClientConfig;
import org.apache.rocketmq.remoting.netty.NettyRemotingClient;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;

@Slf4j
@UtilityClass
public class RocketmqUtils {
    private static final RemotingClient remotingClient;

    private static int readQueueNums = 8;
    private static int writeQueueNums = 8;

    static {
        NettyClientConfig config = new NettyClientConfig();
        config.setUseTLS(false);
        remotingClient = new NettyRemotingClient(config);
        remotingClient.start();
    }

    public void createTopic(String topicName, String topicFilterType, int perm, String brokerUrl, long requestTimeoutMillis){
        try {
            CreateTopicRequestHeader requestHeader = new CreateTopicRequestHeader();
            requestHeader.setTopic(topicName);
            requestHeader.setTopicFilterType(topicFilterType);
            requestHeader.setReadQueueNums(readQueueNums);
            requestHeader.setWriteQueueNums(writeQueueNums);
            requestHeader.setPerm(perm);
            RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.UPDATE_AND_CREATE_TOPIC, requestHeader);
            Object result = remotingClient.invokeSync(brokerUrl, request, requestTimeoutMillis);
            log.info(result.toString());
        } catch (Exception e) {
            log.error("RocketmqTopic create failed.", e);
        }
    }

    public void getTopic(String topicName, String brokerUrl, long requestTimeoutMillis){
        try {
            GetTopicStatsInfoRequestHeader requestHeader = new GetTopicStatsInfoRequestHeader();
            requestHeader.setTopic(topicName);
            RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.GET_TOPIC_STATS_INFO, requestHeader);
            Object result = remotingClient.invokeSync(brokerUrl, request, requestTimeoutMillis);
            log.info(result.toString());
        } catch (Exception e) {
            log.error("RocketmqTopic get failed.", e);
        }
    }

    public void deleteTopic(String topicName, String brokerUrl, long requestTimeoutMillis){
        try {
            DeleteTopicRequestHeader requestHeader = new DeleteTopicRequestHeader();
            requestHeader.setTopic(topicName);
            RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.DELETE_TOPIC_IN_BROKER, requestHeader);
            Object result = remotingClient.invokeSync(brokerUrl, request, requestTimeoutMillis);
            log.info(result.toString());
        } catch (Exception e) {
            log.error("RocketmqTopic delete failed.", e);
        }
    }

}
