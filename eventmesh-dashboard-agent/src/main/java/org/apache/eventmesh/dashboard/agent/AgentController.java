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

package org.apache.eventmesh.dashboard.agent;

import org.apache.eventmesh.dashboard.agent.action.AbstractAgentActionClient;
import org.apache.eventmesh.dashboard.agent.config.ConfigHandler;
import org.apache.eventmesh.dashboard.agent.config.ConfigHandlerManage;
import org.apache.eventmesh.dashboard.agent.model.AgentCheckRuntimeVO;
import org.apache.eventmesh.dashboard.agent.model.AgentStartActionVO;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import com.alibaba.fastjson2.JSON;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AgentController {


    public static String getLocalAddress() {
        try {
            // Traversal Network interface to get the first non-loopback and non-private address
            Enumeration<NetworkInterface> enumeration = NetworkInterface.getNetworkInterfaces();
            ArrayList<String> ipv4Result = new ArrayList<String>();
            ArrayList<String> ipv6Result = new ArrayList<String>();
            while (enumeration.hasMoreElements()) {
                final NetworkInterface networkInterface = enumeration.nextElement();
                final Enumeration<InetAddress> en = networkInterface.getInetAddresses();
                while (en.hasMoreElements()) {
                    final InetAddress address = en.nextElement();
                    if (!address.isLoopbackAddress()) {
                        if (address instanceof Inet6Address) {
                            ipv6Result.add(normalizeHostAddress(address));
                        } else {
                            ipv4Result.add(normalizeHostAddress(address));
                        }
                    }
                }
            }

            // prefer ipv4
            if (!ipv4Result.isEmpty()) {
                for (String ip : ipv4Result) {
                    if (ip.startsWith("127.0") || ip.startsWith("192.168")) {
                        continue;
                    }

                    return ip;
                }

                return ipv4Result.get(ipv4Result.size() - 1);
            } else if (!ipv6Result.isEmpty()) {
                return ipv6Result.get(0);
            }
            //If failed to find,fall back to localhost
            final InetAddress localHost = InetAddress.getLocalHost();
            return normalizeHostAddress(localHost);
        } catch (Exception e) {
            //log.error("Failed to obtain local address", e);
        }

        return null;
    }

    public static String normalizeHostAddress(final InetAddress localHost) {
        if (localHost instanceof Inet6Address) {
            return "[" + localHost.getHostAddress() + "]";
        } else {
            return localHost.getHostAddress();
        }
    }


    private final ConfigHandlerManage configHandlerManage = new ConfigHandlerManage();

    private final List<Long> waitTime = new ArrayList<>();

    private final AbstractAgentActionClient agentActionClient = new DefaultAgentActionClient();

    private final Map<String, String> env = System.getenv();

    private final String localAddress = getLocalAddress();

    {
        for (int i = 1; i < 4; i++) {
            waitTime.add(50L);
            waitTime.add(200L);
            waitTime.add(500L);
            waitTime.add(1000L);
            waitTime.add(3000L);
        }
        Collections.sort(waitTime);

    }

    public static void main(String[] args) {
        AgentController agentController = new AgentController();
        agentController.start();
    }

    public void start() {

        String consoleAddress = this.getEnv(Constant.ENV_CONSOLE);
        if (Objects.isNull(consoleAddress)) {
            log.error("{} is null", Constant.ENV_CONSOLE);
            return;
        }
        String clusterId = this.getEnv(Constant.ENV_CLUSTER_ID);
        if (Objects.isNull(clusterId)) {
            log.error("{} is null", Constant.ENV_CLUSTER_ID);
            return;
        }

        String runtimeId = this.getEnv(Constant.ENV_RUNTIME_ID);
        if (Objects.isNull(runtimeId)) {
            log.error("{} is null", Constant.ENV_RUNTIME_ID);
            return;
        }
        String runtimeHome = this.getEnv(Constant.ENV_RUNTIME_HOME);
        if (Objects.isNull(runtimeHome)) {
            log.error("{} is null", Constant.ENV_RUNTIME_HOME);
            return;
        }
        File file = new File(runtimeHome);
        if (file.exists() && !file.isDirectory()) {
            log.error("eventmesh.agent.runtimeHome is not a directory");
            return;
        }
        this.agentActionClient.setBaseUrl(consoleAddress);
        this.agentActionClient.init();

        Map<String, String> data = new HashMap<>();
        data.put("clusterId", clusterId);
        data.put("runtimeId", runtimeId);
        data.put("localAddress", this.localAddress);
        data.put("nodeAddress", this.getEnv("NODE_ID"));
        AgentStartActionVO agentStartActionVO = agentActionClient.agentStartAction(data);

        // 得到 clusterType 对应的执行对象
        ConfigHandler configHandler = null;
        try {
            configHandler = this.configHandlerManage.getConfigHandler(agentStartActionVO.getClusterType());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return;
        }
        if (Objects.isNull(configHandler)) {
            log.error("configHandler is null");
            return;
        }

        configHandler.handler();

        if (!agentStartActionVO.isCheck()) {
            log.info("agentStartActionVO.isCheck is false");
            log.info("agent ok");
            return;
        }

        AtomicBoolean success = new AtomicBoolean(false);
        // 请求 是否可以启动
        waitTime.forEach(waitTime -> {

            try {
                AgentCheckRuntimeVO vo = agentActionClient.agentCheckRuntime(data);
                if (vo.isSuccess()) {
                    success.set(true);
                    log.info("agentCheckRuntime success time {}", waitTime);
                    return;
                }
                Thread.sleep(waitTime);
            } catch (Exception e) {
                log.error("agentStartAction error", e);
            }
        });
        if (!success.get()) {
            log.info("check is fail");
        }

    }

    public String getEnv(String key) {
        return env.get(key);
    }

    private static class DefaultAgentActionClient extends AbstractAgentActionClient {

        private CloseableHttpClient httpClient = HttpClients.createDefault();


        @Override
        public AgentStartActionVO agentStartAction(Map<String, String> data) {
            return this.execute(this.agentStartActionFullPath, data, AgentStartActionVO.class);
        }

        @Override
        public AgentCheckRuntimeVO agentCheckRuntime(Map<String, String> data) {
            return this.execute(this.agentCheckRuntimePath, data, AgentCheckRuntimeVO.class);
        }


        private <T> T execute(String url, Object data, Class<T> clazz) {
            HttpPost request = new HttpPost(url);
            HttpEntity httpEntity = new StringEntity(JSON.toJSONString(data), ContentType.APPLICATION_JSON);
            request.setEntity(httpEntity);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                JSON.parseObject(EntityUtils.toString(response.getEntity()), clazz);
                return JSON.parseObject(EntityUtils.toString(response.getEntity()), clazz);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
