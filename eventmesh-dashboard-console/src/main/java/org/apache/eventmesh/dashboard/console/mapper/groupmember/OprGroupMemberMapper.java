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

package org.apache.eventmesh.dashboard.console.mapper.groupmember;


import org.apache.eventmesh.dashboard.console.entity.GroupMemberEntity;

import org.apache.ibatis.annotations.Delete;
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
public interface OprGroupMemberMapper {

    @Select("select * from group_member where cluster_id=#{clusterId} and is_delete=0")
    List<GroupMemberEntity> getGroupByClusterId(GroupMemberEntity groupMemberEntity);

    @Insert("insert into group_member (cluster_id, topic_name, group_name, eventmesh_user)"
        + " VALUE (#{clusterId},#{topicName},#{groupName},#{eventMeshUser})"
        + "on duplicate key update is_delete=0")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    GroupMemberEntity addGroupMember(GroupMemberEntity groupMemberEntity);

    @Update("update group_member set state=#{state} where id=#{id}")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    GroupMemberEntity updateGroupMember(GroupMemberEntity groupMemberEntity);

    @Delete("update group_member set is_delete=1 where id=#{id} ")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    GroupMemberEntity deleteGroupMember(GroupMemberEntity groupMemberEntity);

    @Select("select * from group_member where cluster_id=#{clusterId} and group_name=#{groupName} and topic_name=#{topicName}")
    GroupMemberEntity selectGroupMemberByUnique(GroupMemberEntity groupMemberEntity);

    @Select("select * from group_member where id=#{id}")
    GroupMemberEntity selectGroupMemberById(GroupMemberEntity groupMemberEntity);

    @Select("<script>"
        + "select * from group_member"
        + "<where>"
        + "<if test='clusterId != null'>"
        + "cluster_id=#{clusterId}"
        + "</if>"
        + "<if test='groupName != null'>"
        + "and group_name=#{groupName}"
        + "</if>"
        + "<if test='topicName != null'>"
        + "and topic_name=#{topicName}"
        + "</if>"
        + "</where>"
        + "</script>")
    List<GroupMemberEntity> selectAllMemberByDynamic(GroupMemberEntity groupMemberEntity);

    @Update("update group_member set state=#{state} where topic_name=#{topicName}")
    GroupMemberEntity updateMemberByTopic(GroupMemberEntity groupMemberEntity);
}