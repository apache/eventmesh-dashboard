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

import org.apache.eventmesh.dashboard.console.entity.base.BaseRuntimeIdEntity;
import org.apache.eventmesh.dashboard.console.mapper.SyncDataHandlerMapper;
import org.apache.eventmesh.dashboard.core.metadata.DataMetadataHandler;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public abstract class AbstractDBDataMetadataHandler<T extends BaseRuntimeIdEntity> implements DataMetadataHandler<T>, ApplicationContextAware {

    private ApplicationContext applicationContext;

    private T baseRuntimeIdBase;

    private SyncDataHandlerMapper<T> syncDataHandlerMapper;

    /**
     * TODO
     *      初始化时间有问题,
     *      1. 是否需要 check 两端数据，
     *          1. 如果 check 第一次全量加载
     *          2. 如果 不 check ， 不需要全量加载
     */
    @PostConstruct
    public void init() {
        Type superClass = getClass().getGenericSuperclass();
        Type type = ((ParameterizedType) superClass).getActualTypeArguments()[0];
        Class<?> mapperClass = (Class<?>) ((ParameterizedType) superClass).getActualTypeArguments()[1];
        try {
            this.baseRuntimeIdBase = (T) ((Class<?>) type).newInstance();
            LocalDateTime date = LocalDateTime.of(2000, 0, 0, 0, 0, 0, 0);
            baseRuntimeIdBase.setUpdateTime(date);
            this.syncDataHandlerMapper = (SyncDataHandlerMapper<T>) this.applicationContext.getAutowireCapableBeanFactory().getBean(mapperClass);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public T getEntity() {
        return this.baseRuntimeIdBase;
    }

    public void handleAll(List<T> addData, List<T> updateData, List<T> deleteData) {

    }

    public List<T> getData() {
        LocalDateTime date = LocalDateTime.now();
        try {
            this.syncDataHandlerMapper.syncGet(this.baseRuntimeIdBase);
            return this.doGetData();
        } finally {
            this.baseRuntimeIdBase.setUpdateTime(date);
        }
    }

    abstract List<T> doGetData();


}
