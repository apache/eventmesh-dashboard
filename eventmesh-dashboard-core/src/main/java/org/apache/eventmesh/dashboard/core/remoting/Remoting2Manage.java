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

import org.apache.eventmesh.dashboard.common.annotation.RemotingServiceMetadata;
import org.apache.eventmesh.dashboard.common.annotation.RemotingServiceMethodMapper;
import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.enums.RemotingType;
import org.apache.eventmesh.dashboard.common.model.base.BaseSyncBase;
import org.apache.eventmesh.dashboard.common.model.remoting.GlobalResult;
import org.apache.eventmesh.dashboard.common.util.ClasspathScanner;
import org.apache.eventmesh.dashboard.core.function.SDK.ClientWrapper;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKManage;

import org.apache.commons.lang3.ArrayUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;


public class Remoting2Manage {

    private static final Map<Class<?>, RemotingServiceMetadataWrapper> mapper = new HashMap<>();

    private final Map<RemotingType, Map<Class<?>, Class<?>>> remotingServiceClasses = new HashMap<>();


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
        Class<?> interfaces = clazz.getInterfaces()[0];
        RemotingServiceMetadata remotingServiceMetadata = interfaces.getAnnotation(RemotingServiceMetadata.class);
        if (Objects.isNull(remotingServiceMetadata)) {
            return;
        }
        RemotingServiceMetadataWrapper remotingServiceMetadataWrapper = new RemotingServiceMetadataWrapper();
        remotingServiceMetadataWrapper.remotingServiceType = interfaces;
        remotingServiceMetadataWrapper.remotingServiceMetadata = remotingServiceMetadata;
        Method[] methods = interfaces.getMethods();
        for (Method method : methods) {
            RemotingServiceMethodMapper[] remotingServiceMethodMappers = interfaces.getAnnotationsByType(RemotingServiceMethodMapper.class);
            if (ArrayUtils.isEmpty(remotingServiceMethodMappers)) {
                continue;
            }
            remotingServiceMetadataWrapper.remotingServiceMethodMapperWrapperMap = new HashMap<>();

            for (RemotingServiceMethodMapper serviceMethodMapper : remotingServiceMethodMappers) {
                RemotingServiceMethodMapperWrapper remotingServiceMethodMapperWrapper = new RemotingServiceMethodMapperWrapper();
                remotingServiceMethodMapperWrapper.serviceMethod = method;
                remotingServiceMethodMapperWrapper.remotingServiceMethodMapper = serviceMethodMapper;
                remotingServiceMetadataWrapper.remotingServiceMethodMapperWrapperMap.computeIfAbsent(serviceMethodMapper.mapperClass(),
                    k -> new HashMap<>()).put(serviceMethodMapper.methodName(), remotingServiceMethodMapperWrapper);

            }
        }
        mapper.put(interfaces, remotingServiceMetadataWrapper);

    }


    public Object createProxy(Class<?> clazz, BaseSyncBase baseSyncBase, ClusterType clusterType) {

        try {
            Map<Class<?>, Class<?>> serviceList = this.remotingServiceClasses.get(clusterType);
            Class<?> executionClazz = serviceList.get(clazz);
            AbstractRemotingService<Object> proxyObject = (AbstractRemotingService<Object>) executionClazz.newInstance();
            ClientWrapper clientWrapper = SDKManage.getInstance().getClientWrapper(baseSyncBase.getUnique());
            proxyObject.setClientWrapper(clientWrapper);
            RemotingServiceHandler remotingServiceHandler = new RemotingServiceHandler();
            remotingServiceHandler.execution = baseSyncBase;
            remotingServiceHandler.proxyObject = proxyObject;
            remotingServiceHandler.remotingResultType = RemotingResultType.ERROR_THROW_EXCEPTION;
            Object object = Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[] {clazz}, remotingServiceHandler);

            return object;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * TODO 怎么做？？？？？？？？？？？？
     *  不能调用批量？
     *  这个 动态代理执行类，只能为了辅助大量重复调用且解决需要处理的
     */
    public class RemotingServiceHandler implements InvocationHandler {


        private Object execution;

        private boolean isHandler = false;

        private Object proxyObject;

        private Map<Method, MethodWrapper> methodWrapperMap = new ConcurrentHashMap<>();

        private RemotingResultType remotingResultType;

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

            MethodWrapper methodWrapper = this.getMethodWrapper(method);
            if (this.isExecuteOne(methodWrapper, args)) {
                return this.executeOne(methodWrapper, args);
            }
            // 如果 client 支持 batch 怎么办
            List<Object> list = (List) args[0];

            return null;
        }

        private boolean isExecuteOne(MethodWrapper methodWrapper, Object[] args) {
            return args == null || args[0] == null || methodWrapper.parameterTypes != null || Objects.equals(methodWrapper.parameterTypes,
                List.class);
        }


        private Object executeOne(MethodWrapper methodWrapper, Object[] args) {
            Exception ex;
            try {
                GlobalResult<Object> result = (GlobalResult<Object>) methodWrapper.tagerMethod.invoke(proxyObject, args);
                if (result.getCode() == 200) {
                    return this.result(result, methodWrapper);
                }
                if (remotingResultType.equals(RemotingResultType.ERROR_THROW_EXCEPTION)) {
                    return this.result(methodWrapper);
                }
                ex = new RuntimeException(result.getMessage());
            } catch (Exception e) {
                //
                ex = e;
            }
            throw new RuntimeException(ex);
        }


        private MethodWrapper getMethodWrapper(Method method) {
            return methodWrapperMap.computeIfAbsent(method, k -> {
                for (Method method1 : proxyObject.getClass().getMethods()) {
                    if (method1.getName().equals(method.getName())) {
                        //
                    }
                }
                MethodWrapper newMethodWrapper = new MethodWrapper();
                newMethodWrapper.parameterTypes = method.getParameterTypes()[0];
                newMethodWrapper.resultClass = method.getReturnType();
                return newMethodWrapper;
            });
        }

        private Object result(GlobalResult<Object> result, MethodWrapper methodWrapper) {
            return Objects.nonNull(methodWrapper.resultClass) ? result.getData() : null;
        }

        private Object result(MethodWrapper methodWrapper) throws InstantiationException, IllegalAccessException {
            return Objects.nonNull(methodWrapper.resultClass) ? methodWrapper.resultClass.newInstance() : null;
        }

    }

    static class RemotingServiceMetadataWrapper {

        private RemotingServiceMetadata remotingServiceMetadata;

        /**
         * 通过这个找到
         */
        private Class<?> remotingServiceType;

        private Map<Class<?>, Map<String/** method name **/, RemotingServiceMethodMapperWrapper>> remotingServiceMethodMapperWrapperMap =
            new HashMap<>();
    }

    static class RemotingServiceMethodMapperWrapper {

        private RemotingServiceMethodMapper remotingServiceMethodMapper;

        private Method serviceMethod;

    }


    static class MethodWrapper {

        private Class<?> parameterTypes;

        private Method tagerMethod;

        private Class<?> resultClass;
    }
}
