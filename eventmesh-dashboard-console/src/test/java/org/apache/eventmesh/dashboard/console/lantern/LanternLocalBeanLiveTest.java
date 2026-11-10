/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.eventmesh.dashboard.console.lantern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lamp.lantern.plugins.api.auth.AuthenticationData;
import com.lamp.lantern.plugins.api.auth.AuthenticationService;
import com.lamp.lantern.plugins.core.authentication.AuthenticationManager;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.UUID;

import javax.sql.DataSource;

import org.apache.dubbo.rpc.service.EchoService;
import org.apache.eventmesh.dashboard.console.controller.MetricsController;
import org.apache.eventmesh.dashboard.console.mapper.cluster.ClusterMapper;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.springframework.boot.Banner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;

class LanternLocalBeanLiveTest {

    private static final String JDBC_URL =
        "jdbc:mysql://127.0.0.1:13316/lantern_v2?useSSL=false&allowPublicKeyRetrieval=true&connectTimeout=3000&socketTimeout=3000";
    private static final String REDIS_URL = "redis://127.0.0.1:16379/0";
    private static final String MYSQL_USERNAME = "lantern_test";
    private static final String MYSQL_PASSWORD = "lantern_test";
    private static final String POLICY_ROUTE = "/lantern-policy-probe/{id}";

    @Test
    void queriesUsersThroughMysqlBeanWithSeparateBusinessSessionFactory() throws Exception {
        String token = UUID.randomUUID().toString();
        try (Connection connection = DriverManager.getConnection(JDBC_URL, MYSQL_USERNAME, MYSQL_PASSWORD)) {
            JdbcTemplate jdbc = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
            jdbc.update("INSERT INTO user_info(system_id,ui_name,ui_nickname,ui_id_card,ui_token,create_user_id,update_user_id) "
                + "VALUES(915,'lantern-local','local-test',?,?,0,0)", token, token);
            try (ConfigurableApplicationContext app = this.start("--spring.profiles.active=lantern",
                "--lantern.local.jdbc-url=" + JDBC_URL, "--lantern.local.username=" + MYSQL_USERNAME,
                "--lantern.local.password=" + MYSQL_PASSWORD)) {
                assertThat(app.getBeansOfType(DataSource.class)).containsOnlyKeys("businessDataSource");
                SqlSessionFactory businessSessionFactory = app.getBean(SqlSessionFactory.class);
                assertThat(businessSessionFactory.getConfiguration().getEnvironment().getDataSource())
                    .isSameAs(app.getBean("businessDataSource"));
                assertThat(app.getBean(ClusterMapper.class)).isNotNull();
                MapperFactoryBean<?> businessMapper = app.getBean("&clusterMapper", MapperFactoryBean.class);
                assertThat(businessMapper.getSqlSessionFactory()).isSameAs(businessSessionFactory);

                AuthenticationService service = app.getBean("DBAuthentication", AuthenticationService.class);
                AuthenticationData data = new AuthenticationData();
                data.setToken(token);
                assertThat(service.getUserInfo(data).getUserInfo().getUiName()).isEqualTo("lantern-local");
                data.setToken(UUID.randomUUID().toString());
                assertThat(service.getUserInfo(data).isSuccess()).isFalse();

                MockMvc mvc = MockMvcBuilders.standaloneSetup(app.getBean(MetricsController.class)).build();
                mvc.perform(get("/hello")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").value("Hello, EventMesh Dashboard!"));
                mvc.perform(get("/druid/stat")).andExpect(status().isOk());
            } finally {
                jdbc.update("DELETE FROM user_info WHERE ui_id_card=?", token);
            }
        }
    }

    @Test
    void appliesDatabasePoliciesToTheRouteTemplateAndChecksTheCurrentUsersRole() throws Exception {
        long systemId = UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;
        String permittedToken = UUID.randomUUID().toString();
        String otherToken = UUID.randomUUID().toString();
        try (Connection connection = DriverManager.getConnection(JDBC_URL, MYSQL_USERNAME, MYSQL_PASSWORD)) {
            JdbcTemplate jdbc = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
            try {
                jdbc.update("INSERT INTO user_info(system_id,ui_name,ui_nickname,ui_id_card,ui_token,create_user_id,update_user_id) "
                    + "VALUES(?,'role-user','local-test',?,?,0,0),(?,'other-user','local-test',?,?,0,0)",
                    systemId, permittedToken, permittedToken, systemId, otherToken, otherToken);
                jdbc.update("INSERT INTO resources(system_id,resource_type,resource_operate,resource_name,resource_conditions,"
                    + "start_time,valid_time,end_time,create_user_id,update_user_id) "
                    + "VALUES(?,'HTTP','GET',?,'PUBLIC',NOW()-INTERVAL 1 DAY,NOW()+INTERVAL 1 DAY,NOW()+INTERVAL 1 DAY,0,0)",
                    systemId, POLICY_ROUTE);
                jdbc.update("INSERT INTO `role`(role_type_id,system_id,role_name,start_time,valid_time,end_time,create_user_id,update_user_id) "
                    + "VALUES(1,?,'api-user',NOW()-INTERVAL 1 DAY,NOW()+INTERVAL 1 DAY,NOW()+INTERVAL 1 DAY,0,0)", systemId);
                Long userId = jdbc.queryForObject("SELECT ui_id FROM user_info WHERE ui_token=?", Long.class, permittedToken);
                Long resourceId = jdbc.queryForObject("SELECT resource_id FROM resources WHERE system_id=?", Long.class, systemId);
                Long roleId = jdbc.queryForObject("SELECT role_id FROM `role` WHERE system_id=?", Long.class, systemId);
                jdbc.update("INSERT INTO user_role_relation(urr_type,role_id,user_id,start_time,valid_time,end_time,create_user_id,update_user_id) "
                    + "VALUES('USER',?,?,NOW()-INTERVAL 1 DAY,NOW()+INTERVAL 1 DAY,NOW()+INTERVAL 1 DAY,0,0)", roleId, userId);
                jdbc.update("INSERT INTO resource_role_relation(resource_id,role_id,start_time,valid_time,end_time,create_user_id,update_user_id) "
                    + "VALUES(?,?,NOW()-INTERVAL 1 DAY,NOW()+INTERVAL 1 DAY,NOW()+INTERVAL 1 DAY,0,0)", resourceId, roleId);

                try (ConfigurableApplicationContext app = new SpringApplicationBuilder(
                    LanternConsoleTestApplication.class, BusinessDataSourceConfiguration.class, PolicyProbeController.class)
                    .web(WebApplicationType.SERVLET).bannerMode(Banner.Mode.OFF).logStartupInfo(false)
                    .run("--spring.profiles.active=lantern", "--server.port=0", "--lantern.authenticationConfig.enable=true",
                        "--lantern.authenticationConfig.authenticationFlowConfig.systemId=" + systemId,
                        "--lantern.authenticationConfig.authenticationFlowConfig.includePaths[0]=" + POLICY_ROUTE,
                        "--lantern.local.jdbc-url=" + JDBC_URL, "--lantern.local.username=" + MYSQL_USERNAME,
                        "--lantern.local.password=" + MYSQL_PASSWORD)) {
                    assertThat(app.getBeansOfType(AuthenticationService.class)).containsOnlyKeys("DBAuthentication");
                    AuthenticationService authentication = app.getBean(AuthenticationManager.class).getAuthenticationService();
                    assertThat(((EchoService) authentication).$echo("auth")).isEqualTo("auth");
                    MockMvc mvc = MockMvcBuilders.webAppContextSetup((WebApplicationContext) app).build();
                    String requestPath = "/lantern-policy-probe/123";
                    mvc.perform(get(requestPath)).andExpect(status().isOk());
                    jdbc.update("UPDATE resources SET resource_conditions='USER' WHERE resource_id=?", resourceId);
                    mvc.perform(get(requestPath)).andExpect(status().isUnauthorized());
                    mvc.perform(get(requestPath).header(HttpHeaders.AUTHORIZATION, "Bearer " + otherToken))
                        .andExpect(status().isOk());
                    jdbc.update("UPDATE resources SET resource_conditions='ROLE' WHERE resource_id=?", resourceId);
                    mvc.perform(get(requestPath).header(HttpHeaders.AUTHORIZATION, "Bearer " + otherToken))
                        .andExpect(status().isForbidden());
                    mvc.perform(get(requestPath).header(HttpHeaders.AUTHORIZATION, "Bearer " + permittedToken))
                        .andExpect(status().isOk());
                }
            } finally {
                jdbc.update("DELETE urr FROM user_role_relation urr JOIN user_info ui ON ui.ui_id=urr.user_id WHERE ui.system_id=?", systemId);
                jdbc.update("DELETE rrr FROM resource_role_relation rrr JOIN resources re ON re.resource_id=rrr.resource_id WHERE re.system_id=?",
                    systemId);
                jdbc.update("DELETE FROM resources WHERE system_id=?", systemId);
                jdbc.update("DELETE FROM `role` WHERE system_id=?", systemId);
                jdbc.update("DELETE FROM user_info WHERE system_id=?", systemId);
            }
        }
    }

    @Test
    void queriesSessionsThroughOriginalRedisBean() {
        String namespace = "lantern-local-test-" + UUID.randomUUID();
        String token = UUID.randomUUID().toString();
        String key = namespace + ":tokenUser:" + token;
        RedisClient client = RedisClient.create(REDIS_URL);
        try (StatefulRedisConnection<String, String> redis = client.connect()) {
            redis.sync().setex(key, 120, "{\"uiId\":91501,\"uiAllowLogin\":\"ACTIVE\",\"isDelete\":0}");
            try (ConfigurableApplicationContext app = this.start("--spring.profiles.active=lantern,lantern-redis",
                "--lantern.local.redis-url=" + REDIS_URL, "--lantern.local.redis-namespace=" + namespace)) {
                assertThat(app.containsBean("DBAuthentication")).isFalse();
                AuthenticationService service = app.getBean("lanternLocalRedisAuthentication", AuthenticationService.class);
                AuthenticationData data = new AuthenticationData();
                data.setToken(token);
                assertThat(service.getUserInfo(data).getUserInfo().getUiId()).isEqualTo(91501L);
                redis.sync().del(key);
                assertThat(service.getUserInfo(data).isSuccess()).isFalse();
            } finally {
                redis.sync().del(key);
            }
        } finally {
            client.shutdown();
        }
    }

    private ConfigurableApplicationContext start(String... properties) {
        return new SpringApplicationBuilder(LanternConsoleTestApplication.class, BusinessDataSourceConfiguration.class, MetricsController.class)
            .web(WebApplicationType.NONE).bannerMode(Banner.Mode.OFF).logStartupInfo(false)
            .run(properties);
    }

    @Configuration(proxyBeanMethods = false)
    static class BusinessDataSourceConfiguration {

        @Bean
        DataSource businessDataSource() {
            return new DriverManagerDataSource("jdbc:mysql://127.0.0.1:1/business", "unused", "unused");
        }
    }

    @RestController
    static class PolicyProbeController {

        @GetMapping(POLICY_ROUTE)
        String protectedResource() {
            return "ok";
        }
    }
}
