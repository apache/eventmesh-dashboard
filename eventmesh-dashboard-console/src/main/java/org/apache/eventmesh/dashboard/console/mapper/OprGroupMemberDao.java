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

package org.apache.eventmesh.dashboard.console.mapper;


import org.apache.eventmesh.dashboard.console.entity.GroupMemberEntity;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 operate GroupMember mapper
 **/

@Mapper
public interface OprGroupMemberDao {
    @Select("select * from group_member where cluster_id=#{clusterId}")
    List<GroupMemberEntity> getGroupList(GroupMemberEntity groupMemberEntity);

    @Insert("insert into group_member (cluster_id, topic_name, group_name, eventmesh_user, state, create_time, update_time)"
        + " VALUE (#{clusterId},#{topicName},#{groupName},#{eventMeshUser},#{state},#{createTime},#{updateTime})")
    Integer addGroupMember(GroupMemberEntity groupMemberEntity);

    @Update("update group_member set cluster_id=#{clusterId},topic_name=#{topicName},group_name=#{groupName}"
        + ",eventmesh_user=#{eventMeshUser},state=#{state},create_time=#{createTime},update_time=#{updateTime} where id=#{id}")
    Integer updateGroupMember(GroupMemberEntity groupMemberEntity);

    @Delete("delete from group_member where id=#{id}")
    Integer deleteGroupMember(Long id);

    @Select("select * from group_member where cluster_id=#{clusterId} and group_name=#{groupName} and topic_name=#{topicName}")
    GroupMemberEntity selectGroupMemberByUnique(GroupMemberEntity groupMemberEntity);

    @Select("select * from group_member where id=#{id}")
    GroupMemberEntity selectGroupMemberById(GroupMemberEntity groupMemberEntity);

    @Select("select * from group_member where cluster_id=#{clusterId} and group_name=#{groupName}")
    List<GroupMemberEntity> selectAllMemberByUnique(GroupMemberEntity groupMemberEntity);

    @Select("select * from group_member where topic_name=#{topicName}")
    List<GroupMemberEntity> selectAllMemberByTopic(GroupMemberEntity groupMemberEntity);

    @Update("update group_member set state=#{state} where topic_name=#{topicName}")
    Integer updateMemberByTopic(GroupMemberEntity groupMemberEntity);
}