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

import org.apache.eventmesh.dashboard.console.entity.LogEntity;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectKey;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 operate operationLog mapper
 **/
@Mapper
public interface OprLogDao {

    @Select("select * from operation_log where cluster_id")
    List<LogEntity> getLogList();

    @Insert("insert into operation_log ( cluster_id, operation_type, status, description, create_time) "
        + "VALUE (#{clusterId},#{operationType},#{status},#{description},#{createTime})")
    @SelectKey(keyColumn = "id", statement =  {" select last_insert_id()"}, keyProperty = "id", before = false, resultType = Long.class)
    Long addLog(LogEntity logEntity);

    @Update("update operation_log set status=#{status},end_time=#{endTime} where id=#{id}")
    Integer updateLog(LogEntity logEntity);
}
