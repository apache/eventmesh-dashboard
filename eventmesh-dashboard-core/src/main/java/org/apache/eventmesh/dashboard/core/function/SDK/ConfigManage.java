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


package org.apache.eventmesh.dashboard.core.function.SDK;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.core.function.SDK.config.AbstractCreateSDKConfig;
import org.apache.eventmesh.dashboard.core.function.SDK.config.AbstractMultiCreateSDKConfig;
import org.apache.eventmesh.dashboard.core.function.SDK.config.AbstractSimpleCreateSDKConfig;

public class ConfigManage {

    private static final ConfigManage configManage = new ConfigManage();

    private ConfigManage() {
    }

    public static ConfigManage getInstance() {
        return configManage;
    }

    public AbstractMultiCreateSDKConfig getMultiCreateSDKConfig(ClusterType clusterType, SDKTypeEnum sdkTypeEnum) {
        return (AbstractMultiCreateSDKConfig) getCreateSDKConfig(clusterType, sdkTypeEnum);
    }

    public AbstractSimpleCreateSDKConfig getSimpleCreateSDKConfig(ClusterType clusterType, SDKTypeEnum sdkTypeEnum) {
        return (AbstractSimpleCreateSDKConfig) getCreateSDKConfig(clusterType, sdkTypeEnum);
    }

    private AbstractCreateSDKConfig getCreateSDKConfig(ClusterType clusterType, SDKTypeEnum sdkTypeEnum) {

        try {
            Class<?> clazz = SDKManage.getInstance().getConfig(clusterType, sdkTypeEnum);
            return (AbstractCreateSDKConfig) clazz.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
