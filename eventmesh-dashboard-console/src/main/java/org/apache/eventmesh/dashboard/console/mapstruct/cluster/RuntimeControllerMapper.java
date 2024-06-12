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

package org.apache.eventmesh.dashboard.console.mapstruct.cluster;

import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.modle.ClusterIdDTO;
import org.apache.eventmesh.dashboard.console.modle.IdDTO;
import org.apache.eventmesh.dashboard.console.modle.cluster.runtime.CrateRuntimeDTO;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 *
 */
@Mapper
public interface RuntimeControllerMapper {

    RuntimeControllerMapper INSTANCE = Mappers.getMapper(RuntimeControllerMapper.class);


    RuntimeEntity queryRuntimeListByClusterId(ClusterIdDTO clusterIdDTO);

    RuntimeEntity queryRuntimeListById(IdDTO idDTO);

    RuntimeEntity crateRuntime(CrateRuntimeDTO crateRuntimeDTO);


}
