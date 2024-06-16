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

package org.apache.eventmesh.dashboard.console.mapper.function;

import org.apache.eventmesh.dashboard.console.entity.function.ConfigEntity;

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

    @Select({
        "<script>",
        "SELECT * FROM config",
        "<where>",
        "instance_type = #{instanceType} and instance_id = #{instanceId}",
        "<if test='isModify != null'>",
        "and is_modify = #{isModify}",
        "</if>",
        "<if test='alreadyUpdate != null'>",
        "and already_update = #{alreadyUpdate}",
        "</if>",
        "<if test='configName != null'>",
        "and config_name like CONCAT('%',#{configName},'%') and is_default=0",
        "</if>",
        "</where>",
        "</script>"})
    List<ConfigEntity> selectConfigsToFrontWithDynamic(ConfigEntity configEntity);

    @Select("SELECT * FROM config WHERE business_type=#{businessType} AND is_default=1")
    List<ConfigEntity> selectConnectorConfigsByBusinessType(ConfigEntity configEntity);

    @Select("SELECT DISTINCT business_type FROM config WHERE instance_type=2 AND is_default=1 AND business_type LIKE CONCAT('%',#{businessType},'%')")
    List<String> selectConnectorBusinessType(ConfigEntity configEntity);

    @Select("SELECT * FROM config WHERE status=1 AND is_default=0")
    List<ConfigEntity> selectAll();

    @Select("SELECT * FROM config WHERE instance_type=#{instanceType} AND instance_id=#{instanceId}")
    List<ConfigEntity> selectConfigsByInstance(ConfigEntity configEntity);

    @Select("SELECT * FROM config WHERE instance_type=#{instanceType} AND instance_id=#{instanceId} AND is_default=0")
    List<ConfigEntity> selectByInstanceId(ConfigEntity configEntity);

    @Select("SELECT * FROM config WHERE cluster_id=-1 AND business_type=#{businessType} AND instance_type=#{instanceType} AND is_default=1")
    List<ConfigEntity> selectDefaultConfig(ConfigEntity configEntity);

    @Select("SELECT * FROM config WHERE is_default=1")
    List<ConfigEntity> selectAllDefaultConfig();

    @Select("SELECT * FROM config WHERE cluster_id=#{clusterId} AND instance_type=#{instanceType} "
        + "AND instance_id=#{instanceId} AND config_name=#{configName} AND status=1")
    ConfigEntity selectByUnique(ConfigEntity configEntity);

    @Update("UPDATE config SET status=0 WHERE id=#{id}")
    Integer deleteConfig(ConfigEntity configEntity);

    @Update("UPDATE config SET config_value=#{configValue} ,already_update=#{alreadyUpdate} WHERE instance_type=#{instanceType} AND"
        + " instance_id=#{instanceId} AND config_name=#{configName} AND is_default=0")
    Integer updateConfig(ConfigEntity configEntity);

    @Insert({
        "<script>",
        "   INSERT INTO config (cluster_id, business_type, instance_type, instance_id, config_name, config_value, start_version,",
        "   eventmesh_version,end_version, diff_type, description, edit, is_default, is_modify) VALUES ",
        "   <foreach collection='list' item='c' index='index' separator=','>",
        "   (#{c.clusterId}, #{c.businessType}, #{c.instanceType}, #{c.instanceId},#{c.configName},",
        "   #{c.configValue}, #{c.startVersion}, #{c.eventmeshVersion},#{c.endVersion},#{c.diffType},#{c.description},",
        "   #{c.edit},#{c.isDefault},#{c.isModify})",
        "   </foreach>",
        "</script>"})
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    Integer batchInsert(List<ConfigEntity> configEntityList);

    @Insert("INSERT INTO config (cluster_id, business_type, instance_type, instance_id, config_name, config_value, "
        + "status, is_default,  diff_type, description, edit, is_modify,start_version,"
        + "eventmesh_version,end_version) VALUE "
        + "(#{clusterId},#{businessType},#{instanceType},#{instanceId},#{configName},"
        + "#{configValue},#{status},#{isDefault},#{diffType},#{description},#{edit},#{isModify},"
        + "#{startVersion},#{eventmeshVersion},#{endVersion})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    void insertConfig(ConfigEntity configEntity);

}
