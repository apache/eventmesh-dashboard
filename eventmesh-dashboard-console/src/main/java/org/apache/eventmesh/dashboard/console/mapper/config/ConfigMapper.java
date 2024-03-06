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

package org.apache.eventmesh.dashboard.console.mapper.config;

import org.apache.eventmesh.dashboard.console.entity.config.ConfigEntity;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * config table operation
 */
@Mapper
public interface ConfigMapper {

    @Insert("INSERT INTO config (cluster_id, business_type, instance_type, instance_id, config_name,"
        + " config_value, start_version,eventmesh_version, description, edit,end_version,is_default,is_modify) VALUE "
        + "(#{clusterId},#{businessType},#{instanceType},#{instanceId},#{configName},"
        + "#{configValue},#{startVersion},#{eventmeshVersion},#{description},#{edit},#{endVersion},#{isDefault},#{isModify})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    Integer addConfig(ConfigEntity configEntity);

    @Update("update config set status=2 where id=#{id}")
    Integer deleteConfig(ConfigEntity configEntity);

    @Update("update config set config_value=#{configValue} where status=1 and edit=2")
    void updateConfig(ConfigEntity configEntity);

    @Select("select * from config where business_type=#{businessType} and instance_type=#{instanceType} "
        + "and instance_id=#{instanceId}")
    List<ConfigEntity> selectByInstanceId(ConfigEntity configEntity);

    @Select("select * from config where cluster_id=-1 and business_type=#{businessType} and instance_type=#{instanceType}")
    List<ConfigEntity> selectDefaultConfig(ConfigEntity configEntity);


    @Select("select * from config where cluster_id=#{clusterId} and instance_type=#{instanceType} "
        + "and instance_id=#{instanceId} and config_name=#{configName} and status=1")
    ConfigEntity selectByUnique(ConfigEntity configEntity);
}
