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

import org.apache.eventmesh.dashboard.common.enums.MetadataType;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.entity.function.ConfigEntity;
import org.apache.eventmesh.dashboard.console.mapper.SyncDataHandlerMapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * config table operation
 */
@Mapper
public interface ConfigMapper extends SyncDataHandlerMapper<ConfigEntity> {


    @Select("""
             select * from config where cluster_id
                 <foreach item='item' index='index' open='in(' separator=',' close=')'>
                    #{item.id}
                 </foreach>
                 and status = 1
        """)
    List<ConfigEntity> queryByClusterIdList(List<ClusterEntity> clusterConfigEntityList);

    @Select("""
        <script>
            <foreach item='item' index='index' separator=' union all '>
                 select * from config where
                                instance_id = #{instanceId}
                            and instance_type = #{instanceType}
                            and config_name = #{configName}
             </foreach>
        </script>
        """)
    List<ConfigEntity> queryByRuntimeIdAndConfigName(List<ConfigEntity> configEntityLists);

    @Select("""
        select * from config where instance_id
             <foreach item='item' index='index' open='in(' separator=',' close=')'>
                #{item.id}
             </foreach>
             and instance_type = #{instanceType}
             and status = 1
        """)
    List<ConfigEntity> queryByInstanceIdList(List<ConfigEntity> configEntityList, MetadataType metadataType);

    @Select("""
        <script>
        select * from config where
            instance_id = #{instanceId} and instance_type = #{instanceType}
            <if test='isModify != null'>
                and is_modify = #{isModify}
            </if>
            <if test='alreadyUpdate != null'>
                and already_update = #{alreadyUpdate}
            </if>
            <if test='configName != null'>
                and config_name like CONCAT('%',#{configName},'%') and is_default=0
            </if>
        </script>
        """)
    List<ConfigEntity> getConfigsToFrontWithDynamic(ConfigEntity configEntity);


    @Select("SELECT * FROM config WHERE status=1 AND is_default=0")
    List<ConfigEntity> selectAll();

    @Select("select * from config where instance_type=#{instanceType} and instance_id=#{instanceId}")
    List<ConfigEntity> selectConfigsByInstance(ConfigEntity configEntity);


    @Update("""
        <script>
            <foreach item='item' index='index' separator=' union all '>
                     update set config_value=#{configValue} 
                        <if test='configValueRange != null'>  >
                            , config_value_range = #{configValueRange}
                         </if>
                        from  config
                        where instance_id = #{instanceId}
                          and instance_type = #{instanceType}
                          and config_name = #{configName}
             </foreach>
        </script>
        """)
    Integer updateValueByConfigList(List<ConfigEntity> configEntityList);

    @Insert("""
        <script>
           insert into config (organization_id,cluster_id, instance_type, instance_id,
                               config_type, config_name, config_value, config_value_type,config_value_range,
                               start_version,end_version, description, edit, is_default)
           values
           <foreach collection='list' item='c' index='index' separator=','>
               (#{c.organizationId},#{c.clusterId}, #{c.instanceType},
                <if test='c.instanceId !=null'>
                    #{c.instanceId},
                </if>
                <if test='c.instanceId ==null'>
                    0,
                </if>
                #{c.configType},#{c.configName},#{c.configValue},#{c.configValueType},#{c.configValueRange},
                #{c.startVersion},#{c.endVersion},#{c.description},#{c.edit},#{c.isDefault})
           </foreach>
        </script>
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    void batchInsert(List<ConfigEntity> configEntityList);


    @Insert("insert into config () select *,#{targetId} as cluster_id from config where clster where cluster_id = #{sourceId}")
    void copyConfig(@Param("sourceId") Long sourceId, @Param("targetId") Long targetId);

    @Insert("INSERT INTO config (cluster_id, business_type, instance_type, instance_id, config_name, config_value, "
            + "status, is_default,  diff_type, description, edit, is_modify,start_version,"
            + "eventmesh_version,end_version) VALUE "
            + "(#{clusterId},#{businessType},#{instanceType},#{instanceId},#{configName},"
            + "#{configValue},#{status},#{isDefault},#{diffType},#{description},#{edit},#{isModify},"
            + "#{startVersion},#{eventmeshVersion},#{endVersion})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    Integer addConfig(ConfigEntity configEntity);

    @Update("UPDATE config SET status=0 WHERE id=#{id}")
    Integer deleteConfig(ConfigEntity configEntity);

    @Update("UPDATE config SET config_value=#{configValue} ,already_update=#{alreadyUpdate} WHERE instance_type=#{instanceType} AND"
            + " instance_id=#{instanceId} AND config_name=#{configName} AND is_default=0")
    void updateConfig(ConfigEntity configEntity);

    @Select("SELECT * FROM config WHERE instance_type=#{instanceType} AND instance_id=#{instanceId} AND is_default=0")
    List<ConfigEntity> selectByInstanceId(ConfigEntity configEntity);

    @Select("SELECT * FROM config WHERE cluster_id=-1 AND business_type=#{businessType} AND instance_type=#{instanceType} AND is_default=1")
    List<ConfigEntity> selectDefaultConfig(ConfigEntity configEntity);

    @Select("SELECT * FROM config WHERE is_default=1")
    List<ConfigEntity> selectAllDefaultConfig();

    @Select("SELECT * FROM config WHERE cluster_id=#{clusterId} AND instance_type=#{instanceType} "
            + "AND instance_id=#{instanceId} AND config_name=#{configName} AND status=1")
    ConfigEntity selectByUnique(ConfigEntity configEntity);

    @Override
    void syncInsert(List<ConfigEntity> entityList);

    @Override
    void syncUpdate(List<ConfigEntity> entityList);

    @Override
    void syncDelete(List<ConfigEntity> entityList);


    @Override
    @Select("""
            select * from config where update_time >= #{updateTime} and status != 0
        """)
    List<ConfigEntity> syncGet(ConfigEntity topicEntity);
}
