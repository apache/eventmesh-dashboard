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

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * cluster table operation
 */
@Mapper
public interface ClusterMapper {

    @Select("SELECT * FROM cluster WHERE is_delete=0")
    List<ClusterEntity> selectAllCluster();

    @Select("SELECT * FROM cluster WHERE id=#{id} AND is_delete=0")
    ClusterEntity selectClusterById(ClusterEntity cluster);

    @Insert("INSERT INTO cluster (name, register_name_list, bootstrap_servers, eventmesh_version, client_properties, "
        + "jmx_properties, reg_properties, description, auth_type, run_state) VALUES (#{name},#{registerNameList},"
        + "#{bootstrapServers},#{eventmeshVersion},#{clientProperties},#{jmxProperties},#{regProperties},#{description},#{authType},#{runState})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    void addCluster(ClusterEntity cluster);

    @Update("UPDATE cluster SET name =#{name},reg_properties=#{regProperties},bootstrap_servers=#{bootstrapServers},"
        + "eventmesh_version=#{eventmeshVersion},client_properties=#{clientProperties},jmx_properties=#{jmxProperties},"
        + "reg_properties=#{regProperties},description=#{description},auth_type=#{authType},run_state=#{runState} ,"
        + "register_name_list=#{registerNameList} WHERE id=#{id}")
    void updateClusterById(ClusterEntity cluster);

    @Delete("UPDATE cluster SET is_delete=1 WHERE id=#{id}")
    void deleteClusterById(ClusterEntity clusterEntity);

}
