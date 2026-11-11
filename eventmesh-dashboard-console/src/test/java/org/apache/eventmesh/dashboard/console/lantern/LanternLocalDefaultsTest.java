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

package org.apache.eventmesh.dashboard.console.lantern;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

import com.lamp.lantern.service.core.provider.autoconfigure.LanternLocalAuthenticationEnvironmentPostProcessor;

class LanternLocalDefaultsTest {

    @Test
    void usesLocalDefaultsWithOnlyTheAuthenticationSwitch() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("lantern.authenticationConfig.enable", "true")
            .withProperty("spring.application.name", "test-console");
        this.loadDefaults(environment);

        assertThat(environment.getProperty("dubbo.application.name")).isEqualTo("test-console");
        assertThat(environment.getProperty("dubbo.registry.address")).isEqualTo("N/A");
        assertThat(environment.getProperty("dubbo.provider.scope")).isEqualTo("local");
        assertThat(environment.getProperty("dubbo.consumer.scope")).isEqualTo("local");
        assertThat(environment.getProperty("lantern.authenticationConfig.authenticationFlowConfig.tokenName"))
            .isEqualTo("Authorization");
    }

    @Test
    void preservesExplicitHostSettings() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("lantern.authenticationConfig.enable", "true")
            .withProperty("dubbo.application.name", "existing-dubbo-application")
            .withProperty("dubbo.consumer.group", "existing-group");
        this.loadDefaults(environment);

        assertThat(environment.getProperty("dubbo.application.name")).isEqualTo("existing-dubbo-application");
        assertThat(environment.getProperty("dubbo.consumer.group")).isEqualTo("existing-group");
    }

    @Test
    void disabledAuthenticationDoesNotChangeTheHostEnvironment() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("lantern.authenticationConfig.enable", "false");
        this.loadDefaults(environment);

        assertThat(environment.getProperty("dubbo.protocol.name")).isNull();
        assertThat(environment.getProperty("spring.sql.init.mode")).isNull();
        assertThat(environment.getProperty("spring.autoconfigure.exclude")).isNull();
    }

    @Test
    void explicitAuthenticationServiceDoesNotSelectTheLocalDatabaseMode() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("lantern.authenticationConfig.enable", "true")
            .withProperty("lantern.authenticationConfig.defaultAuthenticationService.authenticationServiceName", "customAuthentication");
        this.loadDefaults(environment);

        assertThat(environment.getProperty("dubbo.protocol.name")).isNull();
        assertThat(environment.getProperty("spring.sql.init.mode")).isNull();
        assertThat(environment.getProperty("spring.autoconfigure.exclude")).isNull();
    }

    private void loadDefaults(MockEnvironment environment) {
        new LanternLocalAuthenticationEnvironmentPostProcessor().postProcessEnvironment(environment, new SpringApplication());
    }
}
