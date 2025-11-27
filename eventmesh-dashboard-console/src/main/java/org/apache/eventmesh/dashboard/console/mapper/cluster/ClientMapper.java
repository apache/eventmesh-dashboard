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

import org.apache.eventmesh.dashboard.console.entity.cluster.ClientEntity;
import org.apache.eventmesh.dashboard.console.mapper.SyncDataHandlerMapper;
import org.apache.eventmesh.dashboard.console.model.dto.cluster.client.QueryClientByUserFormDTO;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;


/**
 * Mybatis Mapper for the table of client.
 */
@Mapper
public interface ClientMapper extends SyncDataHandlerMapper<ClientEntity> {


    @Select({
        "<script>",
        " INSERT INTO  client (cluster_id, name, platform, language, pid, host, port, protocol, status,",
        " config_ids, description) VALUES ",
        " <foreach collection='list' item='c' index='index' separator=','>",
        "     (#{c.clusterId}, #{c.name}, #{c.platform},#{c.language}, #{c.pid}, #{c.host}, #{c.port}, #{c.protocol},",
        "     #{c.status}, #{c.configIds}, #{c.description})",
        " </foreach>",
        "</script>"})
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    void batchInsert(List<ClientEntity> clientEntityList);


    @Select({"""
            <script>
                select * from client where cluster_id = #{clusterId}
                <where>
                    <if text="runtimeId != null and runtimeId != ''">
                            runtime_id = #{runtimeId}
                     </if>
                     <if text="userId != null and userId != ''">
                            user_id = #{userId}
                     </if>
                     <if text="host != null and host != ''">
                        `host` = #{host}
                     </if>
                </where>
                and status = #{status}
            </script>
            """})
    List<ClientEntity> queryClientByUserForm(QueryClientByUserFormDTO queryClientByUserFormDTO);

    @Select("SELECT * FROM `client` WHERE `host` = #{host} AND `port` = #{port} AND status = 1")
    List<ClientEntity> selectByHostPort(ClientEntity clientEntity);

    @Select("SELECT * FROM `client` WHERE `id` = #{id}")
    ClientEntity selectById(ClientEntity clientEntity);

    @Select("SELECT * FROM `client` WHERE `cluster_id` = #{clusterId}")
    List<ClientEntity> selectByClusterId(ClientEntity clientEntity);

    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    @Insert(
        "INSERT INTO `client` (`cluster_id`, `name`, `platform`,"
        + "`language`, `pid`, `host`, `port`, `protocol`,"
        + "`status`, `config_ids`, `description`) "
        + "VALUES (#{clusterId}, #{name}, #{platform},"
        + "#{language}, #{pid}, #{host}, #{port}, #{protocol},"
        + "#{status}, #{configIds}, #{description})"
        + "ON DUPLICATE KEY UPDATE `status` = 1, `pid` = #{pid}, `config_ids` = #{configIds}, `host` = #{host}, `port` = #{port}")
    Long insert(ClientEntity clientEntity);

    @Update("UPDATE `client` SET status = 0, end_time = NOW() WHERE id = #{id}")
    void deactivate(ClientEntity clientEntity);

    @Update("UPDATE `client` SET status = 0, end_time = NOW() WHERE `host` = #{host} AND `port` = #{port}")
    void deActiveByHostPort(ClientEntity clientEntity);


    void syncInsert(List<ClientEntity> entityList);

    void syncUpdate(List<ClientEntity> entityList);

    void syncDelete(List<ClientEntity> entityList);

    List<ClientEntity> syncGet(ClientEntity topicEntity);
}
