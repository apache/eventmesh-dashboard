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

package org.apache.eventmesh.dashboard.core.meta.runtime;

import org.apache.eventmesh.dashboard.common.model.remoting.runtime.GetRuntimeRequest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Timeout(value = 5)
class NacosRuntimeCoreTest {

    private NacosRuntimeCore nacosRuntimeCore = new NacosRuntimeCore();

    @Test
    public void testGetRuntime() throws InterruptedException {
        try {
            GetRuntimeRequest getRuntimeRequest = new GetRuntimeRequest();
            getRuntimeRequest.setRegistryAddress("127.0.0.1:8848");
            nacosRuntimeCore.getRuntime(getRuntimeRequest).getFuture().thenAccept(
                getRuntimeResponse -> {
                    log.info("testGetRuntime success, the first cluster name is {}",
                        getRuntimeResponse.getRuntimeMetadataList().get(0).getClusterName());
                }
            );
        } catch (Exception e) {
            log.error("testGetRuntime failed", e);
        }
        Thread.sleep(1000);
    }
}