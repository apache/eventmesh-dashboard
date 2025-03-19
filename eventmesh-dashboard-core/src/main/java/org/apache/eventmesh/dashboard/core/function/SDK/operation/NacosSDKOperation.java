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

package org.apache.eventmesh.dashboard.core.function.SDK.operation;

import org.apache.eventmesh.dashboard.core.function.SDK.AbstractSDKOperation;
import org.apache.eventmesh.dashboard.core.function.SDK.config.CreateNacosConfig;
import org.apache.eventmesh.dashboard.core.function.SDK.wrapper.NacosSDKWrapper;

import java.util.Properties;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.naming.NamingService;

public class NacosSDKOperation extends AbstractSDKOperation<NacosSDKWrapper, CreateNacosConfig> {


    private static Properties createProperties(CreateNacosConfig clientConfig) {
        Properties properties = new Properties();
        properties.put(PropertyKeyConst.SERVER_ADDR, clientConfig.doUniqueKey());
        properties.put(PropertyKeyConst.NAMESPACE, clientConfig.getNamespace());
        properties.put(PropertyKeyConst.USERNAME, clientConfig.getUsername());
        properties.put(PropertyKeyConst.PASSWORD, clientConfig.getPassword());
        properties.put(PropertyKeyConst.ACCESS_KEY, clientConfig.getAccessKey());
        properties.put(PropertyKeyConst.SECRET_KEY, clientConfig.getSecretKey());
        return properties;
    }

    private final NacosConfigSDKOperation nacosConfigClientCreateOperation = new NacosConfigSDKOperation();

    private final NacosNamingSDKOperation nacosNamingClientCreateOperation = new NacosNamingSDKOperation();

    @Override
    public NacosSDKWrapper createClient(CreateNacosConfig createClientConfig) throws Exception {
        ConfigService configSimpleEntry = nacosConfigClientCreateOperation.createClient(createClientConfig);
        NamingService namingSimpleEntry = nacosNamingClientCreateOperation.createClient(createClientConfig);
        return new NacosSDKWrapper(configSimpleEntry, namingSimpleEntry);
    }

    @Override
    public void close(NacosSDKWrapper client) throws Exception {
        client.shutdown();
    }

    public static class NacosConfigSDKOperation extends AbstractSDKOperation<ConfigService, CreateNacosConfig> {

        @Override
        public ConfigService createClient(CreateNacosConfig clientConfig) throws Exception {
            return NacosFactory.createConfigService(createProperties(clientConfig));
        }

        @Override
        public void close(ConfigService client) throws Exception {
            client.shutDown();
        }
    }

    public static class NacosNamingSDKOperation extends AbstractSDKOperation<NamingService, CreateNacosConfig> {

        @Override
        public NamingService createClient(CreateNacosConfig clientConfig) throws Exception {
            return NacosFactory.createNamingService(createProperties(clientConfig));
        }

        @Override
        public void close(NamingService client) throws Exception {
            client.shutDown();
        }
    }
}
