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

package org.apache.eventmesh.dashboard.console.mapper.storage;

import org.apache.eventmesh.dashboard.console.entity.storage.StoreEntity;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * store table operation
 */
@Mapper
public interface StoreMapper {

    @Insert("insert into store (cluster_id, store_id, store_type, host, runtime_id, topic_list, diff_type"
        + ", port, jmx_port, start_timestamp, rack, status, endpoint_map ) VALUES ("
        + "#{clusterId},#{storeId},#{storeType},#{host},#{runtimeId},#{topicList},#{diffType},#{port},#{jmxPort}"
        + ",#{startTimestamp},#{rack},#{status},#{endpointMap})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    void addStorage(StoreEntity storeEntity);

    @Update("update store set is_delete=1 where cluster_id=#{clusterId} and store_id=#{storeId}")
    void deleteStoreByUnique(StoreEntity storeEntity);

    @Select("select * from store where cluster_id=#{clusterId} and is_delete=0")
    List<StoreEntity> selectStoreByCluster(StoreEntity storeEntity);

    @Update("update store set status=#{status} where cluster_id=#{clusterId} and store_id=#{storeId}")
    void updateStoreByUnique(StoreEntity storeEntity);
}
