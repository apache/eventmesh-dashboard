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


package org.apache.eventmesh.dashboard.console.mapper.message;

import org.apache.eventmesh.dashboard.console.entity.message.GroupEntity;
import org.apache.eventmesh.dashboard.console.entity.message.TopicEntity;
import org.apache.eventmesh.dashboard.console.mapper.SyncDataHandlerMapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * operate Group mapper
 **/
@Mapper
public interface GroupMapper extends SyncDataHandlerMapper<GroupEntity> {


    @Select("""
        select *  from `group` where name in(
            select group_name from group_member where topic_name  = (
                select topic.topic_name from topic where id=#{id}
            )
        )
    """)
    List<GroupEntity> queryGroupListByTopicId(TopicEntity topicEntity);

    @Select("SELECT * FROM `group` WHERE cluster_id=#{clusterId} AND name=#{name} AND type=0 ")
    GroupEntity selectGroupByNameAndClusterId(GroupEntity groupEntity);


    @Select("SELECT COUNT(*) FROM `group` WHERE cluster_id=#{clusterId} AND type=0")
    Integer getConsumerNumByCluster(GroupEntity groupEntity);


    @Select("SELECT * FROM `group` WHERE status=1")
    List<GroupEntity> selectAll();

    @Update("UPDATE `group` SET member_count=#{memberCount},"
            + "members=#{members},type=#{type},state=#{state} WHERE id=#{id}")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    Integer updateGroup(GroupEntity groupEntity);

    @Update("UPDATE `group` SET  status=1 WHERE id=#{id}")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    Integer deleteGroup(GroupEntity groupEntity);

    @Select("SELECT * FROM `group` WHERE cluster_id=#{clusterId} AND name=#{name} AND status=1")
    GroupEntity selectGroupByUnique(GroupEntity groupEntity);

    @Select("SELECT * FROM `group` WHERE id=#{id} AND status=1")
    GroupEntity selectGroupById(GroupEntity groupEntity);

    @Select({
        "<script>",
        "   SELECT * FROM `group`",
        "   <where>",
        "       <if test='clusterId != null'>",
        "           cluster_id=#{clusterId}",
        "       </if>",
        "       <if test='name != null'>",
        "           name LIKE concat('%',#{name},'%')",
        "       </if>",
        "       AND status=1",
        "   </where>",
        "</script>"})
    List<GroupEntity> selectGroup(GroupEntity groupEntity);


    @Insert("""
        <script>
           insert into `group` (organization_id,cluster_id, name, type,own_type)
            values
               <foreach collection='list' item='c' index='index' separator=','>
                (#{c.organizationId},#{c.clusterId},#{c.name},#{c.type},#{c.ownType})
               </foreach>
        </script>
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    void batchInsert(List<GroupEntity> groupEntities);

    @Insert("""
        <script>
           insert into `group` (organization_id,cluster_id, name, type,own_type)
            values (#{c.organizationId},#{clusterId},#{name},#{type},#{ownType}) on duplicate key update  status=1
        </script>
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    void addGroup(GroupEntity groupEntity);


    @Override
    void syncInsert(List<GroupEntity> entityList);

    @Override
    void syncUpdate(List<GroupEntity> entityList);

    @Override
    void syncDelete(List<GroupEntity> entityList);

    @Override
    List<GroupEntity> syncGet(GroupEntity topicEntity);
}
