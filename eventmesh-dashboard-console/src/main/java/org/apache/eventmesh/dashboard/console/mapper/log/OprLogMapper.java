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

package org.apache.eventmesh.dashboard.console.mapper.log;

import org.apache.eventmesh.dashboard.console.entity.log.LogEntity;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectKey;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * operate operationLog mapper
 **/
@Mapper
public interface OprLogMapper {

    @Select("<script>"
        + "select * from operation_log"
        + "<where>"
        + "<if test='targetType!=null'>"
        + "target_type=#{targetType}"
        + "</if>"
        + "<if test='operationUser!=null'>"
        + "and operation_user=#{operationUser}"
        + "</if>"
        + "<if test='clusterId!=null'>"
        + "and cluster_id=#{clusterId} "
        + "</if>"
        + "and is_delete=0"
        + "</where>"
        + "</script>")
    List<LogEntity> getLogList(LogEntity logEntity);

    @Insert("insert into operation_log ( cluster_id, operation_type,target_Type, description,operation_user,result_content)"
        + "VALUE (#{clusterId},#{operationType},#{targetType},#{description},#{operationUser},#{resultContent})")
    @SelectKey(keyColumn = "id", statement = {" select last_insert_id()"}, keyProperty = "id", before = false, resultType = Long.class)
    Long addLog(LogEntity logEntity);

    @Update("update operation_log set status=#{status} ,result_content=#{resultContent} where id=#{id}")
    Integer updateLog(LogEntity logEntity);
}
