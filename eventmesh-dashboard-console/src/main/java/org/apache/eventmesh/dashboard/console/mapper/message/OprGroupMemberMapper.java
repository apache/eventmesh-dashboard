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


import org.apache.eventmesh.dashboard.console.entity.message.SubscriptionEntity;

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


    @Deprecated
    @Select("select topic_name from group_member where cluster_id=#{clusterId} and group_name=#{groupName}")
    List<String> selectTopicsByGroupNameAndClusterId(SubscriptionEntity subscriptionEntity);

    @Deprecated
    @Select("select DISTINCT (group_name) from group_member where cluster_id=#{clusterId} and topic_name=#{topicName}")
    List<String> selectGroupNameByTopicName(SubscriptionEntity subscriptionEntity);

    @Select("select * from group_member where status=1")
    List<SubscriptionEntity> selectAll();


    @Deprecated
    @Select("select * from group_member where cluster_id=#{clusterId} and status=1")
    List<SubscriptionEntity> getGroupByClusterId(SubscriptionEntity subscriptionEntity);


    @Deprecated
    @Select("select * from group_member where cluster_id=#{clusterId} and group_name=#{groupName} and topic_name=#{topicName} and status=1")
    SubscriptionEntity selectGroupMemberByUnique(SubscriptionEntity subscriptionEntity);

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
    List<SubscriptionEntity> selectMember(SubscriptionEntity subscriptionEntity);


    @Select("select * from group_member where id=#{id} and status=1")
    SubscriptionEntity selectGroupMemberById(SubscriptionEntity subscriptionEntity);

    @Update("UPDATE group_member SET state=#{state} where id=#{id}")
    void updateGroupMember(SubscriptionEntity subscriptionEntity);

    @Deprecated
    @Update("UPDATE group_member SET state=#{state} where topic_name=#{topicName}")
    void updateMemberByTopic(SubscriptionEntity subscriptionEntity);


    @Update("UPDATE group_member SET status=0 where id=#{id} ")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    SubscriptionEntity deleteGroupMember(SubscriptionEntity subscriptionEntity);

    @Insert({
        "<script>",
        "   insert into group_member (cluster_id, topic_name, group_name, eventmesh_user, state) values ",
        "   <foreach collection='list' item='c' index='index' separator=','>",
        "(#{c.clusterId},#{c.topicName},#{c.groupName},#{c.eventMeshUser},#{c.state})",
        "   </foreach>",
        "</script>"})
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    void batchInsert(List<SubscriptionEntity> groupMemberEntities);


    @Insert("insert into group_member (cluster_id, topic_name, group_name, eventmesh_user,state)"
            + " values (#{clusterId},#{topicName},#{groupName},#{eventMeshUser},#{state})"
            + "ON DUPLICATE KEY UPDATE status=0")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    void addGroupMember(SubscriptionEntity subscriptionEntity);


}