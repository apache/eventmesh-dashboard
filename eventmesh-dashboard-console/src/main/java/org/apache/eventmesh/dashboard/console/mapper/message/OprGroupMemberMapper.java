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

    @Select("SELECT topic_name FROM group_member WHERE cluster_id=#{clusterId} AND group_name=#{groupName}")
    List<String> selectTopicsByGroupNameAndClusterId(GroupMemberEntity groupMemberEntity);

    @Select("SELECT DISTINCT (group_name) FROM group_member WHERE cluster_id=#{clusterId} AND topic_name=#{topicName}")
    List<String> selectGroupNameByTopicName(GroupMemberEntity groupMemberEntity);

    @Select("SELECT * FROM group_member WHERE status=1")
    List<GroupMemberEntity> selectAll();

    @Select("SELECT * FROM group_member WHERE cluster_id=#{clusterId} AND group_name=#{groupName} AND topic_name=#{topicName} AND status=1")
    GroupMemberEntity selectGroupMemberByUnique(GroupMemberEntity groupMemberEntity);

    @Select("SELECT * FROM group_member WHERE id=#{id} AND status=1")
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
        "   AND status=1",
        "</script>"})
    List<GroupMemberEntity> selectMember(GroupMemberEntity groupMemberEntity);

    @Select("SELECT * FROM group_member WHERE cluster_id=#{clusterId} AND status=1")
    List<GroupMemberEntity> selectGroupByClusterId(GroupMemberEntity groupMemberEntity);

    @Update("UPDATE group_member SET state=#{state} WHERE id=#{id}")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    Integer updateGroupMember(GroupMemberEntity groupMemberEntity);

    @Update("UPDATE group_member SET status=0 WHERE id=#{id} ")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    Integer deleteGroupMember(GroupMemberEntity groupMemberEntity);

    @Update("UPDATE group_member SET state=#{state} WHERE topic_name=#{topicName}")
    Integer updateMemberByTopic(GroupMemberEntity groupMemberEntity);

    @Insert({
        "<script>",
        "   INSERT INTO group_member (cluster_id, topic_name, group_name, eventmesh_user, state) VALUES ",
        "   <foreach collection='list' item='c' index='index' separator=','>",
        "(#{c.clusterId},#{c.topicName},#{c.groupName},#{c.eventMeshUser},#{c.state})",
        "   </foreach>",
        "</script>"})
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    Integer batchInsert(List<GroupMemberEntity> groupMemberEntities);

    @Insert("INSERT INTO group_member (cluster_id, topic_name, group_name, eventmesh_user,state)"
        + " VALUE (#{clusterId},#{topicName},#{groupName},#{eventMeshUser},#{state})"
        + "ON DUPLICATE KEY UPDATE status=0")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    void insertGroupMember(GroupMemberEntity groupMemberEntity);


}
