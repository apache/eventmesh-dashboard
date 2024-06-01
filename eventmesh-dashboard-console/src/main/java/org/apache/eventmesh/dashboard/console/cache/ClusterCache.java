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


import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import lombok.Getter;

public class ClusterCache {

    @Getter
    private static final ClusterCache INSTANCE = new ClusterCache();

    //cluster name
    private HashMap<String, ClusterEntity> clusterNameMap = new HashMap<>();

    private HashMap<Long, ClusterEntity> clusterIdMap = new HashMap<>();

    private static final Object lock = new Object();

    public ClusterEntity getClusterById(Long id) {
        return clusterIdMap.get(id);
    }

    public ClusterEntity getClusterByName(String name) {
        return clusterNameMap.get(name);
    }

    public ClusterEntity getClusterByRegistryAddress(String registryAddress) {
        for (ClusterEntity clusterEntity : clusterIdMap.values()) {
            if (clusterEntity.getRegistryAddress().equals(registryAddress)) {
                return clusterEntity;
            }
        }
        return null;
    }

    public List<ClusterEntity> getClusters() {
        return new ArrayList<>(INSTANCE.clusterIdMap.values());
    }

    public void addCluster(ClusterEntity clusterEntity) {
        synchronized (lock) {
            if (INSTANCE.clusterIdMap.containsKey(clusterEntity.getId())
                || INSTANCE.clusterNameMap.containsKey(clusterEntity.getName())) {
                return;
            }
            INSTANCE.clusterIdMap.put(clusterEntity.getId(), clusterEntity);
            INSTANCE.clusterNameMap.put(clusterEntity.getName(), clusterEntity);
        }
    }

    public void deleteClusterById(Long id) {
        synchronized (lock) {
            ClusterEntity clusterEntity = INSTANCE.clusterIdMap.get(id);
            INSTANCE.clusterIdMap.remove(id);
            INSTANCE.clusterNameMap.remove(clusterEntity.getName());
        }
    }

    public void deleteClusterByName(String name) {
        synchronized (lock) {
            ClusterEntity clusterEntity = INSTANCE.clusterNameMap.get(name);
            INSTANCE.clusterNameMap.remove(name);
            INSTANCE.clusterIdMap.remove(clusterEntity.getId());
        }
    }

    public void syncClusters(List<ClusterEntity> clusters) {
        // TODO 性能问题严总
        synchronized (lock) {
            INSTANCE.clusterIdMap.clear();
            INSTANCE.clusterNameMap.clear();
            for (ClusterEntity clusterEntity : clusters) {
                INSTANCE.clusterIdMap.put(clusterEntity.getId(), clusterEntity);
                INSTANCE.clusterNameMap.put(clusterEntity.getName(), clusterEntity);
            }
        }
    }
}
