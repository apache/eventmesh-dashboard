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

package org.apache.eventmesh.dashboard.console.mapper.connector;

import org.apache.eventmesh.dashboard.console.entity.connector.ConnectorEntity;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * Mybatis Mapper for the table of connector.
 */
@Mapper
public interface ConnectorMapper {

    @Select("SELECT * FROM connector WHERE status=1")
    ConnectorEntity selectAll();

    @Select("SELECT * FROM connector WHERE id = #{id}")
    ConnectorEntity selectById(ConnectorEntity connectorEntity);

    @Select("SELECT * FROM connector WHERE cluster_id = #{clusterId}")
    List<ConnectorEntity> selectByClusterId(ConnectorEntity connectorEntity);

    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    @Insert("INSERT INTO connector (cluster_id,name, class_name, type, status, pod_state, config_ids) "
        + "VALUES (#{clusterId}, #{name}, #{className}, #{type}, #{status}, #{podState}, #{configIds})")
    void insert(ConnectorEntity connectorEntity);

    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    @Insert({
        "<script>",
        "   INSERT INTO connector (cluster_id, name, class_name, type, status, pod_state, config_ids) VALUES ",
        "   <foreach collection='list' item='connectorEntity' index='index' separator=','>",
        "       (#{connectorEntity.clusterId}, #{connectorEntity.name}, #{connectorEntity.className},",
        "       #{connectorEntity.type}, #{connectorEntity.status}, #{connectorEntity.podState}, #{connectorEntity.configIds})",
        "   </foreach>",
        "</script>"})
    void batchInsert(List<ConnectorEntity> connectorEntityList);

    @Update("UPDATE connector SET status = #{status} WHERE id = #{id}")
    void updateStatus(ConnectorEntity connectorEntity);

    @Update("UPDATE connector SET pod_state = #{podState} WHERE id = #{id}")
    void updatePodState(ConnectorEntity connectorEntity);

    @Update("UPDATE connector SET config_ids = #{configIds} WHERE id = #{id}")
    void updateConfigIds(ConnectorEntity connectorEntity);

    @Update("UPDATE connector SET status = 0 WHERE cluster_id = #{clusterId}")
    void deActiveById(ConnectorEntity connectorEntity);

    @Update({
        "<script>",
        "   update connector set status = 0 ",
        "   where id in ",
        "   <foreach collection='list' item='item' index='index' open='(' separator=',' close=')'>",
        "       #{item.id}",
        "   </foreach>",
        "</script>"
    })
    void batchDeActive(List<ConnectorEntity> connectorEntities);
}
