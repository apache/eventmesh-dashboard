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

import org.apache.eventmesh.dashboard.console.entity.cases.DeployScriptEntity;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 *
 */
@Mapper
public interface DeployScriptMapper {


    @Insert("""
        insert into deploy_script () values()
        """)
    void insert(DeployScriptEntity deployScriptEntity);


    @Update("""
        update deploy_script set description=#{description} content=#{content} where id=#{id}")"
        """)
    void update(DeployScriptEntity deployScriptEntity);


    @Update("""
        update deploy_script set status = 1 where id=#{id}")"
        """)
    void deleteById(DeployScriptEntity deployScriptEntity);


    @Select("""
        <script>
            select * from deploy_script where organization_id=#{id}
            <if test = "name!=null">
               and name like concat('%',#{name},'%')
            </if>
        </script>
        """)
    void queryByName(DeployScriptEntity deployScriptEntity);
}
