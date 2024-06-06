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

package org.apache.eventmesh.dashboard.core.function.SDK.operation;

import org.apache.eventmesh.dashboard.core.function.SDK.config.CreateEtcdConfig;

import java.util.AbstractMap.SimpleEntry;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import io.etcd.jetcd.KV;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Timeout(value = 5)
public class EtcdSDKCreateOperationTest {

    private static final String url = "http://localhost:2379";
    private final EtcdSDKOperation etcdSDKOperation = new EtcdSDKOperation();

    @Test
    void testCreateClient() {
        final CreateEtcdConfig etcdConfig = CreateEtcdConfig.builder()
            .etcdServerAddress(url)
            .connectTime(5)
            .build();
        SimpleEntry<String, KV> simpleEntry = null;
        try {
            simpleEntry = etcdSDKOperation.createClient(etcdConfig);
            Assertions.assertEquals(url, simpleEntry.getKey());
            simpleEntry.getValue().close();
        } catch (Exception e) {
            log.error("create etcd client failed", e);
            if (simpleEntry != null) {
                simpleEntry.getValue().close();
            }
        }
    }
}
