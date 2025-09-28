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
import org.apache.eventmesh.dashboard.common.model.base.BaseRuntimeIdBase;
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;


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

    /**
     *  识别 AbstractGlobal2Request<T> 行为
     * @param clazz 远程服务
     */
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
        for (ClusterType clusterType : ClusterType.values()) {
            final Map<Class<?>, RemotingServiceMetadataWrapper> remotingServiceMetadataWrapperMap =
                REMOTING_SERVICE_METADATA_WRAPPER_MAP.computeIfAbsent(clusterType, k -> new ConcurrentHashMap<>());

            RemotingServiceMetadataWrapper remotingServiceMetadataWrapper = new RemotingServiceMetadataWrapper();
            remotingServiceMetadataWrapper.clusterType = clusterType;
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

    }


    public static Remoting2Manage getInstance() {
        return INSTANCE;
    }

    private Remoting2Manage() {
    }

    /**
     * @param clazz        Remoting Service
     * @param baseSyncBase baseSyncBase
     * @return RemotingService
     */
    public DataMetadataHandler<BaseClusterIdBase> createDataMetadataHandler(Class<?> clazz, BaseSyncBase baseSyncBase) {
        Map<Class<?>, RemotingServiceMetadataWrapper> remotingServiceMetadataWrapperMap =
            REMOTING_SERVICE_METADATA_WRAPPER_MAP.get(baseSyncBase.getClusterType());
        if (Objects.isNull(remotingServiceMetadataWrapperMap)) {
            throw new RuntimeException("clusterType " + baseSyncBase.getClusterType().toString() + " ,can not find service");
        }
        RemotingServiceMetadataWrapper remotingServiceMetadataWrapper = remotingServiceMetadataWrapperMap.get(clazz);
        AbstractRemotingService<BaseClusterIdBase> proxyObject =
            SDKManage.getInstance().createAbstractClientInfo(remotingServiceMetadataWrapper.remotingServiceType, baseSyncBase);
        RemotingService<BaseClusterIdBase> remotingService = new RemotingService<>();
        remotingService.execution = proxyObject;
        remotingService.wrapper = remotingServiceMetadataWrapper;
        return remotingService;
    }


    @SuppressWarnings("unchecked")
    @Slf4j
    public static class RemotingService<T> implements DataMetadataHandler<T> {

        private RemotingServiceMetadataWrapper wrapper;

        private Object execution;

        @Override
        public void handleAll(Collection<T> allData, List<T> addData, List<T> updateData, List<T> deleteData) {
            this.execute(addData, RemotingActionType.ADD);
            this.execute(updateData, RemotingActionType.UPDATE);
            this.execute(deleteData, RemotingActionType.DELETE);
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
            BaseRuntimeIdBase baseRuntimeIdBase = (BaseRuntimeIdBase) object;
            boolean error = false;
            String errorMessage = null;
            try {
                if (Objects.nonNull(methodMapper.parameterTypes)) {
                    AbstractGlobal2Request<Object> request =
                        (AbstractGlobal2Request<Object>) methodMapper.parameterTypes.newInstance();
                    request.setMetaData(object);
                    arg = request;
                }
                GlobalResult<T> result;
                if (Objects.isNull(arg)) {
                    result = (GlobalResult<T>) methodMapper.remotingServiceMethod.invoke(execution);
                } else {
                    result = (GlobalResult<T>) methodMapper.remotingServiceMethod.invoke(execution, arg);
                }
                if (Objects.isNull(result)) {
//                    log.error(" result is null, service is {} action is {} , method name is {} arg is {}", wrapper.remotingServiceType.getSimpleName(),
//                        remotingActionType,methodMapper.remotingServiceMethod.getName(), object);
                    if (Objects.equals(RemotingActionType.QUEUE_ALL, remotingActionType)) {
                        return Collections.EMPTY_LIST;
                    }
                    return null;
                }
                if (result.getCode() != 200) {
                    error = true;
                    errorMessage = result.getMessage();
                }
                return result.getData();
            } catch (Exception e) {
                error = true;
                errorMessage = e.getMessage();
                throw new RuntimeException(e);
            } finally {
                if(Objects.equals(RemotingActionType.QUEUE_ALL, remotingActionType)) {

                }else{
                    log.info("$sync execute service {} result {} errorMessage {} action {} cluster {} runtime {} unique",
                        wrapper.remotingServiceType.getSimpleName(),
                        error ? "error" : "success", errorMessage,
                        baseRuntimeIdBase.getClusterId(), baseRuntimeIdBase.getRuntimeId(), baseRuntimeIdBase.getUnique());

                }
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
