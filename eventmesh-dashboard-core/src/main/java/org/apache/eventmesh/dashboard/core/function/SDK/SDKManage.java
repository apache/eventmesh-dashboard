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
import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;
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

    private static final SDKManage INSTANCE = new SDKManage();


    // register all client create operation
    static {
        Set<Class<?>> interfaceSet = new HashSet<>();
        interfaceSet.add(SDKOperation.class);
        ClasspathScanner classpathScanner =
            ClasspathScanner.builder().base(SDKManage.class).subPath("/operation/**").interfaceSet(interfaceSet).build();
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
                SDKTypeEnum[] sdkTypeEnums = sdkMetadata.sdkTypeEnum();
                if (Objects.equals(sdkTypeEnums[0], SDKTypeEnum.ALL)) {
                    sdkTypeEnums = new SDKTypeEnum[] {SDKTypeEnum.ADMIN, SDKTypeEnum.PING, SDKTypeEnum.PRODUCER, SDKTypeEnum.CONSUMER};
                }
                for (SDKTypeEnum sdkTypeEnum : sdkTypeEnums) {
                    SDKMetadataWrapper sdkMetadataWrapper = new SDKMetadataWrapper();
                    sdkMetadataWrapper.sdkMetadata = sdkMetadata;
                    sdkMetadataWrapper.createSDKConfigClass = multi;
                    sdkMetadataWrapper.abstractSDKOperation = (AbstractSDKOperation<Object, CreateSDKConfig>) clazz.newInstance();
                    map.put(sdkTypeEnum, sdkMetadataWrapper);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Class<?> getCreateSDKConfigClass(Class<?> genericClass) {
        Type supercType = genericClass.getGenericSuperclass();
        if (supercType instanceof ParameterizedType type) {
            Type[] typeArguments = type.getActualTypeArguments();
            for (Type typeArgument : typeArguments) {
                Class<?> argument;
                if (typeArgument instanceof ParameterizedType parameterizedType) {
                    argument = (Class<?>) parameterizedType.getRawType();
                } else {
                    argument = (Class<?>) typeArgument;
                }
                for (; ; ) {
                    Class<?> superclass = argument.getSuperclass();
                    if (Objects.isNull(superclass)) {
                        break;
                    }
                    if (Objects.equals(superclass, AbstractCreateSDKConfig.class)) {
                        if (typeArgument instanceof Class<?>) {
                            return (Class<?>) typeArgument;
                        }
                    }
                    argument = superclass;
                }
            }
        }
        return null;
    }

    private SDKManage() {
    }

    public static synchronized SDKManage getInstance() {
        return INSTANCE;
    }

    /**
     *  cluster 模式下，有问题？
     *  TODO 去重。 如果去重
     *       1. 只识别 地址？
     *       2. 识别 整个 CreateSDKConfig
     *
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
            // all 模式下应该共享一个对象。这里需要优化
            if (Objects.equals(SDKTypeEnum.ADMIN, sdkTypeEnum)) {
                object = sdkMetadataWrapper.abstractSDKOperation.createClient(config);
                wrapper.getClientMap().put(SDKTypeEnum.PING, object);
            }
            final String uniqueKey = baseSyncBase.getUnique();
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

    @SuppressWarnings("unchecked")
    public <T> T getClient(SDKTypeEnum clientTypeEnum, String uniqueKey) {
        return (T) clientMap.get(uniqueKey).getClientMap().get(clientTypeEnum);
    }

    public ClientWrapper getClientWrapper(String uniqueKey) {
        return clientMap.get(uniqueKey);
    }

    public <T> T createAbstractClientInfo(Class<?> clazz, BaseSyncBase baseSyncBase) {
        try {
            String unique = baseSyncBase.getUnique();
            if (!baseSyncBase.isCluster() && ClusterSyncMetadataEnum.getClusterFramework(baseSyncBase.getClusterType()).isCAP()) {
                unique = ((RuntimeMetadata) baseSyncBase).clusterUnique();
            }
            AbstractClientInfo<Object> abstractClientInfo = (AbstractClientInfo<Object>) clazz.newInstance();
            abstractClientInfo.setClientWrapper(clientMap.get(unique));
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
