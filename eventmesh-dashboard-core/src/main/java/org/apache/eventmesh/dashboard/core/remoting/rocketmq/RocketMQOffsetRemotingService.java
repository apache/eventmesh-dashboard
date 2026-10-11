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

import org.apache.eventmesh.dashboard.common.enums.message.ResetOffsetMode;
import org.apache.eventmesh.dashboard.common.model.remoting.offset.GetOffsetRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.offset.GetOffsetResponse;
import org.apache.eventmesh.dashboard.common.model.remoting.offset.GetOffsetResult;
import org.apache.eventmesh.dashboard.common.model.remoting.offset.ResetOffsetRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.offset.ResetOffsetResponse;
import org.apache.eventmesh.dashboard.common.model.remoting.offset.ResetOffsetResult;
import org.apache.eventmesh.dashboard.service.remoting.OffsetRemotingService;

import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;
import org.apache.rocketmq.remoting.protocol.RemotingSerializable;
import org.apache.rocketmq.remoting.protocol.RequestCode;
import org.apache.rocketmq.remoting.protocol.ResponseCode;
import org.apache.rocketmq.remoting.protocol.admin.ConsumeStats;
import org.apache.rocketmq.remoting.protocol.body.ResetOffsetBody;
import org.apache.rocketmq.remoting.protocol.header.GetConsumeStatsRequestHeader;
import org.apache.rocketmq.remoting.protocol.header.ResetOffsetRequestHeader;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

public class RocketMQOffsetRemotingService extends AbstractRocketMQRemotingService implements OffsetRemotingService {

    @Override
    public GetOffsetResult getOffset(GetOffsetRequest request) throws Exception {
        if (request == null || StringUtils.isBlank(request.getGroupName())
            || request.getTopic() != null && StringUtils.isBlank(request.getTopic())) {
            throw new IllegalArgumentException("Consumer group is required and topic filter must not be blank");
        }
        GetConsumeStatsRequestHeader header = new GetConsumeStatsRequestHeader();
        header.setConsumerGroup(request.getGroupName());
        header.setTopic(request.getTopic());
        RemotingCommand response = this.invokeSync(RemotingCommand.createRequestCommand(RequestCode.GET_CONSUME_STATS, header));
        GetOffsetResult result = this.buildResult(response, new GetOffsetResult());
        if (response.getCode() != ResponseCode.SUCCESS) {
            return result;
        }
        ConsumeStats stats = this.decodeBody(response, ConsumeStats.class);
        if (stats.getOffsetTable() == null) {
            throw new IllegalStateException("RocketMQ returned no consumption offset table");
        }
        List<GetOffsetResponse> offsets = new ArrayList<>();
        stats.getOffsetTable().forEach((queue, value) -> {
            if (queue == null || value == null) {
                throw new IllegalStateException("RocketMQ returned an invalid consumption offset entry");
            }
            GetOffsetResponse offset = new GetOffsetResponse();
            offset.setTopic(queue.getTopic());
            offset.setBrokerName(queue.getBrokerName());
            offset.setPartitionId(queue.getQueueId());
            offset.setOffset(value.getConsumerOffset());
            offset.setBrokerOffset(value.getBrokerOffset());
            offset.setLastTimestamp(value.getLastTimestamp());
            offsets.add(offset);
        });
        offsets.sort(Comparator.comparing(GetOffsetResponse::getTopic).thenComparing(GetOffsetResponse::getBrokerName)
            .thenComparing(GetOffsetResponse::getPartitionId));
        result.setData(offsets);
        return result;
    }

    /** Reset is an explicit administrative operation, never a metadata synchronization action. */
    @Override
    public ResetOffsetResult resetOffset(ResetOffsetRequest request) throws Exception {
        if (request == null || StringUtils.isBlank(request.getGroupName()) || StringUtils.isBlank(request.getTopic())
            || request.getResetOffsetMode() == null) {
            throw new IllegalArgumentException("Consumer group, topic and reset mode are required");
        }
        ResetOffsetMode mode = request.getResetOffsetMode();
        if (mode == ResetOffsetMode.CONSUME_FROM_DESIGNATED_OFFSET) {
            if (request.getPartitionId() == null || request.getPartitionId() < 0 || request.getOffset() == null || request.getOffset() < 0) {
                throw new IllegalArgumentException("Designated reset requires a nonnegative queue ID and offset");
            }
        } else if (request.getPartitionId() != null || request.getOffset() != null) {
            throw new IllegalArgumentException("Queue ID and offset are only supported for designated reset");
        }
        if (mode == ResetOffsetMode.CONSUME_FROM_TIMESTAMP) {
            if (request.getTimestamp() == null || request.getTimestamp() < 0) {
                throw new IllegalArgumentException("Timestamp reset requires nonnegative epoch milliseconds");
            }
        } else if (request.getTimestamp() != null) {
            throw new IllegalArgumentException("Timestamp is only supported for timestamp reset");
        }
        // The legacy client-side path ignores queue ID and explicit offset. Never fall back to it.
        RemotingCommand config = this.invokeSync(RemotingCommand.createRequestCommand(RequestCode.GET_BROKER_CONFIG, null));
        if (config.getCode() != ResponseCode.SUCCESS) {
            return this.buildResult(config, new ResetOffsetResult());
        }
        if (config.getBody() == null || config.getBody().length == 0) {
            throw new IllegalStateException("RocketMQ returned no Broker configuration");
        }
        Properties properties = new Properties();
        properties.load(new StringReader(new String(config.getBody(), StandardCharsets.UTF_8)));
        if (!Boolean.parseBoolean(properties.getProperty("useServerSideResetOffset"))) {
            throw new UnsupportedOperationException("Offset reset requires Broker useServerSideResetOffset=true");
        }
        ResetOffsetRequestHeader header = new ResetOffsetRequestHeader();
        header.setGroup(request.getGroupName());
        header.setTopic(request.getTopic());
        header.setForce(true);
        header.setQueueId(-1);
        header.setOffset(-1L);
        switch (mode) {
            case CONSUME_FROM_FIRST_OFFSET:
                header.setTimestamp(0L);
                break;
            case CONSUME_FROM_LAST_OFFSET:
                header.setTimestamp(-1L);
                break;
            case CONSUME_FROM_TIMESTAMP:
                header.setTimestamp(request.getTimestamp());
                break;
            case CONSUME_FROM_DESIGNATED_OFFSET:
                header.setQueueId(request.getPartitionId());
                header.setOffset(request.getOffset());
                header.setTimestamp(-1L);
                break;
            default:
                throw new IllegalArgumentException("Unsupported reset mode: " + mode);
        }
        RemotingCommand response = this.invokeSync(RemotingCommand.createRequestCommand(RequestCode.INVOKE_BROKER_TO_RESET_OFFSET, header));
        ResetOffsetResult result = this.buildResult(response, new ResetOffsetResult());
        if (response.getCode() != ResponseCode.SUCCESS) {
            return result;
        }
        ResetOffsetBody body = this.decodeBody(response, ResetOffsetBody.class);
        if (body.getOffsetTable() == null || body.getOffsetTable().isEmpty()) {
            throw new IllegalStateException("RocketMQ returned no reset offsets; verify Broker state before retrying");
        }
        List<ResetOffsetResponse> offsets = new ArrayList<>();
        body.getOffsetTable().forEach((queue, value) -> {
            if (queue == null || value == null) {
                throw new IllegalStateException("RocketMQ returned an invalid reset offset entry");
            }
            ResetOffsetResponse offset = new ResetOffsetResponse();
            offset.setTopic(queue.getTopic());
            offset.setBrokerName(queue.getBrokerName());
            offset.setPartitionId(queue.getQueueId());
            offset.setOffset(value);
            offsets.add(offset);
        });
        offsets.sort(Comparator.comparing(ResetOffsetResponse::getTopic).thenComparing(ResetOffsetResponse::getBrokerName)
            .thenComparing(ResetOffsetResponse::getPartitionId));
        result.setData(offsets);
        return result;
    }

    private <T> T decodeBody(RemotingCommand response, Class<T> type) {
        if (response.getBody() == null || response.getBody().length == 0) {
            throw new IllegalStateException("RocketMQ returned no offset response body");
        }
        T body = RemotingSerializable.decode(response.getBody(), type);
        if (body == null) {
            throw new IllegalStateException("RocketMQ returned an invalid offset response body");
        }
        return body;
    }
}
