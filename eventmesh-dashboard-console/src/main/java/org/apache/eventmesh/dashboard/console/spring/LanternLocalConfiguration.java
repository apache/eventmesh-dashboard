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

package org.apache.eventmesh.dashboard.console.spring;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import com.lamp.lantern.plugins.api.auth.config.RedisCacheConfig;
import com.lamp.lantern.plugins.core.authentication.service.CacheAuthenticationService;
import com.lamp.lantern.service.core.provider.mapper.ResourcesMapper;
import com.lamp.lantern.service.core.provider.mapper.RoleMapper;
import com.lamp.lantern.service.core.provider.mapper.UserInfoMapper;
import com.lamp.lantern.service.core.provider.service.impl.DBAuthenticationServiceImpl;
import com.zaxxer.hikari.HikariDataSource;

@Configuration(proxyBeanMethods = false)
@Profile("lantern")
public class LanternLocalConfiguration {

    @Bean
    @Lazy
    @Profile("lantern-redis")
    CacheAuthenticationService lanternLocalRedisAuthentication(Environment environment) {
        RedisCacheConfig config = new RedisCacheConfig();
        config.setRedisCacheUrl(environment.getRequiredProperty("lantern.local.redis-url"));
        config.setRedisCacheSystemName(environment.getRequiredProperty("lantern.local.redis-namespace"));
        return new CacheAuthenticationService(config);
    }

    @Configuration(proxyBeanMethods = false)
    @Profile("lantern & !lantern-redis")
    @Lazy
    @ComponentScan(basePackageClasses = DBAuthenticationServiceImpl.class, useDefaultFilters = false, lazyInit = true,
        includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = DBAuthenticationServiceImpl.class))
    @MapperScan(basePackages = "org.apache.eventmesh.dashboard.console.mapper",
        annotationClass = Mapper.class, sqlSessionFactoryRef = "sqlSessionFactory")
    static class DatabaseConfiguration {

        @Bean
        LanternMapperSession lanternMapperSession(Environment environment) {
            HikariDataSource dataSource = new HikariDataSource();
            dataSource.setJdbcUrl(environment.getRequiredProperty("lantern.local.jdbc-url"));
            dataSource.setUsername(environment.getRequiredProperty("lantern.local.username"));
            dataSource.setPassword(environment.getRequiredProperty("lantern.local.password"));
            dataSource.setMaximumPoolSize(2);
            dataSource.setMinimumIdle(0);
            dataSource.setConnectionTimeout(3000);
            dataSource.setValidationTimeout(1000);

            // Keep Lantern's data source and factory private so MyBatis can auto-configure the business factory.
            org.apache.ibatis.mapping.Environment mapperEnvironment =
                new org.apache.ibatis.mapping.Environment("lantern", new SpringManagedTransactionFactory(), dataSource);
            org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration(mapperEnvironment);
            configuration.setMapUnderscoreToCamelCase(true);
            configuration.setDefaultStatementTimeout(3);
            return new LanternMapperSession(dataSource, new SqlSessionFactoryBuilder().build(configuration));
        }

        @Bean
        MapperFactoryBean<UserInfoMapper> lanternUserInfoMapper(LanternMapperSession session) {
            return session.mapper(UserInfoMapper.class);
        }

        @Bean
        MapperFactoryBean<RoleMapper> lanternRoleMapper(LanternMapperSession session) {
            return session.mapper(RoleMapper.class);
        }

        @Bean
        MapperFactoryBean<ResourcesMapper> lanternResourcesMapper(LanternMapperSession session) {
            return session.mapper(ResourcesMapper.class);
        }
    }

    private record LanternMapperSession(HikariDataSource dataSource, SqlSessionFactory sqlSessionFactory) implements AutoCloseable {

        private <T> MapperFactoryBean<T> mapper(Class<T> mapperType) {
            MapperFactoryBean<T> mapper = new MapperFactoryBean<>(mapperType);
            mapper.setSqlSessionFactory(sqlSessionFactory);
            return mapper;
        }

        @Override
        public void close() {
            dataSource.close();
        }
    }
}
