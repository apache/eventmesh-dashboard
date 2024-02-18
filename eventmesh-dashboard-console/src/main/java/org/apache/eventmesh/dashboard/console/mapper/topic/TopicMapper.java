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

package org.apache.eventmesh.dashboard.console.mapper.topic;


import org.apache.eventmesh.dashboard.console.entity.topic.TopicEntity;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * operate Topic mapper
 **/
@Mapper
public interface TopicMapper {

    @Select("<script>"
        + "select * from topic"
        + "<where>"
        + "<if test='topicName!=null'>"
        + "and topic_name=#{topicName}"
        + "</if>"
        + "<if test='clusterId!=null'>"
        + "and cluster_id=#{clusterId} "
        + "</if>"
        + "and is_delete=0"
        + "</where>"
        + "</script>")
    List<TopicEntity> getTopicList(TopicEntity topicEntity);

    @Insert("INSERT INTO topic (cluster_id, topic_name, runtime_id, storage_id, retention_ms, type, description) "
        + "VALUE (#{clusterId},#{topicName},#{runtimeId},#{storageId},#{retentionMs},#{type},#{description})"
        + "on duplicate key update is_delete = 0")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    void addTopic(TopicEntity topicEntity);

    @Update("update topic set type=#{type},description=#{description} where id=#{id}")
    void updateTopic(TopicEntity topicEntity);

    @Delete("update `topic` set is_delete=1 where id=#{id}")
    void deleteTopic(TopicEntity topicEntity);

    @Select("select * from topic where cluster_id=#{clusterId} and topic_name=#{topicName}")
    TopicEntity selectTopicByUnique(TopicEntity topicEntity);

    @Select("select * from topic where id=#{id}")
    TopicEntity selectTopicById(TopicEntity topicEntity);

}