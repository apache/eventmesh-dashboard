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

package org.apache.eventmesh.dashboard.console.service.function;

import org.apache.eventmesh.dashboard.console.entity.function.HealthCheckResultEntity;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.springframework.stereotype.Component;

@Component
public class HealthDataServiceMemoryStorage {

    private static final List<HealthCheckResultEntity> cache = new ArrayList<>();
    private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();


    public HealthCheckResultEntity insertHealthCheckResult(HealthCheckResultEntity healthCheckResultEntity) {
        lock.writeLock().lock();
        try {
            healthCheckResultEntity.setCreateTime(LocalDateTime.now());
            cache.add(healthCheckResultEntity);
            return healthCheckResultEntity;
        } finally {
            lock.writeLock().unlock();
        }
    }


    public void batchInsertHealthCheckResult(List<HealthCheckResultEntity> healthCheckResultEntityList) {
        lock.writeLock().lock();
        try {
            for (HealthCheckResultEntity entity : healthCheckResultEntityList) {
                entity.setCreateTime(LocalDateTime.now());
                cache.add(entity);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }


    public List<HealthCheckResultEntity> queryHealthCheckResultByClusterIdAndTimeRange(Long clusterId, Timestamp startTime, Timestamp endTime) {
        lock.readLock().lock();
        try {
            List<HealthCheckResultEntity> result = new ArrayList<>();
            for (HealthCheckResultEntity entity : cache) {
                result.add(entity);
                //if (entity.getClusterId().equals(clusterId) && entity.getCreateTime().after(startTime) && entity.getCreateTime().before(endTime)) {

                //}
            }
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<HealthCheckResultEntity> popAll() {
        lock.writeLock().lock();
        try {
            List<HealthCheckResultEntity> result = new ArrayList<>(cache);
            cache.clear();
            return result;
        } finally {
            lock.writeLock().unlock();
        }
    }
}