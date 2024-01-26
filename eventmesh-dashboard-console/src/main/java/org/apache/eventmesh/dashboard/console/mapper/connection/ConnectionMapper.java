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

package org.apache.eventmesh.dashboard.console.mapper.connection;

import org.apache.eventmesh.dashboard.console.entity.connection.ConnectionEntity;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Mybatis Mapper for the table of connection.
 */
@Mapper
public interface ConnectionMapper {

    @Select("SELECT * FROM connection WHERE id = #{id}")
    ConnectionEntity selectById(ConnectionEntity connectionEntity);

    @Select("SELECT * FROM connection")
    List<ConnectionEntity> selectAll();

    @Select("SELECT * FROM connection WHERE cluster_phy_id = #{clusterPhyId}")
    List<ConnectionEntity> selectByClusterPhyId(ConnectionEntity connectionEntity);

    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    @Insert("INSERT INTO connection (cluster_phy_id, source_type, source_id, source_status, sink_type, sink_id, sink_status, runtime_id, status, topic, group_id, group_name, description) VALUES ( #{clusterPhyId}, #{sourceType}, #{sourceId}, #{sourceStatus}, #{sinkType}, #{sinkId}, #{sinkStatus}, #{runtimeId}, #{status}, #{topic}, #{groupId}, #{groupName}, #{description})")
    void insert(ConnectionEntity connectionEntity);

    @Insert("<script>"
        + "INSERT INTO connection (cluster_phy_id, source_type, source_id, source_status, sink_type, sink_id, sink_status, runtime_id, status, topic, group_id, group_name, description) VALUES "
        +
        "<foreach collection='list' item='connectionEntity' index='index' separator=','>"
        + "(#{connectionEntity.clusterPhyId}, #{connectionEntity.sourceType}, #{connectionEntity.sourceId}, #{connectionEntity.sourceStatus}, #{connectionEntity.sinkType}, #{connectionEntity.sinkId}, #{connectionEntity.sinkStatus}, #{connectionEntity.runtimeId}, #{connectionEntity.status}, #{connectionEntity.topic}, #{connectionEntity.groupId}, #{connectionEntity.groupName}, #{connectionEntity.description})"
        + "</foreach>"
        + "</script>")
    void batchInsert(List<ConnectionEntity> connectionEntityList);


    @Delete("DELETE FROM connection WHERE cluster_phy_id = #{clusterPhyId}")
    void deleteAllByClusterPhyId(ConnectionEntity connectionEntity);

    @Delete("DELETE FROM connection WHERE id = #{id}")
    void deleteById(ConnectionEntity connectionEntity);

    @Delete("<script>"
        + "DELETE FROM connection WHERE id IN "
        + "<foreach item='item' index='index' collection='list' open='(' separator=',' close=')'>"
        + "#{item.id}"
        + "</foreach>"
        + "</script>")
    void batchDelete(List<ConnectionEntity> connectionEntityList);

}
