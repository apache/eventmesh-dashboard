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

package org.apache.eventmesh.dashboard.console.function.SDK.operation;

import org.apache.eventmesh.dashboard.console.function.SDK.AbstractSDKOperation;
import org.apache.eventmesh.dashboard.console.function.SDK.config.CreateNacosConfig;
import org.apache.eventmesh.dashboard.console.function.SDK.config.CreateSDKConfig;

import java.util.AbstractMap.SimpleEntry;
import java.util.Properties;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NacosNamingSDKOperation extends AbstractSDKOperation<NamingService> {

    @Override
    public SimpleEntry<String, NamingService> createClient(CreateSDKConfig clientConfig) {
        NamingService namingService = null;
        CreateNacosConfig config = (CreateNacosConfig) clientConfig;
        try {
            Properties properties = new Properties();
            properties.put("serverAddr", config.getServerAddress());
            namingService = NacosFactory.createNamingService(properties);
        } catch (NacosException e) {
            log.error("NacosCheck init failed caused by {}", e.getErrMsg());
        }
        return new SimpleEntry<>(config.getUniqueKey(), namingService);
    }

    @Override
    public void close(Object client) {
        try {
            ((ConfigService) client).shutDown();
        } catch (NacosException e) {
            log.error("NacosCheck close failed caused by {}", e.getErrMsg());
        }
    }
}
