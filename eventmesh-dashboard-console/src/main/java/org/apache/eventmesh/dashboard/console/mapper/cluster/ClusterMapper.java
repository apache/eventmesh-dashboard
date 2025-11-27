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

import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.model.ClusterIdDTO;
import org.apache.eventmesh.dashboard.console.model.QO.cluster.QueryRelationClusterByClusterIdListAndType;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/**
 * cluster table operation
 */
@Mapper
public interface ClusterMapper {

    @Select("select * from cluster where id=#{id} and status=1")
    ClusterEntity queryByClusterId(ClusterEntity cluster);


    @Select("""
        <script>
            select *  from cluster where id
            <foreach collection='list' item='item'  index='index' open='in(' separator=',' close=')'>
                #{item.id}
            </foreach>
        </script>
        """)
    List<ClusterEntity> queryClusterListByClusterList(List<ClusterEntity> clusterEntityList);

    @Select("""
        select * from cluster where organization_id =#{organizationId}  and cluster_type=#{clusterType}
        """)
    List<ClusterEntity> queryClusterByOrganizationIdAndType(ClusterEntity clusterEntity);


    @Select("""
        <script>
        select * from cluster where id in(
            select relationship_id from cluster_relationship where cluster_id=#{id}
               <if test="clusterType != null and clusterType != ''">
                 and cluster_type = #{clusterType}
               </if>
            )
        </script>
        """)
    List<ClusterEntity> queryRelationClusterByClusterIdAndType(ClusterEntity clusterEntity);

    @Select("""
        <script>
        select * from cluster where id in(
            select relationship_id from cluster_relationship where cluster_id in
                <foreach item='item' index='index' open='(' separator=',' close=')'>
                            #{item.id}
                </foreach>
            )
        </script>
        """)
    List<ClusterEntity> queryRelationClusterByClusterIdListAndType(
        QueryRelationClusterByClusterIdListAndType queryRelationClusterByClusterIdListAndType);


    @Select("""
            <script>
                select * from cluster where id in(
                    select relationship_id from cluster_relationship where cluster_id in(
                        select relationship_id from cluster_relationship where cluster_id=#{id}
                           <if test="clusterType != null and clusterType != ''">
                             and cluster_type = #{clusterType}
                           </if>
                        )
                    )                   }
            </script>
        """)
    List<ClusterEntity> queryStorageClusterByEventMeshId(ClusterEntity clusterEntity);


    @Select("""
            <script>
                select * from cluster where id in(
                    select relationship_id from cluster_relationship where cluster_id in(
                        <foreach item='item' index='index' separator=','>
                            item.id
                        </foreach>
                    )
            </script>
        """)
    List<ClusterEntity> queryRelationshipClusterByClusterIdAndType(List<ClusterEntity> clusterEntityList);

    @Select("select * from cluster where status=1")
    List<ClusterEntity> queryAllCluster();

    @Select("SELECT * FROM cluster where update_time >  #{updateTime} and is_delete != 1")
    List<ClusterEntity> queryClusterByUpdate(ClusterEntity clusterEntity);




    @Select("""
        <script>
        </script>
        """)
    Map<String, Integer> queryHomeClusterData(ClusterIdDTO clusterIdDTO);

    @Insert("""
        <script>
            insert into cluster( organization_id,  name ,cluster_type,version,trusteeship_type,first_to_whom,replication_type,
                deploy_status_type,cluster_own_type,resources_config_id,deploy_script_id,description,
                config,auth_type,jmx_properties)
            values
              <foreach collection='list' item='item' index='index'  separator=','>
                (
                   #{item.organizationId}, #{item.name},#{item.clusterType},#{item.version},#{item.trusteeshipType},
                   #{item.firstToWhom},#{item.replicationType},#{item.deployStatusType},#{item.clusterOwnType},
                   #{item.resourcesConfigId},#{item.deployScriptId},#{item.description},#{item.config},
                   #{item.authType},#{item.jmxProperties}
                )
              </foreach>
        
        </script>
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    Integer batchInsert(List<ClusterEntity> clusterEntities);


    @Insert("""
        insert into cluster( organization_id,  name ,cluster_type,version,trusteeship_type,first_to_whom,replication_type,
                            deploy_status_type,cluster_own_type,resources_config_id,deploy_script_id,description,
                            config,auth_type,jmx_properties)
                      values(#{organizationId}, #{name},#{clusterType},#{version},#{trusteeshipType},#{firstToWhom},#{replicationType},
                             #{deployStatusType},#{clusterOwnType},#{resourcesConfigId},#{deployScriptId},#{description},
                             #{config},#{authType},#{jmxProperties})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    Integer insertCluster(ClusterEntity cluster);

    @Update("""
        <script>
            update cluster
                set name=#{name},jmx_properties=#{jmxProperties},description=#{description},auth_type=#{authType},
                    run_state=#{runState}
                where id=#{id}
        </script>
        """)
    Integer updateClusterById(ClusterEntity cluster);

    @Update("UPDATE cluster SET status=0 WHERE id=#{id}")
    Integer deactivate(ClusterEntity clusterEntity);


    @Select("""
            select * from cluster where id=#{id} for update
        """)
    ClusterEntity lockByClusterId(ClusterEntity clusterEntity);

    @Update("""
            update cluster set runtime_index= runtime_index + #{runtimeIndex} where id=#{id}
        """)
    Integer updateNumByClusterId(ClusterEntity clusterEntity);


}
