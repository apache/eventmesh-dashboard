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


package org.apache.eventmesh.dashboard.console.mapper.deploy;

import org.apache.eventmesh.dashboard.console.entity.cases.PortEntity;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 
 */
@Mapper
public interface PortMapper {


    @Insert("insert into port(cluster_id, current_port)values (#{clusterId},#{currentPort})")
    void insertPort(PortEntity portEntity);


    @Select(" select * from port where cluster_id = #{clusterId} for update")
    PortEntity lockPort(PortEntity portEntity);

    @Update(" update port set current_port = current_port + #{currentPort} where cluster_id=#{clusterId}")
    void updatePort(PortEntity portEntity);
}
