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

package org.apache.eventmesh.dashboard.console.mapper.cluster;

import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * runtime table operation
 */
@Mapper
public interface RuntimeMapper {

    @Select("SELECT COUNT(*) FROM runtime WHERE cluster_id=#{clusterId} AND status=1")
    Integer selectRuntimeNumByCluster(RuntimeEntity runtimeEntity);

    @Select("SELECT * FROM runtime WHERE status=1")
    List<RuntimeEntity> selectAll();

    @Select("select * from runtime where cluster_id=#{clusterid} and status=1")
    List<RuntimeEntity> selectRuntimeByCluster(RuntimeEntity runtimeEntity);


    @Select("select * from runtime where id=#{id} and status=1")
    RuntimeEntity queryRuntimeEntityById(RuntimeEntity runtimeEntity);

    @Select({
        "<script>",
        "SELECT * FROM runtime",
        "<where>",
        "cluster_id =#{runtimeEntity.clusterId}",
        "<if test='runtimeEntity.host!=null'>",
        "and host like CONCAT('%',#{runtimeEntity.host},'%')",
        "</if>",
        "</where>",
        "</script>"})
    List<RuntimeEntity> selectRuntimesToFrontByCluster(@Param("runtimeEntity") RuntimeEntity runtimeEntity);

    @Select("SELECT * FROM runtime WHERE host = #{host} and port = #{port} and status = 1")
    List<RuntimeEntity> selectByHostPort(RuntimeEntity runtimeEntity);

    @Update("UPDATE runtime SET port=#{port} ,jmx_port=#{jmxPort} ,status=#{status} WHERE cluster_id=#{clusterId} AND status=1")
    Integer updateRuntimeByCluster(RuntimeEntity runtimeEntity);

    @Update("UPDATE runtime SET status=0 WHERE cluster_id=#{clusterId}")
    Integer deleteRuntimeByCluster(RuntimeEntity runtimeEntity);

    @Update("UPDATE runtime SET status = 0 WHERE id = #{id}")
    Integer deactivate(RuntimeEntity runtimeEntity);

    @Insert({
        "<script>",
        "   INSERT INTO runtime (cluster_id, host, storage_cluster_id, port, jmx_port, start_timestamp, rack, status, endpoint_map) VALUES",
        "   <foreach collection='list' item='c' index='index' separator=','>",
        "   (#{c.clusterId},#{c.host},#{c.storageClusterId},#{c.port},#{c.jmxPort},NOW(),#{c.rack},#{c.status},#{c.endpointMap})",
        "   </foreach>",
        "</script>"})
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    Integer batchInsert(List<RuntimeEntity> runtimeEntities);

    @Insert("INSERT INTO runtime (cluster_id, host, storage_cluster_id, port, jmx_port, start_timestamp, rack, status, "
        + "endpoint_map) VALUES(#{clusterId},#{host},#{storageClusterId},#{port},#{jmxPort},NOW(),#{rack},#{status},#{endpointMap})"
        + " ON DUPLICATE KEY UPDATE status=1,start_timestamp = now()")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    void insertRuntime(RuntimeEntity runtimeEntity);

}
