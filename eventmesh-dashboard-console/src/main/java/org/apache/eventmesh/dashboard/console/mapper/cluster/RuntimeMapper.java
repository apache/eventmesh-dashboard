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
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.mapper.SyncDataHandlerMapper;
import org.apache.eventmesh.dashboard.console.modle.DO.runtime.QueryRuntimeByBigExpandClusterDO;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * runtime table operation
 */
@Mapper
public interface RuntimeMapper extends SyncDataHandlerMapper<RuntimeEntity> {

    @Select("""
        <script>
            select * from runtime where cluster_id =#{runtimeEntity.clusterId}
                <if test='runtimeEntity.host!=null'>
                    and host like CONCAT('%',#{runtimeEntity.host},'%')
                </if>
        </script>
        """)
    List<RuntimeEntity> getRuntimesToFrontByCluster(@Param("runtimeEntity") RuntimeEntity runtimeEntity);

    @Select("""
        <script>
            select * from runtime where cluster_id in(
                select relationship_id from cluster_relationship where cluster_id in (
                    select cluster_id from cluster_relationship where relationship_id = #{followClusterId}
                  )
                 <if test='queryClusterTypeList!=null'>
                 and relationship_type in
                    <foreach collection='queryClusterTypeList' item='item' index='index' open="(" separator=',' close=")">
                      #{item}
                    </foreach>
                 </if>
            )
        </script>
        """)
    /**
     * 查询 子集群的主集群中的某个类型的集群。
     */
    List<RuntimeEntity> queryClusterRuntimeOnClusterSpecifyByClusterId(QueryRuntimeByBigExpandClusterDO queryRuntimeByBigExpandClusterDO);

    /**
     * TODO 如何设计动态查询
     * 通过 本集群 查询 主集群中 特定依赖集群中 的 特定依赖集群的 runtime
     * <p> 比如 eventmesh runtime 得到所有存储的 meta集群信息 </p>
     * <ol>
     *  <li> 通过 eventmesh runtime id 以及 eventmesh cluster type 得到 eventmesh cluster id  </li>
     *  <li> 通过 eventmesh cluster id 以及 storage cluster type（列表） or storage type  得到 storage cluster id  </li>
     *  <li> 通过 storage cluster id 以及 storage meta type 列表） or meta type  得到 meta cluster id  </li>
     *  <li> 通过 meta cluster id  得到 runtime </li>
     * </ol>
     */
    @Select("""
            <script>
                select * from runtime where cluster_id in(
                    select relationship_id from cluster_relationship where cluster_id in(
                        select relationship_id from cluster_relationship where cluster_id in(
                          select cluster_id from cluster_relationship where relationship_id = #{followClusterId} and cluster_type= #{mainClusterType}
                        )
                        and relationship_type in
                         <foreach collection='storageClusterTypeList' item='item' index='index' open="(" separator=',' close=")">
                           #{item}
                         </foreach>
                    )
                    <if test='storageClusterTypeList!=null'>
                        and relationship_type in
                            <foreach collection='storageMetaClusterTypeList' item='item' index='index' open="(" separator=',' close=")">
                               #{item}
                            </foreach>
                    </if>
                )
            </script>
        """)
    List<RuntimeEntity> queryRuntimeByBigExpandCluster(QueryRuntimeByBigExpandClusterDO queryRuntimeByBigExpandClusterDO);


    @Select("""
        <script>
            select * from runtime where cluster_id in
            <foreach collection='list' item='item' index='index' open="(" separator=',' close=")">
               #{item.id}
            </foreach>
        </script>
        """)
    List<RuntimeEntity> queryRuntimeByClusterId(List<ClusterEntity> clusterEntityList);


    @Select("select * from runtime where cluster_id=#{clusterid} and status=1")
    List<RuntimeEntity> selectRuntimeByCluster(RuntimeEntity runtimeEntity);


    @Select("select COUNT(*) from runtime where cluster_id=#{clusterId} AND status=1")
    Integer getRuntimeNumByCluster(RuntimeEntity runtimeEntity);

    @Select("select * from runtime where update_time >= #{updateTime} and status=1")
    List<RuntimeEntity> queryByUpdateTime(RuntimeEntity runtimeEntity);


    @Select("select * from runtime where id=#{id} and status=1")
    RuntimeEntity queryRuntimeEntityById(RuntimeEntity runtimeEntity);

    @Select("select * from runtime where status=1")
    List<RuntimeEntity> queryAll();

    @Update("UPDATE runtime SET port=#{port} ,jmx_port=#{jmxPort} ,status=#{status} where cluster_id=#{clusterId} AND status=1")
    void updateRuntimeByCluster(RuntimeEntity runtimeEntity);

    @Update("UPDATE runtime SET status=0 where cluster_id=#{clusterId}")
    void deleteRuntimeByCluster(RuntimeEntity runtimeEntity);

    @Update("UPDATE runtime SET status = 0 where id = #{id}")
    void deactivate(RuntimeEntity runtimeEntity);


    @Insert("""
        <script>
        insert into runtime( organization_id  , cluster_id , name , cluster_type  ,version    ,host    ,port  ,trusteeship_type,first_to_whom,
                            replication_type,kubernetes_cluster_id, deploy_status_type,  resources_config_id,deploy_script_id,
                            create_script_content ,auth_type,jmx_port)
          values
              <foreach collection='list' item='item' index='index'  separator=','>
                (
                    #{item.organizationId}, #{item.clusterId}, #{item.name}, #{item.clusterType},#{item.version},#{item.host},
                    #{item.port},#{item.trusteeshipType},#{item.firstToWhom},#{item.replicationType},#{item.kubernetesClusterId},
                    #{item.deployStatusType}, #{item.resourcesConfigId},#{item.deployScriptId},'',#{item.authType},#{item.jmxPort}
                )
              </foreach>
        
        </script>
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    void batchInsert(List<RuntimeEntity> runtimeEntities);

    @Insert("""
        insert into runtime( organization_id  , cluster_id , name , cluster_type  ,version    ,host    ,port  ,trusteeship_type,first_to_whom,
                            replication_type, deploy_status_type,  resources_config_id,deploy_script_id,create_script_content ,auth_type,jmx_port)
                      values(#{organizationId}, #{clusterId}, #{name}, #{clusterType},#{version},#{host},#{port},#{trusteeshipType},#{firstToWhom},
                             #{replicationType}, #{deployStatusType},#{resourcesConfigId},#{deployScriptId},'',#{authType},#{jmxPort})
        ON DUPLICATE KEY UPDATE status=1,online_timestamp = now()
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    void insertRuntime(RuntimeEntity runtimeEntity);

    void syncInsert(List<RuntimeEntity> runtimeEntities);

    void syncUpdate(List<RuntimeEntity> runtimeEntities);

    void syncDelete(List<RuntimeEntity> runtimeEntities);


    List<RuntimeEntity> syncGet(RuntimeEntity topicEntity);
}
