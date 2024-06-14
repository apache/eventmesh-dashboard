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

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 *
 */
@Mapper
public interface ClusterRelationshipMapper {

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

    @Select(" select * from cluster_relationship where status = 3")
    List<ClusterRelationshipEntity> selectAll();

    @Select(" select * from cluster_relationship where update_time = #{updateTime} and status in( 2 ,3)")
    List<ClusterRelationshipEntity> selectNewlyIncreased();

    @Update("update cluster_relationship set status = 3 where id = #{id} ")
    Integer relieveRelationship(ClusterRelationshipEntity clusterRelationshipEntity);

    @Insert({
        " insert into cluster_relationship (cluster_type,cluster_id,relationship_type,relationship_id)values( #{clusterType},#{clusterId},",
        "#{relationshipType},#{relationshipId})"
    })
    void insertClusterRelationshipEntry(ClusterRelationshipEntity clusterRelationshipEntity);


}

