/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.apache.eventmesh.dashboard.core.remoting.rocketmq;

import org.apache.eventmesh.dashboard.common.model.metadata.ClientMetadata;
import org.apache.eventmesh.dashboard.common.model.remoting.client.GetClientsResult;
import org.apache.eventmesh.dashboard.service.remoting.ClientRemotingService;

import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.remoting.protocol.LanguageCode;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;
import org.apache.rocketmq.remoting.protocol.RemotingSerializable;
import org.apache.rocketmq.remoting.protocol.RequestCode;
import org.apache.rocketmq.remoting.protocol.ResponseCode;
import org.apache.rocketmq.remoting.protocol.body.Connection;
import org.apache.rocketmq.remoting.protocol.body.ConsumerConnection;
import org.apache.rocketmq.remoting.protocol.body.ProducerInfo;
import org.apache.rocketmq.remoting.protocol.body.ProducerTableInfo;
import org.apache.rocketmq.remoting.protocol.body.SubscriptionGroupWrapper;
import org.apache.rocketmq.remoting.protocol.header.GetConsumerConnectionListRequestHeader;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

/** Broker-local producer connections and consumers of configured groups; no inferred process IDs. */
public class RocketMQClientRemotingService extends AbstractRocketMQRemotingService implements ClientRemotingService {

    @Override
    public GetClientsResult getClientList() throws Exception {
        Map<String, ClientMetadata> clients = new TreeMap<>();
        RemotingCommand response = this.invokeSync(RemotingCommand.createRequestCommand(RequestCode.GET_ALL_PRODUCER_INFO, null));
        if (response.getCode() != ResponseCode.SUCCESS) {
            return this.buildResult(response, new GetClientsResult());
        }
        ProducerTableInfo producers = this.decode(response, ProducerTableInfo.class, "data");
        if (producers.getData() == null) {
            throw new IllegalStateException("RocketMQ returned no producer table");
        }
        for (List<ProducerInfo> group : producers.getData().values()) {
            if (group == null) {
                throw new IllegalStateException("RocketMQ returned an invalid producer group");
            }
            for (ProducerInfo producer : group) {
                if (producer == null) {
                    throw new IllegalStateException("RocketMQ returned an invalid producer connection");
                }
                this.addClient(clients, producer.getClientId(), producer.getRemoteIP(), producer.getLanguage());
            }
        }
        response = this.invokeSync(RemotingCommand.createRequestCommand(RequestCode.GET_ALL_SUBSCRIPTIONGROUP_CONFIG, null));
        if (response.getCode() != ResponseCode.SUCCESS) {
            return this.buildResult(response, new GetClientsResult());
        }
        SubscriptionGroupWrapper groups = this.decode(response, SubscriptionGroupWrapper.class, "subscriptionGroupTable");
        if (groups.getSubscriptionGroupTable() == null) {
            throw new IllegalStateException("RocketMQ returned no subscription group table");
        }
        for (String group : new java.util.TreeSet<>(groups.getSubscriptionGroupTable().keySet())) {
            GetConsumerConnectionListRequestHeader header = new GetConsumerConnectionListRequestHeader();
            header.setConsumerGroup(group);
            response = this.invokeSync(RemotingCommand.createRequestCommand(RequestCode.GET_CONSUMER_CONNECTION_LIST, header));
            if (response.getCode() == ResponseCode.CONSUMER_NOT_ONLINE) {
                continue;
            }
            if (response.getCode() != ResponseCode.SUCCESS) {
                // Do not return a partially collected list as a successful snapshot.
                return this.buildResult(response, new GetClientsResult());
            }
            ConsumerConnection consumers = this.decode(response, ConsumerConnection.class, "connectionSet");
            if (consumers.getConnectionSet() == null) {
                throw new IllegalStateException("RocketMQ returned no consumer connection set");
            }
            for (Connection connection : consumers.getConnectionSet()) {
                if (connection == null) {
                    throw new IllegalStateException("RocketMQ returned an invalid consumer connection");
                }
                this.addClient(clients, connection.getClientId(), connection.getClientAddr(), connection.getLanguage());
            }
        }
        GetClientsResult result = new GetClientsResult();
        result.setCode(200);
        result.setData(new ArrayList<>(clients.values()));
        return result;
    }

    private void addClient(Map<String, ClientMetadata> clients, String name, String address, LanguageCode language) {
        if (StringUtils.isBlank(name) || StringUtils.isBlank(address) || language == null) {
            throw new IllegalStateException("RocketMQ returned incomplete client identity");
        }
        String endpoint = address.startsWith("/") ? address.substring(1) : address;
        int separator = endpoint.lastIndexOf(':');
        if (separator <= 0 || separator == endpoint.length() - 1) {
            throw new IllegalStateException("RocketMQ returned an invalid client address");
        }
        String host = endpoint.substring(0, separator);
        if (host.startsWith("[") && host.endsWith("]")) {
            host = host.substring(1, host.length() - 1);
        }
        int port;
        try {
            port = Integer.parseInt(endpoint.substring(separator + 1));
        } catch (NumberFormatException e) {
            throw new IllegalStateException("RocketMQ returned an invalid client port", e);
        }
        if (StringUtils.isBlank(host) || port < 1 || port > 65535) {
            throw new IllegalStateException("RocketMQ returned an invalid client endpoint");
        }
        ClientMetadata client = new ClientMetadata();
        client.setName(name);
        client.setHost(host);
        client.setPort(port);
        client.setLanguage(language.name());
        client.setProtocol("RocketMQ");
        ClientMetadata previous = clients.putIfAbsent(client.nodeUnique(), client);
        if (previous != null && (!name.equals(previous.getName()) || !client.getLanguage().equals(previous.getLanguage()))) {
            throw new IllegalStateException("RocketMQ returned conflicting client identities for one connection");
        }
    }

    private <T> T decode(RemotingCommand response, Class<T> type, String field) {
        if (response.getBody() == null || response.getBody().length == 0) {
            throw new IllegalStateException("RocketMQ returned no client query body");
        }
        JSONObject json = JSON.parseObject(response.getBody(), JSONObject.class);
        if (json == null || !json.containsKey(field)) {
            throw new IllegalStateException("RocketMQ returned an invalid client query body");
        }
        T body = RemotingSerializable.decode(response.getBody(), type);
        if (body == null) {
            throw new IllegalStateException("RocketMQ returned an invalid client query body");
        }
        return body;
    }
}
