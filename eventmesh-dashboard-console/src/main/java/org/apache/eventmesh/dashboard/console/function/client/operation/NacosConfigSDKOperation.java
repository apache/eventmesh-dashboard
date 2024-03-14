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

package org.apache.eventmesh.dashboard.console.function.client.operation;

import org.apache.eventmesh.dashboard.console.function.client.AbstractSDKOperation;
import org.apache.eventmesh.dashboard.console.function.client.config.CreateClientConfig;
import org.apache.eventmesh.dashboard.console.function.client.config.CreateNacosConfig;

import java.util.AbstractMap.SimpleEntry;
import java.util.Properties;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NacosConfigSDKOperation extends AbstractSDKOperation<ConfigService> {

    @Override
    public SimpleEntry<String, ConfigService> createClient(CreateClientConfig clientConfig) {
        ConfigService configService = null;
        CreateNacosConfig config = (CreateNacosConfig) clientConfig;
        try {
            Properties properties = new Properties();
            properties.put("serverAddr", config.getServerAddress());
            configService = NacosFactory.createConfigService(properties);
        } catch (NacosException e) {
            log.error("NacosCheck init failed caused by {}", e.getErrMsg());
        }
        return new SimpleEntry<>(config.getServerAddress(), configService);
    }

    @Override
    public void close(Object client) {
        try {
            castClient(client).shutDown();
        } catch (NacosException e) {
            log.error("NacosCheck close failed caused by {}", e.getErrMsg());
        }
    }
}