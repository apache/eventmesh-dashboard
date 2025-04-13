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

package org.apache.eventmesh.dashboard.core.remoting;

import org.apache.eventmesh.dashboard.common.annotation.RemotingServiceMapper;
import org.apache.eventmesh.dashboard.common.annotation.RemotingServiceMethodMapper;
import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.model.base.BaseClusterIdBase;
import org.apache.eventmesh.dashboard.common.model.base.BaseSyncBase;
import org.apache.eventmesh.dashboard.common.model.remoting.AbstractGlobal2Request;
import org.apache.eventmesh.dashboard.common.model.remoting.GlobalResult;
import org.apache.eventmesh.dashboard.common.model.remoting.RemotingActionType;
import org.apache.eventmesh.dashboard.common.util.ClasspathScanner;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKManage;
import org.apache.eventmesh.dashboard.core.metadata.DataMetadataHandler;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class Remoting2Manage {

    private static final Remoting2Manage INSTANCE = new Remoting2Manage();

    private static final Map<ClusterType, Map<Class<?>, RemotingServiceMetadataWrapper>> REMOTING_SERVICE_METADATA_WRAPPER_MAP = new HashMap<>();

    private static final Map<Class<?>, Map<RemotingActionType, RemotingServiceMethodMapperWrapper>> CLASS_METHOD_MAPPER = new HashMap<>();

    static {
        ClasspathScanner classpathScanner = ClasspathScanner.builder().base(Remoting2Manage.class).subPath("/**").build();
        try {
            classpathScanner.getClazz().forEach(Remoting2Manage::registerService);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void registerService(Class<?> clazz) {
        if (ArrayUtils.isEmpty(clazz.getInterfaces())) {
            return;
        }
        if (Objects.isNull(clazz.getSuperclass())) {
            return;
        }
        RemotingServiceMapper remotingServiceMapper = clazz.getSuperclass().getAnnotation(RemotingServiceMapper.class);
        if (Objects.isNull(remotingServiceMapper)) {
            return;
        }
        final Class<?> interfaces = clazz.getInterfaces()[0];

        final Map<Class<?>, RemotingServiceMetadataWrapper> remotingServiceMetadataWrapperMap =
            REMOTING_SERVICE_METADATA_WRAPPER_MAP.computeIfAbsent(remotingServiceMapper.clusterType(), k -> new ConcurrentHashMap<>());

        RemotingServiceMetadataWrapper remotingServiceMetadataWrapper = new RemotingServiceMetadataWrapper();
        remotingServiceMetadataWrapper.clusterType = remotingServiceMapper.clusterType();
        remotingServiceMetadataWrapper.remotingServiceType = clazz;
        remotingServiceMetadataWrapper.remotingServiceMapper = remotingServiceMapper;
        remotingServiceMetadataWrapper.targetClass = interfaces;
        remotingServiceMetadataWrapperMap.put(interfaces, remotingServiceMetadataWrapper);

        Map<RemotingActionType, RemotingServiceMethodMapperWrapper> actionMap = CLASS_METHOD_MAPPER.get(interfaces);
        if (Objects.isNull(actionMap)) {
            actionMap = new HashMap<>();
            CLASS_METHOD_MAPPER.put(interfaces, actionMap);
            Method[] methods = interfaces.getMethods();
            for (Method method : methods) {
                RemotingServiceMethodMapper remotingServiceMethodMappers = method.getAnnotation(RemotingServiceMethodMapper.class);
                if (Objects.isNull(remotingServiceMethodMappers)) {
                    continue;
                }
                RemotingServiceMethodMapperWrapper remotingServiceMethodMapperWrapper = new RemotingServiceMethodMapperWrapper();
                remotingServiceMethodMapperWrapper.remotingServiceMethod = method;

                for (RemotingActionType remotingActionType : remotingServiceMethodMappers.value()) {
                    actionMap.put(remotingActionType, remotingServiceMethodMapperWrapper);
                }

                Class<?>[] clazzs = method.getParameterTypes();
                if (ArrayUtils.isNotEmpty(clazzs)) {
                    remotingServiceMethodMapperWrapper.parameterTypes = clazzs[0];
                }
            }

        }
        remotingServiceMetadataWrapper.actionMap = actionMap;

    }


    public static Remoting2Manage getInstance() {
        return INSTANCE;
    }

    private Remoting2Manage() {}

    /**
     * @param clazz        Remoting Service
     * @param baseSyncBase baseSyncBase
     * @return RemotingService
     */
    public DataMetadataHandler<BaseClusterIdBase> createDataMetadataHandler(Class<?> clazz, BaseSyncBase baseSyncBase) {
        RemotingServiceMetadataWrapper remotingServiceMetadataWrapper =
            REMOTING_SERVICE_METADATA_WRAPPER_MAP.get(baseSyncBase.getClusterType()).get(clazz);
        AbstractRemotingService<BaseClusterIdBase> proxyObject =
            SDKManage.getInstance().createAbstractClientInfo(remotingServiceMetadataWrapper.remotingServiceType, baseSyncBase);
        RemotingService<BaseClusterIdBase> remotingService = new RemotingService<>();
        remotingService.execution = proxyObject;
        remotingService.wrapper = remotingServiceMetadataWrapper;
        return remotingService;
    }


    @SuppressWarnings("unchecked")
    public static class RemotingService<T> implements DataMetadataHandler<T> {

        private RemotingServiceMetadataWrapper wrapper;

        private Object execution;

        @Override
        public void handleAll(List<T> addData, List<T> updateData, List<T> deleteData) {
            this.execute(addData, RemotingActionType.ADD);
            this.execute(addData, RemotingActionType.UPDATE);
            this.execute(addData, RemotingActionType.DELETE);
        }

        private void execute(List<T> data, RemotingActionType remotingActionType) {
            if (CollectionUtils.isEmpty(data)) {
                return;
            }
            data.forEach((value) -> {
                this.execution(value, remotingActionType);
            });
        }

        @SuppressWarnings("unchecked")
        private Object execution(T object, RemotingActionType remotingActionType) {
            RemotingServiceMethodMapperWrapper methodMapper = wrapper.actionMap.get(remotingActionType);
            Object arg = null;
            try {
                if (Objects.nonNull(methodMapper.parameterTypes)) {
                    AbstractGlobal2Request<Object> request =
                        (AbstractGlobal2Request<Object>) methodMapper.parameterTypes.newInstance();
                    request.setMetaData(object);
                    arg = request;
                }
                GlobalResult<T> result = (GlobalResult<T>) methodMapper.remotingServiceMethod.invoke(execution, arg);
                if (Objects.isNull(result)) {
                    log.error(" result is null, service is {} action is {} , arg is {}", wrapper.remotingServiceType.getSimpleName(),
                        remotingActionType, object);
                    if (Objects.equals(RemotingActionType.QUEUE_ALL, remotingActionType)) {
                        return Collections.EMPTY_LIST;
                    }
                    return null;
                }
                return result.getData();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public List<T> getData() {
            return (List<T>) this.execution(null, RemotingActionType.QUEUE_ALL);

        }
    }


    static class RemotingServiceMetadataWrapper {

        private RemotingServiceMapper remotingServiceMapper;

        private ClusterType clusterType;

        private Class<?> targetClass;

        /**
         * 通过这个找到
         */
        private Class<?> remotingServiceType;

        private Map<RemotingActionType, RemotingServiceMethodMapperWrapper> actionMap;
    }

    static class RemotingServiceMethodMapperWrapper {

        private RemotingServiceMethodMapper remotingServiceMethodMapper;

        private Method remotingServiceMethod;

        private Method targetMethod;

        private String targetMethodName;

        private Class<?> parameterTypes;


    }

}
