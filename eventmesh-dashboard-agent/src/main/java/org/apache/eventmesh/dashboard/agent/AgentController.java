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

import org.apache.eventmesh.dashboard.agent.config.ConfigHandler;
import org.apache.eventmesh.dashboard.agent.config.ConfigHandlerManage;
import org.apache.eventmesh.dashboard.agent.model.AgentCheckRuntimeVO;
import org.apache.eventmesh.dashboard.agent.model.AgentStartActionVO;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AgentController {


    private final ConfigHandlerManage configHandlerManage = new ConfigHandlerManage();
    private final List<Long> waitTime = new ArrayList<>();
    private AgentActionClient agentActionClient;

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
        String consoleAddress = System.getenv("eventmesh.agent.console");
        if (Objects.isNull(consoleAddress)) {
            log.error("eventmesh.agent.console is null");
            return;
        }

        String clusterId = System.getenv("eventmesh.agent.clusterId");
        if (Objects.isNull(clusterId)) {
            log.error("eventmesh.agent.clusterId is null");
            return;
        }

        String runtimeId = System.getenv("eventmesh.agent.runtimeId");
        if (Objects.isNull(runtimeId)) {
            log.error("eventmesh.agent.runtimeId is null");
            return;
        }
        String runtimeHome = System.getenv("eventmesh.agent.runtimeHome");
        if (Objects.isNull(runtimeHome)) {
            log.error("eventmesh.agent.runtimeHome is null");
            return;
        }
        File file = new File(runtimeHome);
        if (file.exists() && !file.isDirectory()) {
            log.error("eventmesh.agent.runtimeHome is not a directory");
            return;
        }

        Map<String, String> data = new HashMap<>();
        data.put("clusterId", clusterId);
        data.put("runtimeId", runtimeId);
        AgentStartActionVO agentStartActionVO = agentActionClient.agentStartAction(data);

        // 得到 clusterType 对应的执行对象
        ConfigHandler configHandler = null;
        try {
            configHandler = this.configHandlerManage.getConfigHandler(agentStartActionVO.getClusterType());
        } catch (Exception e) {
            throw new RuntimeException(e);
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
}
