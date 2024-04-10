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

import java.nio.charset.StandardCharsets;
import java.util.AbstractMap.SimpleEntry;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.KV;
import io.etcd.jetcd.KeyValue;
import io.etcd.jetcd.kv.GetResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EtcdSDKCreateOperationTest {

    private final EtcdSDKOperation etcdSDKOperation = new EtcdSDKOperation();

    private static final String key = "/test/foo";

    private static final String value = "test";

    private static final String url = "http://localhost:2379";

    @Test
    void testCreateClient() {
        // final CreateEtcdConfig etcdConfig = CreateEtcdConfig.builder()
        //     .etcdServerAddress(url)
        //     .connectTime(10L)
        //     .build();
        // // etcdConfig.setEtcdServerAddress(url);
        // SimpleEntry<String, KV> simpleEntry = null;
        // try {
        //     simpleEntry = etcdSDKOperation.createClient(etcdConfig);
        //     Assertions.assertEquals(url, simpleEntry.getKey());
        //     simpleEntry.getValue().put(bytesOf(key), bytesOf(value));
        //     final GetResponse response = simpleEntry.getValue().get(bytesOf(key)).get();
        //     final List<KeyValue> keyValues = response.getKvs();
        //     log.info("get key = {} , value = {} from etcd success",
        //         keyValues.get(0).getKey().toString(StandardCharsets.UTF_8),
        //         keyValues.get(0).getValue().toString(StandardCharsets.UTF_8));
        //     simpleEntry.getValue().close();
        // } catch (Exception e) {
        //     log.error("create etcd client failed", e);
        //     if (simpleEntry != null) {
        //         simpleEntry.getValue().close();
        //     }
        // }
    }

    private static ByteSequence bytesOf(String val) {
        return ByteSequence.from(val, StandardCharsets.UTF_8);
    }
}
