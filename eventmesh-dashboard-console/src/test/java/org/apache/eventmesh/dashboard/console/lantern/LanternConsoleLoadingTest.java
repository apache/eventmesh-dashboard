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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;



import java.util.Map;

import org.apache.eventmesh.dashboard.console.controller.MetricsController;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor;

import com.lamp.lantern.plugins.core.authentication.AuthenticationManager;
import com.lamp.lantern.plugins.api.auth.AuthenticationService;
import com.lamp.lantern.plugins.core.login.HandlerService;
import com.lamp.lantern.plugins.springmvc.autoconfigure.LanternAuthenticationSupportConfigure;
import com.lamp.lantern.plugins.springmvc.support.AuthenticationInterceptor;
import com.lamp.lantern.plugins.springmvc.support.LanternAuthenticationUserMethodArgumentResolver;
import com.lamp.lantern.plugins.springmvc.support.LanternRequestResponseBodyMethodProcessor;
import com.lamp.lantern.plugins.springmvc.support.LanternSpringMVCBehavior;

class LanternConsoleLoadingTest {

    @Test
    void loadsWithoutLanternProperties() {
        this.assertDisabled();
    }

    @Test
    void loadsWithExplicitlyDisabledFeatures() {
        this.assertDisabled("lantern.loginConfig.enable=false", "lantern.authenticationConfig.enable=false",
            "server.port=9898", "lantern.handlerRedisMap.unreachable=redis://127.0.0.1:1/0");
    }

    @Test
    void serverPortDoesNotEnableLantern() {
        this.assertDisabled("server.port=9898");
    }

    private void assertDisabled(String... properties) {
        // 加载 Console 基础配置，验证引入插件后 MVC 仍按本地 Bean 模式运行。
        new WebApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withUserConfiguration(ProbeConfiguration.class, LanternConsoleTestApplication.class, MetricsController.class)
            .withPropertyValues("spring.profiles.active=lantern-loading-test")
            .withPropertyValues(properties)
            .run(context -> {
                assertThat(context).hasNotFailed().hasSingleBean(LanternAuthenticationSupportConfigure.class)
                    .doesNotHaveBean("healthEndpoint")
                    .doesNotHaveBean(AuthenticationService.class)
                    .doesNotHaveBean(AuthenticationManager.class).doesNotHaveBean(HandlerService.class)
                    .doesNotHaveBean(AuthenticationInterceptor.class).doesNotHaveBean(LanternSpringMVCBehavior.class)
                    .doesNotHaveBean(LanternAuthenticationUserMethodArgumentResolver.class);
                RequestMappingHandlerAdapter adapter = context.getBean(RequestMappingHandlerAdapter.class);
                assertThat(adapter.getArgumentResolvers())
                    .anyMatch(resolver -> resolver.getClass().equals(RequestResponseBodyMethodProcessor.class))
                    .noneMatch(resolver -> resolver instanceof LanternRequestResponseBodyMethodProcessor
                        || resolver instanceof LanternAuthenticationUserMethodArgumentResolver);
                MockMvc mvc = MockMvcBuilders.webAppContextSetup(context.getSourceApplicationContext()).build();
                mvc.perform(get("/security/currentUser")).andExpect(status().isNotFound());
                mvc.perform(get("/hello")).andExpect(status().isOk());
                mvc.perform(get("/lantern-smoke"))
                    .andExpect(status().isOk()).andExpect(content().string("ok"));
                mvc.perform(post("/lantern-smoke").contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"ok\"}"))
                    .andExpect(status().isOk()).andExpect(content().json("{\"message\":\"ok\"}"));
            });
    }

    @Configuration(proxyBeanMethods = false)
    static class ProbeConfiguration {

        @Bean
        ProbeController probeController() {
            return new ProbeController();
        }
    }

    @RestController
    static class ProbeController {

        @GetMapping("/lantern-smoke")
        String getProbe() {
            return "ok";
        }

        @PostMapping("/lantern-smoke")
        Map<String, String> postProbe(@RequestBody Map<String, String> body) {
            return body;
        }
    }
}
