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


package org.apache.eventmesh.dashboard.core.remoting.kafka;

import org.apache.eventmesh.dashboard.common.annotation.RemotingServiceMapper;
import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.model.remoting.BaseGlobalResult;
import org.apache.eventmesh.dashboard.core.remoting.AbstractRemotingService;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.common.KafkaFuture;

import java.util.concurrent.TimeUnit;

@RemotingServiceMapper(clusterType = {ClusterType.STORAGE_KAFKA_BROKER, ClusterType.STORAGE_KAFKA_RAFT})
public abstract class AbstractKafkaRemotingService extends AbstractRemotingService<AdminClient> {

    protected static final int ADMIN_TIMEOUT_MS = 10000;

    protected <T> T awaitResult(KafkaFuture<T> future) throws Exception {
        try {
            return future.get(ADMIN_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        }
    }

    protected String requireName(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    protected BaseGlobalResult successfulResult() {
        BaseGlobalResult result = new BaseGlobalResult();
        result.setCode(200);
        return result;
    }
}
