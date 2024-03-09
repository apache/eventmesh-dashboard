package org.apache.eventmesh.dashboard.common.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.eventmesh.dashboard.common.model.TopicProperties;
import org.apache.rocketmq.acl.common.AclClientRPCHook;
import org.apache.rocketmq.acl.common.SessionCredentials;
import org.apache.rocketmq.common.TopicConfig;
import org.apache.rocketmq.common.TopicFilterType;
import org.apache.rocketmq.common.admin.TopicOffset;
import org.apache.rocketmq.common.admin.TopicStatsTable;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.remoting.RPCHook;
import org.apache.rocketmq.tools.admin.DefaultMQAdminExt;
import org.apache.rocketmq.tools.command.CommandUtil;

import java.util.*;

@Slf4j
@UtilityClass
public class RocketmqUtils {

    private DefaultMQAdminExt createMqAdminExt(String nameServerAddr) {
        final RPCHook rpcHook = new AclClientRPCHook(new SessionCredentials());
        DefaultMQAdminExt adminExt = new DefaultMQAdminExt(rpcHook);
        String groupId = UUID.randomUUID().toString();
        adminExt.setAdminExtGroup("admin_ext_group-" + groupId);
        adminExt.setNamesrvAddr(nameServerAddr);
        return adminExt;
    }


    public void createTopic(String topicName, TopicFilterType topicFilterType, int perm, String nameServerAddr,
                            String clusterName, int readQueueNums, int writeQueueNums) {
        if (StringUtils.isBlank(topicName)) {
            log.info("Topic name can not be blank");
        }
        DefaultMQAdminExt adminExt = createMqAdminExt(nameServerAddr);

        try {
            adminExt.start();
            Set<String> brokerAddress = CommandUtil.fetchMasterAddrByClusterName(adminExt, clusterName);
            for (String masterAddress : brokerAddress) {
                TopicConfig topicConfig = new TopicConfig();
                topicConfig.setTopicName(topicName);
                topicConfig.setReadQueueNums(readQueueNums);
                topicConfig.setWriteQueueNums(writeQueueNums);
                topicConfig.setPerm(perm);
                topicConfig.setTopicFilterType(topicFilterType);
                adminExt.createAndUpdateTopicConfig(masterAddress, topicConfig);
            }
        } catch (Exception e) {
            log.info("Failed to create topic: " + e.getMessage());
        } finally {
            adminExt.shutdown();
        }
    }

    public List<TopicProperties> getTopics(String nameServerAddr) {
        DefaultMQAdminExt adminExt = createMqAdminExt(nameServerAddr);
        List<TopicProperties> result = new ArrayList<>();
        try {
            adminExt.start();
            Set<String> topicList = adminExt.fetchAllTopicList().getTopicList();
            for (String topic : topicList) {
                long messageCount = 0;
                TopicStatsTable topicStats = adminExt.examineTopicStats(topic);
                HashMap<MessageQueue, TopicOffset> offsetTable = topicStats.getOffsetTable();
                for (TopicOffset topicOffset : offsetTable.values()) {
                    messageCount += topicOffset.getMaxOffset() - topicOffset.getMinOffset();
                }
                result.add(new TopicProperties(topic, messageCount));
            }

            result.sort(Comparator.comparing(TopicProperties::getName));
            return result;
        } catch (Exception e) {
            log.info("Failed to getTopics: " + e.getMessage());
        } finally {
            adminExt.shutdown();
        }
        return result;
    }


    public void deleteTopic(String topicName, String nameServerAddr, String clusterName) {
        if (StringUtils.isBlank(topicName)) {
            log.info("Topic name can not be blank.");
            return;
        }
        DefaultMQAdminExt adminExt = createMqAdminExt(nameServerAddr);
        try {
            adminExt.start();
            Set<String> brokerAddress = CommandUtil.fetchMasterAddrByClusterName(adminExt, clusterName);
            adminExt.deleteTopicInBroker(brokerAddress, topicName);
        } catch (Exception e) {
            log.info("Failed to delete topic: " + e.getMessage());
        } finally {
            adminExt.shutdown();
        }
    }

}
