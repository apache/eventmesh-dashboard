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

package org.apache.eventmesh.dashboard.console.cache;

import org.apache.eventmesh.dashboard.console.entity.runtime.RuntimeEntity;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RuntimeCache {

    private static final RuntimeCache INSTANCE = new RuntimeCache();
    // ip:port -> runtime
    private Map<String, RuntimeEntity> runtimeMap = new ConcurrentHashMap<>();

    private RuntimeCache() {
    }

    public static final RuntimeCache getInstance() {
        return INSTANCE;
    }

    public void addRuntime(RuntimeEntity runtimeEntity) {
        runtimeMap.put(runtimeEntity.getHost() + ":" + runtimeEntity.getPort(), runtimeEntity);
    }

    public Collection<RuntimeEntity> getRuntimeList() {
        return runtimeMap.values();
    }

    public void deleteRuntime(RuntimeEntity runtimeEntity) {
        runtimeMap.remove(runtimeEntity.getHost() + ":" + runtimeEntity.getPort());
    }

    public void replaceAllRuntime(List<RuntimeEntity> runtimeEntities) {
        Map<String, RuntimeEntity> newRuntimeList = new ConcurrentHashMap<>();
        runtimeEntities.forEach(runtimeEntity -> {
            newRuntimeList.put(runtimeEntity.getHost() + ":" + runtimeEntity.getPort(), runtimeEntity);
        });
        runtimeMap = newRuntimeList;
    }
}
