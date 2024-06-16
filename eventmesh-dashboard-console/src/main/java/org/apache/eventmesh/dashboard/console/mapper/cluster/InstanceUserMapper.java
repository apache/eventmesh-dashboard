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

import org.apache.eventmesh.dashboard.console.entity.cluster.InstanceUserEntity;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * Mybatis Mapper for the table of instanceuser.
 */
@Mapper
public interface InstanceUserMapper {

    @Select("select * from instance_user where status=1")
    List<InstanceUserEntity> selectAll();

    @Select("SELECT * FROM instance_user WHERE id=#{id} AND status=1")
    InstanceUserEntity selectById(InstanceUserEntity instanceuserEntity);

    @Select("SELECT * FROM instance_user WHERE name=#{name} AND status=1")
    List<InstanceUserEntity> selectByName(InstanceUserEntity instanceuserEntity);

    @Update("UPDATE instance_user SET status=0 WHERE id=#{clusterId}")
    Integer deleteInstanceUserById(InstanceUserEntity instanceuserEntity);

    @Update("UPDATE instance_user SET password=#{password} WHERE id=#{id}")
    Integer updatePasswordById(InstanceUserEntity instanceuserentity);

    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    @Insert("INSERT INTO instance_user (id, instance_type, password, cluster_id, name, token, status) "
        + "VALUES (#{id}, #{instanceType}, #{password}, #{clusterId}, #{name}, #{token},1)")
    void insert(InstanceUserEntity instanceuserEntity);

}
