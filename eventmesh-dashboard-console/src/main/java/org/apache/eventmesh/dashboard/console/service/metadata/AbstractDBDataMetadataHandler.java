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


package org.apache.eventmesh.dashboard.console.service.metadata;

import org.apache.eventmesh.dashboard.common.model.base.BaseSyncBase;
import org.apache.eventmesh.dashboard.common.model.remoting.Global2Request;
import org.apache.eventmesh.dashboard.console.entity.base.BaseIdEntity;
import org.apache.eventmesh.dashboard.console.entity.base.BaseRuntimeIdEntity;
import org.apache.eventmesh.dashboard.console.mapper.SyncDataHandlerMapper;
import org.apache.eventmesh.dashboard.core.metadata.DataMetadataHandler;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.ibatis.binding.MapperProxy;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractDBDataMetadataHandler<T extends BaseIdEntity> implements DataMetadataHandler<T>, ApplicationContextAware {

    private static final Map<Type, Object> CLASS_SYNC_DATA_HANDLER_MAPPER_MAP = new HashMap<>();
    protected SyncDataHandlerMapper<T> syncDataHandlerMapper;
    private final List<T> dataList = new ArrayList<>(1);
    private AbstractApplicationContext applicationContext;
    private T baseRuntimeIdBase;

    /**
     * TODO
     *      初始化时间有问题,
     *      1. 是否需要 check 两端数据，
     *          1. 如果 check 第一次全量加载
     *          2. 如果 不 check ， 不需要全量加载
     */
    @SuppressWarnings({"unchecked", "AliDeprecation", "deprecation"})
    @PostConstruct
    public void init() {
        Type superClass = getClass().getGenericSuperclass();
        Type type = ((ParameterizedType) superClass).getActualTypeArguments()[0];
        try {
            this.baseRuntimeIdBase = (T) ((Class<?>) type).newInstance();
            LocalDateTime date = LocalDateTime.of(2000, 1, 1, 0, 0, 0, 0);
            baseRuntimeIdBase.setUpdateTime(date);
            if (CLASS_SYNC_DATA_HANDLER_MAPPER_MAP.isEmpty()) {
                this.applicationContext.getBeansOfType(SyncDataHandlerMapper.class).values().forEach(syncDataHandlerMapper -> {
                    try {
                        Object proxyObject = Proxy.getInvocationHandler(syncDataHandlerMapper);
                        Class<?> mapperInterface = (Class<?>) FieldUtils.readField(proxyObject, "mapperInterface", true);
                        for (Type mapperType : mapperInterface.getGenericInterfaces()) {
                            Class<?> mapperClass = (Class<?>) ((ParameterizedType) mapperType).getRawType();
                            if (Objects.equals(mapperClass, SyncDataHandlerMapper.class)) {
                                Type syncType = ((ParameterizedType) mapperType).getActualTypeArguments()[0];
                                CLASS_SYNC_DATA_HANDLER_MAPPER_MAP.put(syncType, syncDataHandlerMapper);
                                log.info("db sync mapper , syncType:{} , syncMapper", syncDataHandlerMapper.getClass().getSimpleName());
                            }
                        }
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
            this.syncDataHandlerMapper = (SyncDataHandlerMapper<T>) CLASS_SYNC_DATA_HANDLER_MAPPER_MAP.get(type);
            if (Objects.isNull(syncDataHandlerMapper)) {
                log.error("syncDataHandlerMapper is null, type is {}", type);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void setApplicationContext(@Nullable ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = (AbstractApplicationContext) applicationContext;
    }

    public T getEntity() {
        return this.baseRuntimeIdBase;
    }

    @Override
    public void handleAll(Collection<T> allData, List<T> addData, List<T> updateData, List<T> deleteData) {
        if (CollectionUtils.isNotEmpty(addData)) {
            this.syncDataHandlerMapper.syncInsert(addData);
        }
        if (CollectionUtils.isNotEmpty(updateData)) {
            this.syncDataHandlerMapper.syncUpdate(updateData);
        }
        if (CollectionUtils.isNotEmpty(deleteData)) {
            this.syncDataHandlerMapper.syncDelete(deleteData);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<T> getData() {
        // DDL timestamps have second precision; overlap the boundary so same-second changes are replayed.
        LocalDateTime date = LocalDateTime.now().withNano(0);
        List<T> list = this.doGetData();
        if (this.dataList == list) {
            list = this.syncDataHandlerMapper.syncGet(this.baseRuntimeIdBase);
        }
        this.baseRuntimeIdBase.setUpdateTime(date);
        return list;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<T> getData(Global2Request global2Request) {
        if (Objects.isNull(global2Request)) {
            return this.syncDataHandlerMapper.syncGet(this.baseRuntimeIdBase);
        }
        Class<?> clazz = null;
        try {
            MapperProxy<Object> mapperProxy = (MapperProxy<Object>) Proxy.getInvocationHandler(this.syncDataHandlerMapper);
            Class<?> mapperInterface = (Class<?>) FieldUtils.readField(mapperProxy, "mapperInterface", true);
            Type[] genericInterfaces = mapperInterface.getGenericInterfaces();
            for (Type type : genericInterfaces) {
                if (type instanceof ParameterizedType) {
                    ParameterizedType parameterizedType = (ParameterizedType) type;

                    Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                    Type actualType = actualTypeArguments[0];
                    clazz = (Class<?>) actualType;
                }
            }
            BaseRuntimeIdEntity baseClusterIdEntity = (BaseRuntimeIdEntity) clazz.newInstance();
            baseClusterIdEntity.setClusterId(global2Request.getClusterId());
            baseClusterIdEntity.setRuntimeId(global2Request.getRuntimeId());
            return this.syncDataHandlerMapper.syncGet((T) baseClusterIdEntity);
        } catch (IllegalAccessException | InstantiationException e) {
            throw new RuntimeException(e);
        }

    }

    public List<T> getClusterIdOrRuntime(BaseSyncBase baseSyncBase) {
        return this.syncDataHandlerMapper.syncGet(this.baseRuntimeIdBase);
    }

    List<T> doGetData() {
        return this.dataList;
    }

}
