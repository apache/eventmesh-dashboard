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

package org.apache.eventmesh.dashboard.console.service.cluster.impl;

import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.mapper.cluster.ClusterMapper;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterService;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class ClusterServiceImpl implements ClusterService {

    @Autowired
    private ClusterMapper clusterMapper;

    @Override
    public void addCluster(ClusterEntity cluster) {
        clusterMapper.addCluster(cluster);
    }

    @Override
    public List<ClusterEntity> selectAllCluster() {
        return clusterMapper.selectAllCluster();
    }

    @Override
    public ClusterEntity selectClusterById(ClusterEntity cluster) {
        return clusterMapper.selectClusterById(cluster);
    }

    @Override
    public void updateClusterById(ClusterEntity cluster) {
        clusterMapper.updateClusterById(cluster);
    }

    @Override
    public void deleteClusterById(ClusterEntity cluster) {
        clusterMapper.deleteClusterById(cluster);
    }

}
