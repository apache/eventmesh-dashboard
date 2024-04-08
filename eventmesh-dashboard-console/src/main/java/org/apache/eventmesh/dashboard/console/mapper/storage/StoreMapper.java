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

    @Select("SELECT * FROM store WHERE status=1")
    List<StoreEntity> selectAll();

    @Select("SELECT * FROM store WHERE id=#{id} AND status=1")
    StoreEntity selectById(StoreEntity storeEntity);

    @Insert({
        "<script>",
        "INSERT INTO store (cluster_id, store_id, store_type, host, runtime_id, topic_list, diff_type, port, jmx_port,start_timestamp, rack,",
        " status, endpoint_map) VALUES ",
        "   <foreach collection='list' item='c' index='index' separator=','>",
        "       (#{c.clusterId}, #{c.storeId}, #{c.storeType}, #{c.host}, #{c.runtimeId}, #{c.topicList}, #{c.diffType}, #{c.port}, #{c.jmxPort},",
        "       #{c.startTimestamp}, #{c.rack}, #{c.status}, #{c.endpointMap})",
        "   </foreach>",
        "</script>"})
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    void batchInsert(List<StoreEntity> storeEntities);

    @Insert("INSERT INTO store (cluster_id, store_id, store_type, host, runtime_id, topic_list, diff_type"
        + ", port, jmx_port, start_timestamp, rack, status, endpoint_map ) VALUES ("
        + "#{clusterId},#{storeId},#{storeType},#{host},#{runtimeId},#{topicList},#{diffType},#{port},#{jmxPort}"
        + ",#{startTimestamp},#{rack},#{status},#{endpointMap})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    void addStore(StoreEntity storeEntity);

    @Update("UPDATE store SET status=0 WHERE cluster_id=#{clusterId} AND store_id=#{storeId}")
    void deleteStoreByUnique(StoreEntity storeEntity);

    @Select("SELECT * FROM store WHERE cluster_id=#{clusterId} AND status=1")
    StoreEntity selectStoreByCluster(StoreEntity storeEntity);

    @Update("UPDATE store SET status=#{status} WHERE cluster_id=#{clusterId} AND store_id=#{storeId}")
    void updateStoreByUnique(StoreEntity storeEntity);

    @Update("UPDATE store SET topic_list=#{topicList} WHERE cluster_id=#{clusterId}")
    void updateTopicListByCluster(StoreEntity storeEntity);
}
