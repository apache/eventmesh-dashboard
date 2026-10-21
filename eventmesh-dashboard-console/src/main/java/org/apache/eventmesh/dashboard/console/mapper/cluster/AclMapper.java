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

import org.apache.eventmesh.dashboard.console.entity.cluster.AclEntity;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * Mybatis Mapper for the table of acl.
 */
@Mapper
public interface AclMapper {

    /** Resource 21 belongs to the independent RocketMQ RPC path. */
    @Select("SELECT * FROM acl WHERE resource_type != 21 OR resource_type IS NULL")
    List<AclEntity> selectKafkaSnapshot();

    @Insert({"""
            <script>
               INSERT INTO acl (cluster_Id, pattern, operation, permission_Type, host, resource_Type, resource_Name, pattern_Type) value
               <foreach collection='list' item='c' index='index' separator=''>
                    (#{c.clusterId}, #{c.pattern}, #{c.operation}, #{c.permissionType}, #{c.host}#{c.resourceType},
                     #{c.resourceName}, #{c.patternType})
               </foreach>
            </script>
          """})
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    void batchInsert(List<AclEntity> aclEntities);

    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    @Insert("INSERT INTO acl (cluster_id, pattern, operation, permission_type, host, resource_type, resource_name, pattern_type)"
            + "VALUE (#{clusterId}, #{pattern}, #{operation}, #{permissionType}, #{host}, #{resourceType}, #{resourceName}, #{patternType})")
    void insert(AclEntity aclEntity);

    @Update("UPDATE acl SET is_delete=1, update_time=CURRENT_TIMESTAMP WHERE id=#{id} AND is_delete=0")
    void deleteById(AclEntity aclEntity);

    @Update("UPDATE acl SET resource_type=#{resourceType} WHERE id=#{id}")
    void updateResourceTypeById(AclEntity aclEntity);

    @Select("SELECT * FROM acl WHERE is_delete=0")
    List<AclEntity> selectAll();

    @Select("SELECT * FROM acl WHERE id=#{id} AND is_delete=0")
    AclEntity selectById(AclEntity aclEntity);
}
