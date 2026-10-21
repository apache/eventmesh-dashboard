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
import org.apache.eventmesh.dashboard.common.enums.SyncStatus;
import org.apache.eventmesh.dashboard.common.model.base.BaseClusterIdBase;
import org.apache.eventmesh.dashboard.common.model.base.BaseRuntimeIdBase;
import org.apache.eventmesh.dashboard.common.model.base.BaseSyncBase;
import org.apache.eventmesh.dashboard.common.model.metadata.ClusterMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;
import org.apache.eventmesh.dashboard.common.model.remoting.AbstractGlobal2Request;
import org.apache.eventmesh.dashboard.common.model.remoting.GlobalResult;
import org.apache.eventmesh.dashboard.common.model.remoting.RemotingActionType;
import org.apache.eventmesh.dashboard.common.util.ClasspathScanner;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKManage;
import org.apache.eventmesh.dashboard.core.metadata.DataMetadataHandler;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;



@Slf4j
public class Remoting2Manage {

    private static final Remoting2Manage INSTANCE = new Remoting2Manage();

    private static final Map<ClusterType, Map<Class<?>, RemotingServiceMetadataWrapper>> REMOTING_SERVICE_METADATA_WRAPPER_MAP = new HashMap<>();

    private static final Map<Class<?>, Map<RemotingActionType, RemotingServiceMethodMapperWrapper>> CLASS_METHOD_MAPPER = new HashMap<>();

    public static Remoting2Manage getInstance() {
        return INSTANCE;
    }

    /**
     * 识别 AbstractGlobal2Request 行为
     *
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
        for (ClusterType clusterType : remotingServiceMapper.clusterType()) {
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
                    remotingServiceMethodMapperWrapper.notResult = Objects.equals(method.getReturnType(), Void.class)
                        || Objects.equals(method.getReturnType(), Void.TYPE);
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

    static {
        ClasspathScanner classpathScanner = ClasspathScanner.builder().base(Remoting2Manage.class).subPath("/**").build();
        try {
            classpathScanner.getClazz().forEach(Remoting2Manage::registerService);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private final List<RemotingServiceHandler> remotingServiceHandlerList = new ArrayList<>();

    private final List<RemotingResultHook> remotingResultHookList = new ArrayList<>();

    private Remoting2Manage() {
    }


    public void registerHook(RemotingResultHook remotingResultHook) {
        this.remotingResultHookList.add(remotingResultHook);
    }

    public void registerServiceHandler(RemotingServiceHandler remotingServiceHandler) {
        this.remotingServiceHandlerList.add(remotingServiceHandler);
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
        if (!this.remotingServiceHandlerList.isEmpty()) {
            this.remotingServiceHandlerList.forEach(remotingServiceHandler -> {
                remotingServiceHandler.serviceInit(proxyObject);
            });
        }
        remotingService.execution = proxyObject;
        remotingService.wrapper = remotingServiceMetadataWrapper;
        remotingService.baseSyncBase = baseSyncBase;
        if (baseSyncBase instanceof ClusterMetadata) {
            remotingService.clusterId = baseSyncBase.getId();
        } else {
            remotingService.clusterId = ((RuntimeMetadata) baseSyncBase).getClusterId();
            remotingService.runtimeId = baseSyncBase.getId();
        }
        return remotingService;
    }

    @SuppressWarnings("unchecked")
    public <T> T createRemotingService(Class<?> clazz, BaseSyncBase baseSyncBase) {
        RemotingService<T> remotingService = (RemotingService<T>) this.createDataMetadataHandler(clazz, baseSyncBase);
        return (T) remotingService.getExecution();
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

        private boolean notResult;


    }

    /**
     * 这个类，只支持 DataMetadataHandler 通道调用
     */
    @SuppressWarnings("unchecked")
    public class RemotingService<T> implements DataMetadataHandler<T> {

        private final AtomicLong actionLong = new AtomicLong(1);
        private RemotingServiceMetadataWrapper wrapper;
        @Getter
        private Object execution;
        private BaseSyncBase baseSyncBase;
        private Long clusterId;
        private Long runtimeId;

        private final Map<T, Set<RemotingActionType>> completedWrites = new IdentityHashMap<>();
        private final Map<T, Map<RemotingActionType, GlobalResult<T>>> confirmedWrites = new IdentityHashMap<>();

        private AtomicBoolean isAll = new AtomicBoolean(false);

        @Override
        public void handleAll(Collection<T> allData, List<T> addData, List<T> updateData, List<T> deleteData) {
            Long index = actionLong.incrementAndGet();
            if (CollectionUtils.isNotEmpty(allData)) {
                if (isAll.compareAndSet(false, true)) {
                    List<T> list = new ArrayList<>();
                    allData.forEach(value -> {
                        BaseRuntimeIdBase baseRuntimeIdBase = (BaseRuntimeIdBase) value;
                        if (baseRuntimeIdBase.getSyncStatus() == SyncStatus.ING) {
                            list.add(value);
                        }
                    });
                    for (RemotingResultHook hook : remotingResultHookList) {
                        hook.success(RemotingActionType.EXISTENCE, (List<BaseRuntimeIdBase>) list);
                    }
                }

            }
            this.execute(addData, RemotingActionType.ADD, index);
            this.execute(updateData, RemotingActionType.UPDATE, index);
            this.execute(deleteData, RemotingActionType.DELETE, index);
            this.completedWrites.clear();
            this.confirmedWrites.clear();
        }

        @Override
        public List<T> getData() {
            return (List<T>) this.execution(null, RemotingActionType.QUEUE_ALL, actionLong.incrementAndGet());

        }

        private void execute(List<T> data, RemotingActionType remotingActionType, Long index) {
            if (CollectionUtils.isEmpty(data)) {
                return;
            }
            data.forEach((value) -> {
                Set<RemotingActionType> completed = this.completedWrites.computeIfAbsent(value,
                    key -> EnumSet.noneOf(RemotingActionType.class));
                if (!completed.contains(remotingActionType)) {
                    this.execution(value, remotingActionType, index);
                    completed.add(remotingActionType);
                }
            });
        }

        private Object execution(T object, RemotingActionType remotingActionType, Long index) {
            RemotingServiceMethodMapperWrapper methodMapper = wrapper.actionMap.get(remotingActionType);
            Object arg;
            BaseRuntimeIdBase baseRuntimeIdBase = null;
            GlobalResult<T> result = null;
            Exception exception = null;
            try {
                baseRuntimeIdBase = (BaseRuntimeIdBase) object;
                arg = this.buildRequest(methodMapper, object);
                if (log.isTraceEnabled()) {
                    log.trace(
                        "#remoting manage , index {} Request metadata is {} method is {}  action is {} clusterId is{} runtime is {},"
                        + " id is {} Unique is {} \n data is {}",
                        index,
                        wrapper.remotingServiceType.getSimpleName(),
                        Objects.isNull(methodMapper) ? null : methodMapper.targetMethodName,
                        remotingActionType,
                        this.clusterId,
                        this.runtimeId,
                        Objects.isNull(baseRuntimeIdBase) ? null : baseRuntimeIdBase.getId(),
                        Objects.isNull(baseRuntimeIdBase) ? null : baseRuntimeIdBase.nodeUnique(),
                        arg);
                }
                Map<RemotingActionType, GlobalResult<T>> confirmed = this.confirmedWrites.get(object);
                result = Objects.nonNull(confirmed) && confirmed.containsKey(remotingActionType)
                    ? confirmed.get(remotingActionType) : this.invoke(methodMapper, arg);

                if (log.isTraceEnabled()) {
                    log.trace(
                        """
                        #remoting manage ,result  {} index {} Response metadata is {} method is {}  action is {} clusterId is{} runtime is {},
                         id is {} Unique is {} \n Result is {}
                        """,
                        Objects.isNull(result) ? "success" : !Objects.equals(result.getCode(), 200) ? "error" : "success",
                        index,
                        wrapper.remotingServiceType.getSimpleName(),
                        Objects.isNull(methodMapper) ? null : methodMapper.targetMethodName,
                        remotingActionType,
                        this.clusterId,
                        this.runtimeId,
                        Objects.isNull(baseRuntimeIdBase) ? null : baseRuntimeIdBase.getId(),
                        Objects.isNull(baseRuntimeIdBase) ? null : baseRuntimeIdBase.nodeUnique(),
                        result);
                }
            } catch (Exception e) {
                exception = e;
                if (e instanceof InterruptedException || e.getCause() instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                if (log.isTraceEnabled()) {
                    log.trace(
                        "#remoting manage , result error  index {} Response metadata is {} method is {}  action is {} clusterId is{} "
                        + " runtime is {}, id is {} Unique is {} \n Result is {}",
                        index,
                        wrapper.remotingServiceType.getSimpleName(),
                        Objects.isNull(methodMapper) ? null : methodMapper.targetMethodName,
                        remotingActionType,
                        this.clusterId,
                        this.runtimeId,
                        Objects.isNull(baseRuntimeIdBase) ? null : baseRuntimeIdBase.getId(),
                        Objects.isNull(baseRuntimeIdBase) ? null : baseRuntimeIdBase.nodeUnique(),
                        result);
                }
                log.error(e.getMessage(), e);
            }
            boolean failed = Objects.nonNull(exception) || (Objects.nonNull(result) && !Objects.equals(result.getCode(), 200))
                || (Objects.isNull(result) && (Objects.isNull(methodMapper) || !methodMapper.notResult));
            if (!Objects.equals(RemotingActionType.QUEUE_ALL, remotingActionType)) {
                if (failed) {
                    for (RemotingResultHook hook : remotingResultHookList) {
                        hook.fail(remotingActionType, baseRuntimeIdBase, (GlobalResult<Object>) result, exception);
                    }
                    throw new IllegalStateException("Remote metadata operation failed: " + remotingActionType
                        + ", result=" + result, Objects.nonNull(exception) ? exception : Objects.isNull(result) ? null : result.getThrowable());
                }
                // A reporting failure must retry reporting, not repeat an already confirmed Broker mutation.
                this.confirmedWrites.computeIfAbsent(object, key -> new EnumMap<>(RemotingActionType.class)).put(remotingActionType, result);
                for (RemotingResultHook hook : remotingResultHookList) {
                    hook.success(remotingActionType, baseRuntimeIdBase, (GlobalResult<Object>) result);
                }
                if (Objects.isNull(result) && methodMapper.notResult) {
                    return null;
                }
            }
            if (Objects.nonNull(exception) || Objects.isNull(result) || !Objects.equals(result.getCode(), 200)) {
                throw new IllegalStateException("Remote metadata operation failed: " + remotingActionType
                    + ", result=" + result, Objects.nonNull(exception) ? exception : Objects.isNull(result) ? null : result.getThrowable());
            }
            if (Objects.equals(remotingActionType, RemotingActionType.QUEUE_ALL) && !(result.getData() instanceof List)) {
                throw new IllegalStateException("Remote metadata query must return a list");
            }
            return result.getData();
        }

        @SuppressWarnings({"unchecked", "deprecation"})
        private Object buildRequest(RemotingServiceMethodMapperWrapper methodMapper, T object) throws InstantiationException, IllegalAccessException {
            if (Objects.nonNull(methodMapper.parameterTypes)) {
                AbstractGlobal2Request<Object> request =
                    (AbstractGlobal2Request<Object>) methodMapper.parameterTypes.newInstance();
                request.setMetaData(object);
                return request;
            }
            return object;
        }

        private GlobalResult<T> invoke(RemotingServiceMethodMapperWrapper methodMapper, Object arg)
            throws InvocationTargetException, IllegalAccessException {
            if (Objects.isNull(arg)) {
                return (GlobalResult<T>) methodMapper.remotingServiceMethod.invoke(execution);
            } else {
                return (GlobalResult<T>) methodMapper.remotingServiceMethod.invoke(execution, arg);
            }
        }

    }

}
