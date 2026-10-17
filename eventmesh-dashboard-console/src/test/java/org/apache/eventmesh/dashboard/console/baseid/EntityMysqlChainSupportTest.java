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

package org.apache.eventmesh.dashboard.console.baseid;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.Assume;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/** Isolates real HTTP/MySQL tests without starting unrelated Dashboard background services. */
public abstract class EntityMysqlChainSupportTest {

    protected Fixture fixture(String... tables) throws Exception {
        Assume.assumeTrue("Enable real local MySQL acceptance with -Dentity.chain.mysql=true",
            Boolean.getBoolean("entity.chain.mysql"));
        Fixture fixture = new Fixture();
        try {
            for (String table : tables) {
                fixture.createTable(table);
            }
            return fixture;
        } catch (Exception | AssertionError error) {
            fixture.close();
            throw error;
        }
    }

    protected static final class Fixture implements AutoCloseable {

        private static final Logger LOGGER = LoggerFactory.getLogger(Fixture.class);

        private final String schema = "em_entity_chain_" + UUID.randomUUID().toString().replace("-", "");

        private final Connection admin;

        private final DataSource dataSource;

        private final Map<String, Object> mappers = new LinkedHashMap<>();

        private AnnotationConfigServletWebServerApplicationContext context;

        private Fixture() throws Exception {
            String server = System.getenv("ENTITY_CHAIN_MYSQL_SERVER");
            assertNotNull("ENTITY_CHAIN_MYSQL_SERVER is required", server);
            assertTrue("Only a loopback MySQL test server is allowed",
                server.matches("jdbc:mysql://127\\.0\\.0\\.1:[0-9]+/"));
            String user = System.getenv("ENTITY_CHAIN_MYSQL_USER");
            String password = System.getenv("ENTITY_CHAIN_MYSQL_PASSWORD");
            String options = "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai";
            admin = DriverManager.getConnection(server + options, user, password);
            try (Statement statement = admin.createStatement()) {
                statement.execute("CREATE DATABASE " + schema);
            }
            dataSource = new UnpooledDataSource("com.mysql.cj.jdbc.Driver", server + schema + options, user, password);
        }

        public String schemaName() {
            return schema;
        }

        public JdbcTemplate jdbc() {
            return new JdbcTemplate(dataSource);
        }

        public TransactionTemplate transactions() {
            return new TransactionTemplate(context.getBean(DataSourceTransactionManager.class));
        }

        public void seedRuntime(long id, long clusterId) {
            jdbc().update("""
                INSERT INTO runtime (id, organization_id, cluster_id, cluster_type, name, host, port, version,
                    jmx_port, trusteeship_type, first_to_whom, replication_type, deploy_status_type,
                    kubernetes_cluster_id, create_script_content, resources_config_id, deploy_script_id)
                VALUES (?, 1, ?, 'EVENTMESH', 'chain-runtime', ?, 10000, '1.0.0', '0', 'SELF',
                    'DASHBOARD', 'NOT', 'CREATE_SUCCESS', ?, '', '0', 0)
                """, id, clusterId, id, id);
        }

        public <T> T mapper(Class<T> type) {
            Configuration configuration = new Configuration(
                new Environment("entity-mysql-chain", new SpringManagedTransactionFactory(), dataSource));
            configuration.setMapUnderscoreToCamelCase(true);
            configuration.addMapper(type);
            T mapper = new SqlSessionTemplate(new SqlSessionFactoryBuilder().build(configuration)).getMapper(type);
            mappers.put(type.getSimpleName(), mapper);
            return mapper;
        }

        public void startHttp(Class<?>... components) throws Exception {
            context = new AnnotationConfigServletWebServerApplicationContext();
            mappers.forEach((name, mapper) -> context.getBeanFactory().registerSingleton(name, mapper));
            context.getBeanFactory().registerSingleton("transactionManager", new DataSourceTransactionManager(dataSource));
            context.register(HttpConfiguration.class);
            context.register(components);
            context.refresh();
        }

        public <T> T bean(Class<T> type) {
            return context.getBean(type);
        }

        public HttpResponse<String> post(String path, String json) throws Exception {
            int port = context.getWebServer().getPort();
            HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
            HttpResponse<String> response = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()
                .send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();
            LOGGER.info("HTTP {} returned {} with body: {}", path, response.statusCode(),
                body.substring(0, Math.min(body.length(), 2048)));
            return response;
        }

        private void createTable(String table) throws Exception {
            if ("acl".equals(table) || "runtime".equals(table) || "cluster_relationship".equals(table)) {
                try (InputStream stream = getClass().getResourceAsStream("/eventmesh-dashboard.sql")) {
                    assertNotNull("Production schema resource", stream);
                    String source = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                    // The column comments contain semicolons; end at the table's closing line.
                    Matcher matcher = Pattern.compile("(?ims)^create table `?" + table + "`?\\s*\\(.*?^\\)[^;]*;").matcher(source);
                    assertTrue("DDL exists in the production schema: " + table, matcher.find());
                    jdbc().execute(matcher.group());
                }
            } else if ("connection".equals(table)) {
                // The repository and local database have no production connection table.
                // This fixture verifies mapper field binding, not production schema or uniqueness semantics.
                jdbc().execute("""
                    CREATE TABLE connection (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        cluster_id BIGINT NOT NULL,
                        source_type VARCHAR(64), source_id BIGINT,
                        sink_type VARCHAR(64), sink_id BIGINT,
                        runtime_id BIGINT, status INT DEFAULT 1,
                        topic VARCHAR(255), group_id BIGINT, description TEXT,
                        create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        end_time TIMESTAMP NULL,
                        update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                    )
                    """);
            } else if ("connector".equals(table)) {
                // Production connector DDL contains an invalid standalone 'x'. Keep that resource unchanged.
                // This synthetic table provides existing mapper columns, not production DDL acceptance.
                jdbc().execute("""
                    CREATE TABLE connector (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT, cluster_id BIGINT NOT NULL,
                        name VARCHAR(512) NOT NULL, class_name VARCHAR(512) NOT NULL, type VARCHAR(32) NOT NULL,
                        host VARCHAR(128) NOT NULL, port INT NOT NULL, status INT DEFAULT 1,
                        pod_state INT DEFAULT 0, config_ids VARCHAR(1024) DEFAULT '',
                        create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                    )
                    """);
            } else {
                throw new IllegalArgumentException("Unsupported isolated fixture table: " + table);
            }
        }

        @Override
        public void close() throws Exception {
            try {
                if (context != null) {
                    context.close();
                }
            } finally {
                try (Connection connection = admin; Statement statement = connection.createStatement()) {
                    statement.execute("DROP DATABASE " + schema);
                }
            }
        }
    }

    @org.springframework.context.annotation.Configuration
    @EnableWebMvc
    @EnableTransactionManagement
    public static class HttpConfiguration {

        @Bean
        public TomcatServletWebServerFactory webServerFactory() throws Exception {
            TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory(0);
            factory.setAddress(InetAddress.getByName("127.0.0.1"));
            return factory;
        }

        @Bean
        public DispatcherServlet dispatcherServlet() {
            return new DispatcherServlet();
        }

        @Bean
        public ServletRegistrationBean<DispatcherServlet> dispatcherRegistration(DispatcherServlet servlet) {
            ServletRegistrationBean<DispatcherServlet> registration = new ServletRegistrationBean<>(servlet, "/");
            registration.setLoadOnStartup(1);
            return registration;
        }
    }
}
