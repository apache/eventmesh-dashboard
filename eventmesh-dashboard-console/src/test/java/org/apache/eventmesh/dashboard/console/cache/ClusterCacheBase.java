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

import org.junit.jupiter.api.BeforeAll;

public class ClusterCacheBase {

    @BeforeAll
    public static void addTestCluster() {
        ClusterEntity clusterEntity = new ClusterEntity();
        clusterEntity.setName("cluster1");
        clusterEntity.setRegistryAddress("registryList");
        clusterEntity.setBootstrapServers("server");
        clusterEntity.setVersion("1.7.0");
        clusterEntity.setClientProperties("");
        clusterEntity.setJmxProperties("");
        clusterEntity.setRegProperties("");
        clusterEntity.setDescription("");
        clusterEntity.setAuthType(0);
        clusterEntity.setRunState(0);
        clusterEntity.setStatus(0);
        clusterEntity.setId(0L);
        clusterEntity.setClusterId(0L);

        clusterEntity.setId(1L);
        ClusterCache.getINSTANCE().addCluster(clusterEntity);
    }
}
