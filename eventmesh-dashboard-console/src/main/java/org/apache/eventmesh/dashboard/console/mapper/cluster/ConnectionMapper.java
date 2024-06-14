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

import org.apache.eventmesh.dashboard.console.entity.cluster.ConnectionEntity;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.sql.Timestamp;
import java.util.List;

/**
 * Mybatis Mapper for the table of connection.
 */
@Mapper
public interface ConnectionMapper {


    @Select("SELECT COUNT(*) FROM connection WHERE cluster_id=#{clusterId} AND status=1")
    Integer selectConnectionNumByCluster(ConnectionEntity connectionEntity);

    @Select("SELECT * FROM connection WHERE status = 1")
    List<ConnectionEntity> selectAll();

    @Select("SELECT * FROM connection WHERE cluster_id = #{clusterId} AND status=1")
    List<ConnectionEntity> selectByClusterId(ConnectionEntity connectionEntity);

    @Select({
        "<script>",
        "SELECT * FROM connection",
        "<where>",
        "cluster_id = #{connectionEntity.clusterId} and status=1",
        "<if test='connectionEntity.topic != null'>",
        "and topic like CONCAT('%',#{connectionEntity.topic},'%')",
        "</if>",
        "</where>",
        "</script>"})
    List<ConnectionEntity> selectToFrontByClusterId(@Param("connectionEntity") ConnectionEntity connectionEntity);

    @Select("SELECT * FROM connection WHERE cluster_id = #{clusterId} AND source_id = #{sourceId} AND source_type = #{sourceType} AND status=1")
    public List<ConnectionEntity> selectByClusterIdSourceTypeAndSourceId(ConnectionEntity connectionEntity);

    @Select("SELECT * FROM connection WHERE cluster_id = #{clusterId} AND sink_id = #{sinkId} AND sink_type = #{sinkType}")
    public List<ConnectionEntity> selectByClusterIdSinkTypeAndSinkId(ConnectionEntity connectionEntity);

    @Select("SELECT * FROM connection WHERE cluster_id = #{clusterId} AND source_id = #{sourceId} AND source_type = #{sourceType} "
        + "AND create_time > #{startTime} AND create_time < #{endTime}")
    public List<ConnectionEntity> selectByClusterIdSourceTypeAndSourceIdAndCreateTimeRange(ConnectionEntity connectionEntity,
        @Param("startTime") Timestamp startTime, @Param("endTime") Timestamp endTime);

    @Select("SELECT * FROM connection WHERE cluster_id = #{clusterId} AND sink_id = #{sinkId} AND sink_type = #{sinkType} "
        + "AND create_time > #{startTime} AND create_time < #{endTime}")
    public List<ConnectionEntity> selectByClusterIdSinkTypeAndSinkIdAndCreateTimeRange(ConnectionEntity connectionEntity,
        @Param("startTime") Timestamp startTime, @Param("endTime") Timestamp endTime);

    @Update("UPDATE connection SET status = 1, end_time = NOW() WHERE id = #{id}")
    Integer endConnectionById(ConnectionEntity connectionEntity);

    //batch end
    @Update({
        "<script>",
        "   <foreach collection='list' item='connectionEntity' index='index' separator=';'>",
        "       UPDATE connection SET status = 1, end_time = NOW() WHERE id = #{connectionEntity.id}",
        "   </foreach>",
        "</script>"})
    Integer batchEndConnectionById(List<ConnectionEntity> connectionEntityList);

    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    @Insert("INSERT INTO connection (cluster_id, source_type, source_id," + " sink_type, sink_id, runtime_id, status, topic, group_id, description)"
        + " VALUES ( #{clusterId}, #{sourceType}, #{sourceId}, "
        + " #{sinkType}, #{sinkId},  #{runtimeId}, 1, #{topic}, #{groupId}, #{description})"
        + "ON DUPLICATE KEY UPDATE status = 1")
    void insert(ConnectionEntity connectionEntity);

    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    @Insert({
        "<script>",
        "   INSERT INTO connection (cluster_id, source_type, source_id," + " sink_type, sink_id, runtime_id,",
        "   topic, group_id, description) VALUES ",
        "   <foreach collection='list' item='connectionEntity' index='index' separator=','>",
        "       (#{connectionEntity.clusterId}, #{connectionEntity.sourceType}, #{connectionEntity.sourceId},",
        "       #{connectionEntity.sinkType}, #{connectionEntity.sinkId}, #{connectionEntity.runtimeId},",
        "       #{connectionEntity.topic}, #{connectionEntity.groupId}, #{connectionEntity.description})",
        "   </foreach>",
        "ON DUPLICATE KEY UPDATE status = 1",
        "</script>"})
    Integer batchInsert(List<ConnectionEntity> connectionEntityList);


}
