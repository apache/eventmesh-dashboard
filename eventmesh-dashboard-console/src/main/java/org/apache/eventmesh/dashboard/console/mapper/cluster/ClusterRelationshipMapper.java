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

import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterAndRelationshipEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterRelationshipEntity;
import org.apache.eventmesh.dashboard.console.modle.DO.clusterRelationship.QueryListByClusterIdAndTypeDO;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 *
 */
@Mapper
public interface ClusterRelationshipMapper {


    @Select("""
        <script>
            select *  from cluster_relationship where cluster_id = #{clusterId}
                <if test = "clusterTypeList != null">
                     and relationship_type in
                     <foreach item='item' index='index' collection='relationshipTypeList'  open="(" separator=',' close=")">
                        #{item}
                    </foreach>
                </if>
           union all
            select *  from cluster_relationship where relationship_id = #{clusterId}
                <if test = "clusterTypeList != null">
                     and cluster_type in
                     <foreach item='item' index='index' collection='clusterTypeList'  open="(" separator=',' close=")">
                        #{item}
                    </foreach>
                </if>
            )
        </script>
        """)
    List<ClusterRelationshipEntity> queryClusterRelationshipEntityListByClusterId(QueryListByClusterIdAndTypeDO data);

    @Select("""
        <script>
            select *  from cluster_relationship where cluster_id = #{clusterId}
                <if test = "clusterTypeList != null">
                     and relationship_type in
                     <foreach item='item' index='index' collection='relationshipTypeList'  open="(" separator=',' close=")">
                        #{item}
                    </foreach>
                </if>
        </script>
        """)
    List<ClusterRelationshipEntity> queryListByClusterIdAndType(QueryListByClusterIdAndTypeDO data);


    @Select({
        "<script>",
        " select * from cluster as c inner join cluster_relationship as cr on c.id = c where cr.cluster_id ",
        "<if text=' clusterId != null'>",
        "c.id = c.relationship_id where cr.cluster_id = #{clusterId}",
        "</if>",
        "<if text=' relationshipId != null'>",
        "c.id = c.cluster_id where cr.relationship_id = #{relationshipId}",
        "</if>",
        "</script>",
    })
    List<ClusterAndRelationshipEntity> queryClusterAndRelationshipEntityListByClusterId(ClusterRelationshipEntity clusterRelationshipEntity);



    @Select(" select * from cluster_relationship where update_time > #{updateTime} and status in(1, 2 ,3)")
    List<ClusterRelationshipEntity> queryNewlyIncreased(ClusterRelationshipEntity clusterRelationshipEntity);


    @Select(" select * from cluster_relationship where status = 1")
    List<ClusterRelationshipEntity> queryAll(ClusterRelationshipEntity clusterRelationshipEntity);

    @Select("""
            select * from cluster_relationship where id = #{id}
        """)
    ClusterRelationshipEntity queryById(ClusterRelationshipEntity clusterRelationshipEntity);


    @Update("update cluster_relationship set status = 3 where id = #{id} ")
    Integer relieveRelationship(ClusterRelationshipEntity clusterRelationshipEntity);

    @Insert(""" 
        <script>
            insert into cluster_relationship (organization_id , cluster_type,cluster_id,relationship_type,relationship_id)
            values
                <foreach collection='list' item='item' index='index' separator=','>
                    (#{item.organizationId}, #{item.clusterType},#{item.clusterId},#{item.relationshipType},#{item.relationshipId})
                </foreach>
        </script> 
        """)
    Integer batchClusterRelationshipEntry(List<ClusterRelationshipEntity> clusterRelationshipEntity);

    @Insert("""
        insert into cluster_relationship (organization_id , cluster_type,cluster_id,relationship_type,relationship_id)
                           values(#{organizationId}, #{clusterType},#{clusterId},#{relationshipType},#{relationshipId})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    Integer insertClusterRelationshipEntry(ClusterRelationshipEntity clusterRelationshipEntity);

}
