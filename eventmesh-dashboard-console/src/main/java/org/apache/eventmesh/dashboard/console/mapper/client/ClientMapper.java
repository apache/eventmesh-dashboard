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

package org.apache.eventmesh.dashboard.console.mapper.client;

import org.apache.eventmesh.dashboard.console.entity.client.ClientEntity;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * Mybatis Mapper for the table of client.
 */
@Mapper
public interface ClientMapper {

    @Select("SELECT * FROM `client` WHERE `id` = #{id}")
    ClientEntity selectById(Long id);

    @Select("SELECT * FROM `client` WHERE `cluster_id` = #{clusterId}")
    ClientEntity selectByClusterId(Long clusterId);

    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    @Insert(
        "INSERT INTO `client` (`cluster_id`, `name`, `platform`,"
            + " `language`, `pid`, `host`, `port`, `protocol`,"
            + " `status`, `config_ids`, `description`, `end_time`) "
            + "VALUES (#{clusterId}, #{name}, #{platform},"
            + " #{language}, #{pid}, #{host}, #{port}, #{protocol},"
            + " #{status}, #{configIds}, #{description}, #{endTime})")
    void insert(ClientEntity clientEntity);

    @Update("UPDATE `client` SET status = #{status}, end_time = NOW() WHERE id = #{id}")
    void deActive(ClientEntity clientEntity);

    @Update("UPDATE `client` SET status = #{status} WHERE id = #{id}")
    void updateStatus(ClientEntity clientEntity);

    @Delete("DELETE FROM `client` WHERE id = #{id}")
    void deleteById(Long id);
}
