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


package org.apache.eventmesh.dashboard.core.function.SDK.operation.rocketmq;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.enums.RemotingType;
import org.apache.eventmesh.dashboard.core.function.SDK.AbstractSDKOperation;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKMetadata;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKTypeEnum;
import org.apache.eventmesh.dashboard.core.function.SDK.config.CreateRemotingConfig;
import org.apache.eventmesh.dashboard.core.function.SDK.operation.rocketmq.RocketMQRemotingSDKOperation.DefaultRemotingClient;

import org.apache.rocketmq.remoting.InvokeCallback;
import org.apache.rocketmq.remoting.RPCHook;
import org.apache.rocketmq.remoting.RemotingClient;
import org.apache.rocketmq.remoting.exception.RemotingConnectException;
import org.apache.rocketmq.remoting.exception.RemotingSendRequestException;
import org.apache.rocketmq.remoting.exception.RemotingTimeoutException;
import org.apache.rocketmq.remoting.exception.RemotingTooMuchRequestException;
import org.apache.rocketmq.remoting.netty.NettyClientConfig;
import org.apache.rocketmq.remoting.netty.NettyRemotingClient;
import org.apache.rocketmq.remoting.netty.NettyRequestProcessor;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

@SDKMetadata(clusterType = {ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE,
    ClusterType.STORAGE_ROCKETMQ_NAMESERVER, ClusterType.STORAGE_ROCKETMQ_BROKER_RAFT}, remotingType = RemotingType.ROCKETMQ,
    sdkTypeEnum = {SDKTypeEnum.ADMIN, SDKTypeEnum.PING})
public class RocketMQRemotingSDKOperation extends AbstractSDKOperation<DefaultRemotingClient, CreateRemotingConfig> {

    private RemotingClient remotingClient;

    {
        // TODO
        NettyClientConfig config = new NettyClientConfig();
        config.setUseTLS(false);
        remotingClient = new NettyRemotingClient(config);
        remotingClient.start();
    }

    /**
     * 是否需要封装下 RemotingClient 没有 addr
     *
     * @param clientConfig
     * @return
     */
    @Override
    public DefaultRemotingClient createClient(CreateRemotingConfig clientConfig) {
        DefaultRemotingClient defaultRemotingClient = new DefaultRemotingClient();
        defaultRemotingClient.remotingClient = this.remotingClient;
        defaultRemotingClient.addr = clientConfig.getNetAddress().doUniqueKey();
        return defaultRemotingClient;
    }

    @Override
    public void close(DefaultRemotingClient client) {
        client.shutdown();
    }

    public static class DefaultRemotingClient {

        private String addr;

        private RemotingClient remotingClient;


        public void updateNameServerAddressList(List<String> addrs) {
            this.remotingClient.updateNameServerAddressList(addrs);
        }


        public List<String> getNameServerAddressList() {
            return this.remotingClient.getNameServerAddressList();
        }


        public List<String> getAvailableNameSrvList() {
            return this.remotingClient.getAvailableNameSrvList();
        }


        public RemotingCommand invokeSync(RemotingCommand request, long timeoutMillis)
            throws InterruptedException, RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException {
            return this.remotingClient.invokeSync(this.addr, request, timeoutMillis);
        }


        public void invokeAsync(RemotingCommand request, long timeoutMillis, InvokeCallback invokeCallback)
            throws InterruptedException, RemotingConnectException, RemotingTooMuchRequestException, RemotingTimeoutException,
            RemotingSendRequestException {
            this.remotingClient.invokeAsync(this.addr, request, timeoutMillis, invokeCallback);
        }


        public void invokeOneway(RemotingCommand request, long timeoutMillis)
            throws InterruptedException, RemotingConnectException, RemotingTooMuchRequestException, RemotingTimeoutException,
            RemotingSendRequestException {
            this.remotingClient.invokeOneway(this.addr, request, timeoutMillis);
        }


        public void registerProcessor(int requestCode, NettyRequestProcessor processor, ExecutorService executor) {
            this.remotingClient.registerProcessor(requestCode, processor, executor);
        }


        public boolean isChannelWritable(String addr) {
            return this.remotingClient.isChannelWritable(this.addr);
        }


        public boolean isAddressReachable(String addr) {
            return this.remotingClient.isAddressReachable(this.addr);
        }


        public void closeChannels(List<String> addrList) {
            this.remotingClient.closeChannels(addrList);
        }


        public void start() {
            this.remotingClient.start();
        }


        public void shutdown() {
            List<String> addrList = new ArrayList<>();
            addrList.add(this.addr);
            this.closeChannels(addrList);
        }


        public void registerRPCHook(RPCHook rpcHook) {

        }


        public void clearRPCHook() {

        }
    }
}
