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

package org.apache.eventmesh.dashboard.console.mapper.function;

import org.apache.eventmesh.dashboard.console.entity.function.ConfigTemplateEntity;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 *
 */
@Mapper
public interface ConfigTemplateMapper {

    @Insert("""
            insert into config_template(cluster_type,version,metadata_type,metadata_type,name)
            values(#{clusterType},#{version},#{metadataType},#{name})
        """)
    void insertConfigTemplate(ConfigTemplateEntity configTemplateEntity);

    @Select("""
            select * from config_template where organization_id = #{organizationId}
                <if test="metadataType != null">
                    and metadata_type=#{metadataType}
                </if>
        """)
    List<ConfigTemplateEntity> queryConfigTemplateByClusterType(ConfigTemplateEntity configTemplateEntity);
}
