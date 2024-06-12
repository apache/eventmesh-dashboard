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

import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType;
import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.enums.RemotingType;
import org.apache.eventmesh.dashboard.common.model.metadata.ClusterMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.ClusterRelationshipMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;
import org.apache.eventmesh.dashboard.common.model.remoting.GlobalRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.GlobalResult;
import org.apache.eventmesh.dashboard.common.model.remoting.RemotingAction;
import org.apache.eventmesh.dashboard.core.cluster.ClusterDO;
import org.apache.eventmesh.dashboard.core.cluster.ColonyDO;
import org.apache.eventmesh.dashboard.core.remoting.rocketmq.RocketMQAclRemotingService;
import org.apache.eventmesh.dashboard.core.remoting.rocketmq.RocketMQClientRemotingService;
import org.apache.eventmesh.dashboard.core.remoting.rocketmq.RocketMQConfigRemotingService;
import org.apache.eventmesh.dashboard.core.remoting.rocketmq.RocketMQGroupRemotingService;
import org.apache.eventmesh.dashboard.core.remoting.rocketmq.RocketMQOffsetRemotingService;
import org.apache.eventmesh.dashboard.core.remoting.rocketmq.RocketMQSubscriptionRemotingService;
import org.apache.eventmesh.dashboard.core.remoting.rocketmq.RocketMQTopicRemotingService;
import org.apache.eventmesh.dashboard.core.remoting.rocketmq.RocketMQUserRemotingService;
import org.apache.eventmesh.dashboard.service.remoting.RemotingIntegrationService;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.Data;
import lombok.Getter;


/**
 *
 */
public class RemotingManager {


    private final Map<RemotingType, List<Class<?>>> remotingServiceClasses = new HashMap<>();

    /**
     * key clusterId
     */
    private final Map<Long, RemotingWrapper> remotingWrapperMap = new ConcurrentHashMap<>();

    @Getter
    private final Object proxyObject;


    /**
     * Long key  is clusterId
     */
    private final Map<Long, ColonyDO> colonyDOMap = new HashMap<>();

    private final AtomicBoolean loading = new AtomicBoolean(true);


    {
        for (RemotingType remotingType : RemotingType.values()) {
            remotingServiceClasses.put(remotingType, new ArrayList<>());
        }
        this.registerService(RemotingType.ROCKETMQ, RocketMQAclRemotingService.class, RocketMQConfigRemotingService.class,
            RocketMQClientRemotingService.class, RocketMQGroupRemotingService.class, RocketMQOffsetRemotingService.class,
            RocketMQSubscriptionRemotingService.class, RocketMQTopicRemotingService.class, RocketMQUserRemotingService.class);

        this.registerService(RemotingType.EVENT_MESH_RUNTIME, RocketMQAclRemotingService.class, RocketMQConfigRemotingService.class,
            RocketMQClientRemotingService.class, RocketMQGroupRemotingService.class, RocketMQOffsetRemotingService.class,
            RocketMQSubscriptionRemotingService.class, RocketMQTopicRemotingService.class, RocketMQUserRemotingService.class);

        RemotingServiceHandler remotingServiceHandler = new RemotingServiceHandler();
        Class<?>[] clazzList = new Class[] {RemotingIntegrationService.class};
        proxyObject = Proxy.newProxyInstance(this.getClass().getClassLoader(), clazzList, remotingServiceHandler);
    }

    public void registerService(RemotingType remotingType, Class<?>... clazzs) {
        List<Class<?>> serviceList = this.remotingServiceClasses.get(remotingType);
        Collections.addAll(serviceList, clazzs);
    }


    public void registerColony(ColonyDO colonyDO) throws Exception {
        if (loading.get()) {
            return;
        }

        if (colonyDO.getClusterDO().getClusterInfo().getClusterType().isMainCluster()) {
            ClusterType clusterType = colonyDO.getClusterDO().getClusterInfo().getClusterType();
            RemotingType remotingType = clusterType.getRemotingType();
            List<Class<?>> remotingServersClassList = remotingServiceClasses.get(remotingType);
            Map<Class<?>, Object> remotingServersMap = new HashMap<>();
            for (Class<?> clazz : remotingServersClassList) {
                AbstractRemotingService abstractRemotingService = (AbstractRemotingService) clazz.newInstance();
                abstractRemotingService.setColonyDO(colonyDO);
                abstractRemotingService.init();
                remotingServersMap.put(clazz.getInterfaces()[0], abstractRemotingService);
            }
            RemotingWrapper remotingWrapper = new RemotingWrapper();
            remotingWrapper.setColonyDO(colonyDO);
            remotingWrapper.setObject(remotingServersMap);
            this.remotingWrapperMap.put(colonyDO.getClusterId(), remotingWrapper);
        } else {
            this.updateColony(colonyDO);
        }

    }

    public void updateColony(ColonyDO colonyDO) {
        RemotingWrapper remotingWrapper = this.getMainRemotingWrapper(colonyDO);
        /*
          There is a delay
         */
        if (Objects.isNull(remotingWrapper)) {
            return;
        }
        ColonyDO mainColonyDO = this.getMainColonyDO(colonyDO);
        for (Object object : remotingWrapper.getObject().values()) {
            AbstractRemotingService abstractRemotingService = (AbstractRemotingService) object;
            abstractRemotingService.setColonyDO(mainColonyDO);
            abstractRemotingService.update();
        }
    }

    public RemotingWrapper getMainRemotingWrapper(ColonyDO colonyDO) {
        Long clusterId =
            colonyDO.getClusterDO().getClusterInfo().getClusterType().isMainCluster() ? colonyDO.getClusterId() : colonyDO.getSuperiorId();
        if (Objects.isNull(clusterId)) {
            return null;
        }
        return remotingWrapperMap.get(clusterId);
    }

    public ColonyDO getMainColonyDO(ColonyDO colonyDO) {
        Long clusterId =
            colonyDO.getClusterDO().getClusterInfo().getClusterType().isMainCluster() ? colonyDO.getClusterId() : colonyDO.getSuperiorId();
        return colonyDOMap.get(clusterId);
    }

    public void unregister(ColonyDO colonyDO) {
        remotingWrapperMap.remove(colonyDO.getClusterId());
    }

    public void loadingCompleted() throws Exception {
        this.loading.set(false);
        for (ColonyDO colonyDO : colonyDOMap.values()) {
            if (colonyDO.getClusterDO().getClusterInfo().getClusterType().isMainCluster()) {
                this.registerColony(colonyDO);
            }
        }
    }


    /**
     * 解除完关系，才能删除
     *
     * @param clusterEntityList
     */
    public void cacheCluster(List<ClusterMetadata> clusterEntityList) {
        for (ClusterMetadata cluster : clusterEntityList) {
            Long clusterId = cluster.getId();
            if (cluster.getStatus() == 1) {
                ColonyDO colonyDO = colonyDOMap.remove(cluster.getClusterId());
                this.unregister(colonyDO);
                continue;
            }
            ColonyDO colonyDO = this.colonyDOMap.computeIfAbsent(clusterId, key -> {
                ColonyDO newColonyDO = new ColonyDO();
                ClusterDO newClusterDO = new ClusterDO();
                newColonyDO.setClusterDO(newClusterDO);
                return newColonyDO;
            });
            if (Objects.isNull(colonyDO.getClusterDO().getClusterInfo())) {
                colonyDO.getClusterDO().setClusterInfo(cluster);

                try {
                    this.registerColony(colonyDO);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

            } else {
                colonyDO.getClusterDO().setClusterInfo(cluster);
                this.updateColony(colonyDO);
            }
        }
    }

    public void cacheRuntime(List<RuntimeMetadata> runtimeMeatadataList) {
        for (RuntimeMetadata runtimeMetadata : runtimeMeatadataList) {
            ColonyDO colonyDO = this.colonyDOMap.get(runtimeMetadata.getClusterId());
            if (Objects.equals(runtimeMetadata.getStatus(), 1)) {
                colonyDO.getClusterDO().getRuntimeMap().put(runtimeMetadata.getId(), runtimeMetadata);
            } else {
                colonyDO.getClusterDO().getRuntimeMap().remove(runtimeMetadata.getId());
            }
        }
    }

    /**
     * 解除关系是解除关系，不是删除
     *
     * @param clusterRelationshipEntityList
     */
    public void cacheClusterRelationship(List<ClusterRelationshipMetadata> clusterRelationshipEntityList) {
        for (ClusterRelationshipMetadata clusterRelationshipEntity : clusterRelationshipEntityList) {
            ClusterType relationshipType = clusterRelationshipEntity.getRelationshipType();
            ColonyDO colonyDO = this.colonyDOMap.get(clusterRelationshipEntity.getClusterId());
            if (Objects.equals(relationshipType.getAssemblyNodeType(), ClusterType.META)) {
                this.relationship(colonyDO, colonyDO.getMetaColonyDOList(), clusterRelationshipEntity);
            } else if (Objects.equals(relationshipType.getAssemblyNodeType(), ClusterType.RUNTIME)) {
                this.relationship(colonyDO, colonyDO.getRuntimeColonyDOList(), clusterRelationshipEntity);
            } else if (Objects.equals(relationshipType.getAssemblyNodeType(), ClusterType.STORAGE)) {
                this.relationship(colonyDO, colonyDO.getStorageColonyDOList(), clusterRelationshipEntity);
            }
        }
    }

    private void relationship(ColonyDO colonyDO, Map<Long, ColonyDO> clusterDOList, ClusterRelationshipMetadata clusterRelationshipMetadata) {
        if (Objects.equals(clusterRelationshipMetadata.getStatus(), 2)) {
            clusterDOList.remove(clusterRelationshipMetadata.getRelationshipId());
        } else {
            ColonyDO relationshiCcolonyDO = this.colonyDOMap.get(clusterRelationshipMetadata.getRelationshipId());
            clusterDOList.put(clusterRelationshipMetadata.getRelationshipId(), relationshiCcolonyDO);
            relationshiCcolonyDO.setSuperiorId(colonyDO.getClusterId());
        }
        this.updateColony(colonyDO);
    }

    public List<RemotingWrapper> getEventMeshClusterDO(ClusterTrusteeshipType... clusterTrusteeshipType) {
        return this.filterer(ClusterType.EVENTMESH, clusterTrusteeshipType);
    }

    public List<RemotingWrapper> getMetaNacosClusterDO(ClusterTrusteeshipType... clusterTrusteeshipType) {
        return this.filterer(ClusterType.EVENTMESH_META_ETCD, clusterTrusteeshipType);
    }

    public List<RemotingWrapper> getMetaEtcdClusterDO(ClusterTrusteeshipType... clusterTrusteeshipType) {
        return this.filterer(ClusterType.EVENTMESH_META_NACOS, clusterTrusteeshipType);
    }

    public List<RemotingWrapper> getRocketMQClusterDO(ClusterTrusteeshipType... clusterTrusteeshipType) {
        return this.filterer(ClusterType.STORAGE_ROCKETMQ, clusterTrusteeshipType);
    }

    public List<RemotingWrapper> getStorageCluster(ClusterTrusteeshipType... clusterTrusteeshipType) {
        List<RemotingWrapper> list = new ArrayList<>();
        for (ClusterType clusterType : ClusterType.STORAGE_TYPES) {
            list.addAll(this.filterer(clusterType, clusterTrusteeshipType));
        }
        return list;
    }


    public boolean isClusterTrusteeshipType(Long clusterId, ClusterTrusteeshipType clusterTrusteeshipType) {
        ColonyDO colonyDO = this.colonyDOMap.get(clusterId);
        if (Objects.isNull(colonyDO)) {
            return false;
        }
        return Objects.equals(colonyDO.getClusterDO().getClusterInfo().getTrusteeshipType(), clusterTrusteeshipType);
    }


    private List<RemotingWrapper> filterer(ClusterType clusterType, ClusterTrusteeshipType... clusterTrusteeshipTypes) {
        Set<ClusterTrusteeshipType> clusterTrusteeshipType = new HashSet<>();
        clusterTrusteeshipType.addAll(Arrays.asList(clusterTrusteeshipTypes));
        List<RemotingWrapper> remotingWrapperList = new ArrayList<>();
        for (RemotingWrapper remotingWrapper : remotingWrapperMap.values()) {
            ClusterMetadata clusterMetadata = remotingWrapper.getColonyDO().getClusterDO().getClusterInfo();
            if (Objects.equals(clusterMetadata.getClusterType(), clusterType)) {
                if (clusterTrusteeshipType.contains(clusterMetadata.getTrusteeshipType())) {
                    remotingWrapperList.add(remotingWrapper);
                }
            }
        }
        return remotingWrapperList;
    }


    public <T> T request(RemotingRequestWrapper remotingRequestWrapper, List<RemotingWrapper> remotingWrapperList) {
        List<Object> resultData = new ArrayList<>();

        Class<?> clazz = remotingRequestWrapper.getClass();
        Type superclass = clazz.getGenericSuperclass();
        Class<GlobalRequest> globalRequestClass = null;
        Class<?> executeClass = null;
        if (superclass instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) superclass;
            Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
            executeClass = (Class<GlobalRequest>) actualTypeArguments[0];
            globalRequestClass = (Class<GlobalRequest>) actualTypeArguments[1];
        }
        RemotingRequestWrapper<Object, Object> remotingRequestWrapper1 = (RemotingRequestWrapper<Object, Object>) remotingRequestWrapper;
        for (RemotingWrapper remotingWrapper : remotingWrapperList) {
            try {
                GlobalRequest globalRequest = globalRequestClass.newInstance();
                globalRequest.setClusterId(remotingWrapper.getColonyDO().getClusterId());
                GlobalResult<Object> globalResult = remotingRequestWrapper1.request(globalRequest, executeClass);
                if (globalResult.getData() instanceof List) {
                    resultData.addAll((List<Object>) globalResult.getData());
                } else {
                    resultData.add(globalResult.getData());
                }
            } catch (Exception e) {
                //TODO  There should be no abnormal occurrence of InstantiationException, IllegalAccessException, Exception
                //
            }
        }
        return (T) resultData;

    }

    /**
     * @param <T>
     * @param <RE>
     */
    public interface RemotingRequestWrapper<T, RE> {

        GlobalResult request(T t, RE key);
    }

    @Data
    public static class RemotingWrapper {

        private ColonyDO colonyDO;

        private Map<Class<?>, Object> object = new HashMap<>();
    }


    public class RemotingServiceHandler implements InvocationHandler {

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

            GlobalRequest globalRequest = (GlobalRequest) args[0];
            Long clusterId = globalRequest.getClusterId();
            // ClusterDO
            RemotingWrapper remotingWrapper = remotingWrapperMap.get(clusterId);
            // 完整执行对象
            Class<?> declaringClass = method.getDeclaringClass();
            Object object = remotingWrapper.getObject().get(declaringClass);
            if (Objects.isNull(object)) {
                return null;
            }

            Method currentMethod = object.getClass().getMethod(method.getName(), method.getParameterTypes());
            RemotingAction annotations = currentMethod.getAnnotation(RemotingAction.class);
            if (Objects.nonNull(annotations)) {
                if (!annotations.support()) {
                    ColonyDO colonyDO = remotingWrapper.getColonyDO();
                    Map<Long, ColonyDO> colonyDOMap1 = colonyDO.getStorageColonyDOList();
                    for (ColonyDO c : colonyDOMap1.values()) {
                        RemotingWrapper newRemotingWrapper = remotingWrapperMap.get(c.getClusterId());
                        Object newObject = newRemotingWrapper.getObject().get(declaringClass);
                        return method.invoke(newObject, args);
                    }
                }
            }
            return method.invoke(object, args);

        }
    }
}
