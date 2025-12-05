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


import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
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


    @Select("""
            <script>
                select * from topic where cluster_id
                    <foreach item='item' index='index' open='in(' separator=',' close=')'>
                            #{item.id}
                    </foreach>
            </script>
        """)
    List<TopicEntity> queryByClusterIdList(List<ClusterEntity> topicEntityList);


    @Select("SELECT count(*) FROM topic WHERE cluster_id=#{clusterId} AND status=1")
    Integer selectTopicNumByCluster(TopicEntity topicEntity);

    @Select("""
        <script>
            select * from topic where cluster_id =#{topicEntity.clusterId} and status=1
                <if test='topicEntity.topicName!=null'>
                    and topic_name like concat('%',#{topicEntity.topicName},'%')
                </if>
                and status=1
        </script>
        """)
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
        insert into topic (cluster_id,
                          runtime_id,
                          topic_name,topic_type, read_queue_num, write_queue_num, replication_factor, 
                           `order` , description, create_progress,retention_ms)
           values
               <foreach collection='list' item='c' index='index' separator=','>
                        (#{c.clusterId},
                        <if test='c.runtimeId !=null'>
                            #{c.runtimeId},
                        </if>
                        <if test='c.runtimeId ==null'>
                            0,
                        </if>
                        #{c.topicName},#{c.topicType},#{c.readQueueNum},#{c.writeQueueNum},#{c.replicationFactor}
                            ,#{c.order},#{c.description},#{c.createProgress},#{c.retentionMs})
               </foreach>
        </script>
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    void batchInsert(List<TopicEntity> topicEntities);

    @Insert("""
        insert into topic (cluster_id,runtime_id,topic_name,topic_type, read_queue_num, write_queue_num, replication_factor, 
                           `order` , description, create_progress,retention_ms)
                   values (#{clusterId},#{runtimeId},#{topicName},#{topicType},#{readQueueNum},#{writeQueueNum},#{replicationFactor}
                            ,#{order},#{description},#{createProgress},#{retentionMs})
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
    @Select("""
            select * from topic where update_time >= #{updateTime} and status != 0
        """)
    List<TopicEntity> syncGet(TopicEntity topicEntity);

}