package org.apache.eventmesh.dashboard.common.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.TopicConfig;
import org.apache.rocketmq.common.protocol.RequestCode;
import org.apache.rocketmq.common.protocol.body.TopicConfigSerializeWrapper;
import org.apache.rocketmq.common.protocol.header.CreateTopicRequestHeader;
import org.apache.rocketmq.common.protocol.header.DeleteTopicRequestHeader;
import org.apache.rocketmq.remoting.CommandCustomHeader;
import org.apache.rocketmq.remoting.RemotingClient;
import org.apache.rocketmq.remoting.netty.NettyClientConfig;
import org.apache.rocketmq.remoting.netty.NettyRemotingClient;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;

import java.util.*;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@UtilityClass
public class RocketmqUtils {
    private final RemotingClient remotingClient;

    static{
        NettyClientConfig config = new NettyClientConfig();
        config.setUseTLS(false);
        remotingClient = new NettyRemotingClient(config);
        remotingClient.start();
    }


    public void createTopic(String topicName, String topicFilterTypeName, int perm, String nameServerAddr,
                            int readQueueNums, int writeQueueNums, long requestTimeoutMillis) {
        try {
            CreateTopicRequestHeader requestHeader = new CreateTopicRequestHeader();
            requestHeader.setTopic(topicName);
            requestHeader.setTopicFilterType(topicFilterTypeName);
            requestHeader.setReadQueueNums(readQueueNums);
            requestHeader.setWriteQueueNums(writeQueueNums);
            requestHeader.setPerm(perm);
            RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.UPDATE_AND_CREATE_TOPIC, requestHeader);
            Object result = remotingClient.invokeSync(nameServerAddr, request, requestTimeoutMillis);
            log.info("rocketmq create topic result:" + result.toString());
        } catch (Exception e) {
            log.error("RocketmqTopicCheck init failed when examining topic stats.", e);
        }
    }

    public List<TopicConfig> getTopics(String nameServerAddr, long requestTimeoutMillis) {
        List<TopicConfig> topicConfigList = new ArrayList<>();
        try {
            RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.GET_ALL_TOPIC_CONFIG, (CommandCustomHeader) null);
            RemotingCommand response = remotingClient.invokeSync(nameServerAddr, request, 3000L);
            TopicConfigSerializeWrapper allTopicConfig = TopicConfigSerializeWrapper.decode(response.getBody(), TopicConfigSerializeWrapper.class);
            ConcurrentMap<String, TopicConfig> topicConfigTable = allTopicConfig.getTopicConfigTable();
            topicConfigList = new ArrayList<>(topicConfigTable.values());
        } catch (Exception e) {
            log.error("RocketmqTopicCheck init failed when examining topic stats.", e);
        }
        return topicConfigList;
    }


    public void deleteTopic(String topicName, String nameServerAddr, long requestTimeoutMillis) {
        try {
            DeleteTopicRequestHeader deleteTopicRequestHeader = new DeleteTopicRequestHeader();
            deleteTopicRequestHeader.setTopic(topicName);
            RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.DELETE_TOPIC_IN_NAMESRV, null);
            Object result = remotingClient.invokeSync(nameServerAddr, request, requestTimeoutMillis);

            log.info("rocketmq delete topic result:" + result.toString());
        } catch (Exception e) {
            log.error("RocketmqTopicCheck init failed when examining topic stats.", e);
        }
    }

}
