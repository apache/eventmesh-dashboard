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


package org.apache.eventmesh.dashboard.console.model;

import org.apache.eventmesh.dashboard.common.model.metadata.ConfigMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.TopicMetadata;
import org.apache.eventmesh.dashboard.console.entity.function.ConfigEntity;
import org.apache.eventmesh.dashboard.console.entity.message.TopicEntity;
import org.apache.eventmesh.dashboard.console.mapper.function.ConfigMapper;
import org.apache.eventmesh.dashboard.console.mapper.message.TopicMapper;
import org.apache.eventmesh.dashboard.console.mapstruct.message.ConfigControllerMapper;
import org.apache.eventmesh.dashboard.console.mapstruct.message.TopicControllerMapper;
import org.apache.eventmesh.dashboard.console.model.dto.topic.CreateTopicDTO;
import org.apache.eventmesh.dashboard.console.model.dto.topic.GetTopicListDTO;
import org.apache.eventmesh.dashboard.console.model.function.config.QueryByInstanceIdDTO;
import org.apache.eventmesh.dashboard.console.spring.support.metadata.convert.ConfigConvertMetaData;
import org.apache.eventmesh.dashboard.console.spring.support.metadata.convert.TopicConvertMetaData;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.session.Configuration;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Collections;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

public class NameMappingTest {

    @Test
    public void topicNameSurvivesDtoAndMetadataConversion() {
        CreateTopicDTO request = new CreateTopicDTO();
        request.setTopicName("orders");
        TopicEntity entity = TopicControllerMapper.INSTANCE.createTopic(request);
        entity.setId(100L);
        entity.setClusterId(10L);
        TopicMetadata metadata = TopicConvertMetaData.INSTANCE.toMetaData(entity);
        Assert.assertEquals("orders", metadata.nodeUnique());
        Assert.assertEquals("orders", TopicConvertMetaData.INSTANCE.toEntity(metadata).getName());
        Assert.assertEquals(Long.valueOf(100L), metadata.getId());
        Assert.assertEquals(Long.valueOf(10L), metadata.getClusterId());
        GetTopicListDTO query = new GetTopicListDTO();
        query.setTopicName("orders");
        Assert.assertEquals("orders", TopicControllerMapper.INSTANCE.queryTopicListByClusterId(query).getName());
    }

    @Test
    public void configNameSurvivesDtoAndMetadataConversion() {
        QueryByInstanceIdDTO query = new QueryByInstanceIdDTO();
        query.setConfigName("namesrvAddr");
        ConfigEntity entity = ConfigControllerMapper.INSTANCE.queryByInstanceId(query);
        ConfigMetadata metadata = ConfigConvertMetaData.INSTANCE.toMetaData(entity);
        Assert.assertEquals("namesrvAddr", metadata.nodeUnique());
        Assert.assertEquals("namesrvAddr", ConfigConvertMetaData.INSTANCE.toEntity(metadata).getName());
    }

    @Test
    public void entityJsonPreservesExistingHttpNamesAndAcceptsName() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        TopicEntity topic = mapper.readValue("{\"topicName\":\"orders\"}", TopicEntity.class);
        Assert.assertEquals("orders", topic.getName());
        Assert.assertEquals("orders", mapper.readTree(mapper.writeValueAsString(topic)).get("topicName").asText());
        Assert.assertEquals("orders", mapper.readValue("{\"name\":\"orders\"}", TopicEntity.class).getName());
        ConfigEntity config = mapper.readValue("{\"configName\":\"timeout\"}", ConfigEntity.class);
        Assert.assertEquals("timeout", config.getName());
        Assert.assertEquals("timeout", mapper.readTree(mapper.writeValueAsString(config)).get("configName").asText());
    }

    @Test
    public void batchInsertsBindRenamedProperties() throws Exception {
        TopicEntity topic = new TopicEntity();
        topic.setName("orders");
        ConfigEntity config = new ConfigEntity();
        config.setName("timeout");
        assertBatchNameBinding(TopicMapper.class, topic, "orders");
        assertBatchNameBinding(ConfigMapper.class, config, "timeout");
    }

    @Test
    public void databaseColumnsLoadIntoName() throws Exception {
        UnpooledDataSource source = new UnpooledDataSource("org.h2.Driver", "jdbc:h2:mem:" + UUID.randomUUID(), "sa", "");
        try (Connection connection = source.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("create table topic (id bigint, topic_name varchar(100), status int)");
            statement.execute("insert into topic values (100, 'orders', 1)");
            statement.execute("create table config (id bigint, config_name varchar(100), status int, is_default int)");
            statement.execute("insert into config values (200, 'timeout', 1, 0)");
            Configuration configuration = new Configuration(new Environment("name-test", new JdbcTransactionFactory(), source));
            configuration.setMapUnderscoreToCamelCase(true);
            configuration.addMapper(TopicMapper.class);
            configuration.addMapper(ConfigMapper.class);
            try (SqlSession session = new SqlSessionFactoryBuilder().build(configuration).openSession()) {
                Assert.assertEquals("orders", session.getMapper(TopicMapper.class).selectAll().get(0).getName());
                Assert.assertEquals("timeout", session.getMapper(ConfigMapper.class).selectAll().get(0).getName());
            }
        }
    }

    private void assertBatchNameBinding(Class<?> mapper, Object entity, String expected) throws Exception {
        String script = String.join(" ",
            mapper.getMethod("batchInsert", java.util.List.class).getAnnotation(Insert.class).value());
        Configuration configuration = new Configuration();
        BoundSql sql = new XMLLanguageDriver().createSqlSource(configuration, script, java.util.Map.class)
            .getBoundSql(Collections.singletonMap("list", Collections.singletonList(entity)));
        String property = sql.getParameterMappings().stream().map(value -> value.getProperty())
            .filter(value -> value.endsWith(".name")).findFirst().orElseThrow();
        Assert.assertEquals(expected, sql.getAdditionalParameter(property));
    }
}
