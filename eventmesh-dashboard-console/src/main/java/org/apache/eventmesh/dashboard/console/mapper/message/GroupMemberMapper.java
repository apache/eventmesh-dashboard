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


import org.apache.eventmesh.dashboard.console.entity.message.GroupMemberEntity;
import org.apache.eventmesh.dashboard.console.mapper.SyncDataHandlerMapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * operate GroupMember mapper
 **/

@Mapper
public interface GroupMemberMapper extends SyncDataHandlerMapper<GroupMemberEntity> {


    @Select("""
        <script>
           select * from group_member
           where
               <if test='clusterId != null'>
                   cluster_id=#{clusterId}
               </if>
               <if test='groupName != null'>
                   and group_name=#{groupName}
               </if>
               <if test='topicName != null'>
                   and topic_name=#{topicName}
               </if>
                and status=1
        </script>
        """)
    List<GroupMemberEntity> selectMember(GroupMemberEntity groupMemberEntity);


    @Select("select * from group_member where id=#{id} and status=1")
    GroupMemberEntity selectGroupMemberById(GroupMemberEntity groupMemberEntity);

    @Update("UPDATE group_member SET state=#{state} where id=#{id}")
    void updateGroupMember(GroupMemberEntity groupMemberEntity);



    @Update("UPDATE group_member SET status=0 where id=#{id} ")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    GroupMemberEntity deleteGroupMember(GroupMemberEntity groupMemberEntity);

    @Insert("""
        <script>
           insert into group_member (organization_id,cluster_id, topic_name, group_name, eventmesh_user) values
           <foreach collection='list' item='c' index='index' separator=','>
                (#{c.organizationId},#{c.clusterId},#{c.topicName},#{c.groupName},#{c.eventMeshUser})
           </foreach>
        </script>
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    void batchInsert(List<GroupMemberEntity> groupMemberEntities);


    @Insert("""
        insert into
           group_member (organization_id , cluster_id, topic_name, group_name, eventmesh_user)
           values (#{organizationId},#{clusterId},#{topicName},#{groupName},#{eventMeshUser})
           on duplicate  key update status=0
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    void addGroupMember(GroupMemberEntity groupMemberEntity);


}