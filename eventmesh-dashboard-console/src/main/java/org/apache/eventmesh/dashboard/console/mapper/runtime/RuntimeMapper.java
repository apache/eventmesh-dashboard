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

package org.apache.eventmesh.dashboard.console.mapper.runtime;

import org.apache.eventmesh.dashboard.console.entity.runtime.RuntimeEntity;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * runtime table operation
 */
@Mapper
public interface RuntimeMapper {

    @Insert("INSERT INTO runtime (cluster_id, host, storage_cluster_id, port, jmx_port, start_timestamp, rack, status, "
        + "endpoint_map) VALUES(#{clusterId},#{host},#{storageClusterId},#{port},#{jmxPort},#{startTimestamp},#{rack},#{status},#{endpointMap})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    void addRuntime(RuntimeEntity runtimeEntity);

    @Select("SELECT * FROM runtime WHERE cluster_id=#{clusterId} AND is_delete=0")
    List<RuntimeEntity> selectRuntimeByCluster(RuntimeEntity runtimeEntity);

    @Update("UPDATE runtime SET port=#{port} ,jmx_port=#{jmxPort} ,status=#{status} WHERE cluster_id=#{clusterId} AND is_delete=0")
    void updateRuntimeByCluster(RuntimeEntity runtimeEntity);

    @Delete("UPDATE runtime SET is_delete=1 WHERE cluster_id=#{clusterId}")
    void deleteRuntimeByCluster(RuntimeEntity runtimeEntity);

}
