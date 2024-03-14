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

package org.apache.eventmesh.dashboard.console.mapper.health;

import org.apache.eventmesh.dashboard.console.entity.health.HealthCheckResultEntity;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.sql.Timestamp;
import java.util.List;

/**
 * Mapper for health check result
 */
@Mapper
public interface HealthCheckResultMapper {

    @Select("SELECT * FROM health_check_result")
    List<HealthCheckResultEntity> selectAll();

    @Select("SELECT * FROM health_check_result WHERE id = #{id}")
    HealthCheckResultEntity selectById(HealthCheckResultEntity healthCheckResultEntity);

    @Select("SELECT * FROM health_check_result WHERE cluster_id = #{clusterId} AND type = #{type} AND type_id = #{typeId}")
    List<HealthCheckResultEntity> selectByClusterIdAndTypeAndTypeId(HealthCheckResultEntity healthCheckResultEntity);

    @Select("SELECT * FROM health_check_result WHERE cluster_id = #{clusterId} AND type = #{type}")
    List<HealthCheckResultEntity> selectByClusterIdAndType(HealthCheckResultEntity healthCheckResultEntity);

    @Select("SELECT * FROM health_check_result WHERE cluster_id = #{clusterId} AND create_time > #{startTime} AND create_time < #{endTime}")
    List<HealthCheckResultEntity> selectByClusterIdAndCreateTimeRange(@Param("clusterId") Long clusterId,
        @Param("startTime") Timestamp startTime, @Param("endTime") Timestamp endTime);

    @Options(useGeneratedKeys = true, keyProperty = "id")
    @Insert("INSERT INTO health_check_result(type,type_id, cluster_id, state,result_desc)"
        + " VALUES( #{type}, #{typeId}, #{clusterId}, #{state}, #{resultDesc})")
    void insert(HealthCheckResultEntity healthCheckResultEntity);

    @Insert({
        "<script>",
        "   INSERT INTO health_check_result(type, type_id, cluster_id, state, result_desc) VALUES ",
        "   <foreach collection='list' item='healthCheckResultEntity' index='index' separator=','>",
        "       (#{healthCheckResultEntity.type}, #{healthCheckResultEntity.typeId}, #{healthCheckResultEntity.clusterId},",
        "       #{healthCheckResultEntity.state}, #{healthCheckResultEntity.resultDesc})",
        "   </foreach>",
        "</script>"
    })
    void batchInsert(List<HealthCheckResultEntity> healthCheckResultEntityList);

    @Update("UPDATE health_check_result SET state = #{state}, result_desc = #{resultDesc} WHERE id = #{id}")
    void update(HealthCheckResultEntity healthCheckResultEntity);

    @Update({
        "<script>",
        "   <foreach collection='list' item='healthCheckResultEntity' index='index' separator=';'>",
        "       UPDATE health_check_result SET state = #{healthCheckResultEntity.state},",
        "       result_desc = #{healthCheckResultEntity.resultDesc} WHERE id = #{healthCheckResultEntity.id}",
        "   </foreach>",
        "</script>"})
    void batchUpdate(List<HealthCheckResultEntity> healthCheckResultEntityList);

    @Select({
        "<script>",
        "   SELECT * FROM health_check_result",
        "   WHERE (cluster_id, type, type_id, state) IN",
        "   <foreach collection='list' item='item' open='(' separator=',' close=')'>",
        "       (#{item.clusterId}, #{item.type}, #{item.typeId}, 2)",
        "   </foreach>",
        "   ORDER BY create_time DESC",
        "</script>"
    })
    List<HealthCheckResultEntity> getIdsNeedToBeUpdateByClusterIdAndTypeAndTypeId(List<HealthCheckResultEntity> healthCheckResultEntityList);

}
