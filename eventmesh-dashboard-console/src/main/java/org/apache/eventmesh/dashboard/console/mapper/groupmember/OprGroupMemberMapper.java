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


import org.apache.eventmesh.dashboard.console.entity.groupmember.GroupMemberEntity;

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

    @Select("SELECT * FROM group_member WHERE cluster_id=#{clusterId} AND is_delete=0")
    List<GroupMemberEntity> getGroupByClusterId(GroupMemberEntity groupMemberEntity);

    @Insert("INSERT INTO group_member (cluster_id, topic_name, group_name, eventmesh_user,state)"
        + " VALUE (#{clusterId},#{topicName},#{groupName},#{eventMeshUser},#{state})"
        + "ON DUPLICATE KEY UPDATE is_delete=0")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    void addGroupMember(GroupMemberEntity groupMemberEntity);

    @Update("UPDATE group_member SET state=#{state} WHERE id=#{id}")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    void updateGroupMember(GroupMemberEntity groupMemberEntity);

    @Delete("UPDATE group_member SET is_delete=1 WHERE id=#{id} ")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    GroupMemberEntity deleteGroupMember(GroupMemberEntity groupMemberEntity);

    @Select("SELECT * FROM group_member WHERE cluster_id=#{clusterId} AND group_name=#{groupName} AND topic_name=#{topicName} AND is_delete=0")
    GroupMemberEntity selectGroupMemberByUnique(GroupMemberEntity groupMemberEntity);

    @Select("SELECT * FROM group_member WHERE id=#{id} AND is_delete=0")
    GroupMemberEntity selectGroupMemberById(GroupMemberEntity groupMemberEntity);

    @Select({
        "<script>",
        "   SELECT * FROM group_member",
        "   <where>",
        "       <if test='clusterId != null'>",
        "           cluster_id=#{clusterId}",
        "       </if>",
        "       <if test='groupName != null'>",
        "           AND group_name=#{groupName}",
        "       </if>",
        "       <if test='topicName != null'>",
        "           AND topic_name=#{topicName}",
        "       </if>",
        "    </where>",
        "   AND is_delete=0",
        "</script>"})
    List<GroupMemberEntity> selectMember(GroupMemberEntity groupMemberEntity);

    @Update("UPDATE group_member SET state=#{state} WHERE topic_name=#{topicName}")
    void updateMemberByTopic(GroupMemberEntity groupMemberEntity);
}