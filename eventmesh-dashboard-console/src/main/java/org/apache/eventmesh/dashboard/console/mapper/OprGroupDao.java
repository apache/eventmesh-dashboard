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

import org.apache.eventmesh.dashboard.console.entity.GroupEntity;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
operate Group mapper
 **/
@Mapper
public interface OprGroupDao {
    @Select("select * from `group` where cluster_id=#{clusterId}")
    List<GroupEntity> getGroupList(GroupEntity groupEntity);

    @Insert("INSERT INTO `group` (cluster_id, name, member_count, members, type, state, create_time, update_time)"
        + "VALUE (#{clusterId},#{name},#{memberCount},#{Members},#{type},#{state},#{createTime},#{updateTime})")
    Integer addGroup(GroupEntity groupEntity);

    @Update("update `group`set cluster_id=#{clusterId},name=#{name},member_count=#{memberCount},"
        + "members=#{Members},type=#{type},state=#{state},create_time=#{createTime},update_time=#{updateTime} where id=#{id}")
    Integer updateGroup(GroupEntity groupEntity);

    @Delete("delete from `group` where id=#{id}")
    Integer deleteGroup(Long id);

    @Select("select * from `group` where cluster_id=#{clusterId} and name=#{name}")
    GroupEntity selectGroupByUnique(GroupEntity groupEntity);

    @Select("select * from `group` where id=#{id}")
    GroupEntity selectGroupById(GroupEntity groupEntity);

}
