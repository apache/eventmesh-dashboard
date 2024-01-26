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

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * Mybatis Mapper for the table of connector.
 */
@Mapper
public interface ConnectorMapper {
    @Select("SELECT * FROM connector WHERE id = #{id}")
    ConnectorEntity selecyById(Long id);

    @Select("SELECT * FROM connector WHERE connect_cluster_id = #{connectClusterId}")
    ConnectorEntity selectByConnectClusterId(Long connectClusterId);

    @Delete("DELETE FROM connector WHERE id = #{id}")
    void deleteById(Long id);

    @Update("UPDATE connector SET state = #{state} WHERE id = #{id}")
    void updateStateById(ConnectorEntity connectorEntity);

    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    @Insert("INSERT INTO connector (connect_cluster_id, connector_name, connector_class_name, connector_type, state, topics, task_count) VALUES (#{connectClusterId}, #{connectorName}, #{connectorClassName}, #{connectorType}, #{state}, #{topics}, #{taskCount})")
    void insert(ConnectorEntity connectorEntity);
}
