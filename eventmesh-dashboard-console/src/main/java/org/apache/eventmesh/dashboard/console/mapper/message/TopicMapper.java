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


package org.apache.eventmesh.dashboard.console.mapper.message;


import org.apache.eventmesh.dashboard.console.entity.message.TopicEntity;
import org.apache.eventmesh.dashboard.console.mapper.SyncDataHandlerMapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * operate Topic mapper
 **/
@Mapper
public interface TopicMapper extends SyncDataHandlerMapper<TopicEntity> {


    @Select("SELECT count(*) FROM topic WHERE cluster_id=#{clusterId} AND status=1")
    Integer selectTopicNumByCluster(TopicEntity topicEntity);

    @Select({
        "<script>",
        "SELECT * FROM topic",
        "<where>",
        "cluster_id =#{topicEntity.clusterId} And status=1",
        "<if test='topicEntity.topicName!=null'>",
        "and topic_name like CONCAT('%',#{topicEntity.topicName},'%')",
        "</if>",
        "</where>",
        "</script>"})
    List<TopicEntity> queryTopicsToFrontByClusterId(@Param("topicEntity") TopicEntity topicEntity);


    @Select("SELECT * FROM topic WHERE cluster_id=#{clusterId} and status = 1")
    List<TopicEntity> selectTopicByCluster(TopicEntity topicEntity);


    @Select("SELECT * FROM topic WHERE status=1")
    List<TopicEntity> selectAll();

    @Select("SELECT * FROM topic WHERE id=#{id}")
    TopicEntity queryTopicById(TopicEntity topicEntity);


    @Update("UPDATE topic SET topic_type=#{topicType},description=#{description} WHERE id=#{id}")
    void updateTopic(TopicEntity topicEntity);

    @Update("UPDATE topic SET create_progress=#{createProgress} WHERE id=#{id}")
    void updateTopicCreateProgress(TopicEntity topicEntity);

    @Update("""
        <script>
            update  `topic` set status=0 where id in(
               <foreach collection='list'  item='item' index='index' separator=','>
                    #{item.id}
               </foreach>
            )
        </script>
        """)
    Integer deleteTopicByIds(List<TopicEntity> topicEntity);


    @Update("UPDATE `topic` SET status=0 WHERE id=#{id}")
    Integer deleteTopicById(TopicEntity topicEntity);

    @Insert("""
        <script>
        insert into topic (cluster_id, topic_name, retention_ms, topic_type, description, create_progress)
           values
               <foreach collection='list' item='c' index='index' separator=','>
                   (#{c.clusterId},#{c.topicName},#{c.retentionMs},#{c.topicType},#{c.description},#{c.createProgress})
               </foreach>
        </script>
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    void batchInsert(List<TopicEntity> topicEntities);

    @Insert("""
        insert into topic (cluster_id   , topic_name , retention_ms , topic_type   , description, create_progress)
                   values (#{clusterId} ,#{topicName},#{retentionMs},#{topicType},#{description},#{createProgress})
        on duplicate key update status = 1
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    void insertTopic(TopicEntity topicEntity);


    @Override
    void syncInsert(List<TopicEntity> entityList);

    @Override
    void syncUpdate(List<TopicEntity> entityList);

    @Override
    void syncDelete(List<TopicEntity> entityList);

    @Override
    List<TopicEntity> syncGet(TopicEntity topicEntity);

}