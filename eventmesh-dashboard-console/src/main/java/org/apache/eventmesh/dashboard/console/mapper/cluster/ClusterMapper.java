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

import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.modle.ClusterIdDTO;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/**
 * cluster table operation
 *
 */

@Mapper
public interface ClusterMapper {

    @Select("select * from cluster where id=#{id} and status=1")
    ClusterEntity selectClusterById(ClusterEntity cluster);

    @Select("select * from cluster where status=1")
    List<ClusterEntity> selectAllCluster();

    @Select("SELECT * FROM cluster where update_time >  #{updateTime}")
    List<ClusterEntity> selectClusterByUpdate(@Param("updateTime") long updateTime);

    @Select({
        "<script>",
        "",
        "",
        "</script>"
    })
    Map<String, Integer> queryHomeClusterData(ClusterIdDTO clusterIdDTO);

    @Update({"update cluster set name=#{name},jmx_properties=#{jmxProperties},description=#{description},auth_type=#{authType},run_state=#{runState}",
        " where id=#{id}"
    })
    Integer updateClusterById(ClusterEntity cluster);

    @Update("UPDATE cluster SET status=0 WHERE id=#{id}")
    Integer deactivate(ClusterEntity clusterEntity);

    @Insert({
        "<script>",
        "   insert into cluster (name,trusteeship_type, cluster_type, version, jmx_properties, description, auth_type) values ",
        "   <foreach collection='list' item='c' index='index' separator=','>",
        "   (#{c.name}, #{c.trusteeshipType}, #{c.clusterType}, #{c.version}, , #{c.jmxProperties}, #{c.description}, #{c.authType})",
        "   </foreach>",
        "</script>"})
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    Integer batchInsert(List<ClusterEntity> clusterEntities);

    @Insert({
        "insert into cluster(name,trusteeship_type,cluster_type,version,jmx_properties,description,auth_type)values",
        "(#{name},#{c.trusteeshipType},#{c.clusterType},#{c.version},#{jmxProperties},#{regProperties},#{description},#{authType})"
    })
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    void insertCluster(ClusterEntity cluster);

}
