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

import org.apache.eventmesh.dashboard.common.enums.ClusterSyncMetadataEnum;
import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.model.base.BaseSyncBase;
import org.apache.eventmesh.dashboard.common.util.ClasspathScanner;
import org.apache.eventmesh.dashboard.core.function.SDK.config.AbstractCreateSDKConfig;
import org.apache.eventmesh.dashboard.core.function.SDK.config.CreateSDKConfig;

import org.apache.commons.lang3.ArrayUtils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


/**
 * SDK manager is a singleton to manage all SDK clients, it is a facade to create, delete and get a client.
 */
public class SDKManage {

    /**
     * inner key is the unique key of a client, such as (ip + port) they are defined in CreateClientConfig
     * <p>
     * key: SDKTypeEnum value: A map collection is used with key being (ip+port) and value being client.
     *
     * @see CreateSDKConfig#getUniqueKey()
     */
    private final Map<String, ClientWrapper> clientMap = new ConcurrentHashMap<>();
    /**
     * Initialise the SDKOperation object instance according to SDKTypeEnum.
     * <p>
     * key: SDKTypeEnum value: SDKOperation
     *
     * @see SDKTypeEnum
     * @see SDKOperation
     */
    private static final Map<ClusterType, Map<SDKTypeEnum, SDKMetadataWrapper>> CLUSTER_TYPE_MAP_CONCURRENT_HASH_MAP =
        new ConcurrentHashMap<>();
    private static volatile SDKManage INSTANCE = null;

    // register all client create operation
    static {
        Set<Class<?>> interfaceSet = new HashSet<>();
        interfaceSet.add(SDKOperation.class);
        ClasspathScanner classpathScanner = ClasspathScanner.builder().base(SDKManage.class).subPath("/operation").interfaceSet(interfaceSet).build();
        try {
            List<Class<?>> classList = classpathScanner.getClazz();
            classList.forEach(SDKManage::createSDKMetadataWrapper);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void createSDKMetadataWrapper(Class<?> clazz) {
        SDKMetadata[] sdkMetadataArray = clazz.getAnnotationsByType(SDKMetadata.class);
        if (ArrayUtils.isEmpty(sdkMetadataArray)) {
            return;
        }

        try {
            SDKMetadata sdkMetadata = sdkMetadataArray[0];
            Class<?> multi = getCreateSDKConfigClass(clazz);
            for (ClusterType clusterType : sdkMetadata.clusterType()) {
                Map<SDKTypeEnum, SDKMetadataWrapper> map =
                    CLUSTER_TYPE_MAP_CONCURRENT_HASH_MAP.computeIfAbsent(clusterType, k -> new ConcurrentHashMap<>());
                for (SDKTypeEnum sdkTypeEnum : sdkMetadata.sdkTypeEnum()) {
                    SDKMetadataWrapper sdkMetadataWrapper = new SDKMetadataWrapper();
                    sdkMetadataWrapper.sdkMetadata = sdkMetadata;
                    sdkMetadataWrapper.createSDKConfigClass = multi;
                    sdkMetadataWrapper.abstractSDKOperation = (AbstractSDKOperation<Object, CreateSDKConfig>) clazz.newInstance();
                    map.put(sdkTypeEnum, sdkMetadataWrapper);
                }
            }
            SDKMetadataWrapper sdkMetadataWrapper = new SDKMetadataWrapper();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Class<?> getCreateSDKConfigClass(Class<?> genericClass) {
        Type type = genericClass.getGenericSuperclass();
        if (type instanceof ParameterizedType) {
            ParameterizedType ptype = (ParameterizedType) type;
            Type[] typeArguments = ptype.getActualTypeArguments();
            for (Type typeArgument : typeArguments) {
                if (Objects.equals(typeArgument, AbstractCreateSDKConfig.class)) {
                    return (Class<?>) typeArgument;
                }
            }
        }
        return null;
    }

    private SDKManage() {
    }

    public static synchronized SDKManage getInstance() {
        if (INSTANCE == null) {
            synchronized (SDKManage.class) {
                if (INSTANCE == null) {
                    INSTANCE = new SDKManage();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Create SDK client through (SDKTypeEnum) clientTypeEnum, (CreateSDKConfig) config.
     */
    public <T> T createClient(SDKTypeEnum sdkTypeEnum, BaseSyncBase baseSyncBase, CreateSDKConfig config, ClusterType clusterType) {

        try {

            SDKMetadataWrapper sdkMetadataWrapper = CLUSTER_TYPE_MAP_CONCURRENT_HASH_MAP.get(clusterType).get(sdkTypeEnum);

            Object object = sdkMetadataWrapper.abstractSDKOperation.createClient(config);
            if (Objects.equals(sdkTypeEnum, SDKTypeEnum.PRODUCER) || Objects.equals(sdkTypeEnum, SDKTypeEnum.CONSUMER)) {
                return (T) object;
            }

            ClientWrapper wrapper = new ClientWrapper();
            wrapper.setConfig(config);
            wrapper.setBaseSyncBase(baseSyncBase);

            wrapper.getClientMap().put(SDKTypeEnum.ADMIN, object);
            if (Objects.equals(SDKTypeEnum.ADMIN, sdkTypeEnum)) {
                object = sdkMetadataWrapper.abstractSDKOperation.createClient(config);
                wrapper.getClientMap().put(SDKTypeEnum.PING, object);
            }
            final String uniqueKey = config.getUniqueKey();
            clientMap.put(uniqueKey, wrapper);
            return (T) object;
        } catch (Exception e) {
            throw new RuntimeException("create client error", e);
        }
    }


    public void deleteClient(SDKTypeEnum sdkTypeEnum, String uniqueKey) {
        if (Objects.isNull(sdkTypeEnum)) {
            this.clientMap.remove(uniqueKey);
        } else {
            this.clientMap.get(uniqueKey).getClientMap().put(sdkTypeEnum, null);
        }
    }

    public Object getClient(SDKTypeEnum clientTypeEnum, String uniqueKey) {
        return clientMap.get(uniqueKey).getClientMap().get(clientTypeEnum);
    }

    public ClientWrapper getClientWrapper(String uniqueKey) {
        return clientMap.get(uniqueKey);
    }

    public <T> T createAbstractClientInfo(Class<?> clazz, BaseSyncBase baseSyncBase) {
        try {
            if (ClusterSyncMetadataEnum.getClusterSyncMetadata(baseSyncBase.getClusterType()).getClusterFramework().isCAP()) {

            }
            AbstractClientInfo abstractClientInfo = (AbstractClientInfo) clazz.newInstance();
            abstractClientInfo.setClientWrapper(clientMap.get(baseSyncBase.getUnique()));
            return (T) abstractClientInfo;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Class<?> getConfig(ClusterType clusterType, SDKTypeEnum sdkTypeEnum) {
        return CLUSTER_TYPE_MAP_CONCURRENT_HASH_MAP.get(clusterType).get(sdkTypeEnum).createSDKConfigClass;
    }


    static class SDKMetadataWrapper {

        private SDKMetadata sdkMetadata;

        private Class<?> createSDKConfigClass;

        private AbstractSDKOperation<Object, CreateSDKConfig> abstractSDKOperation;

    }
}
