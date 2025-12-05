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

package org.apache.eventmesh.dashboard.common.model.metadata;

import org.apache.eventmesh.dashboard.common.enums.CollectType;
import org.apache.eventmesh.dashboard.common.model.base.BaseClusterIdBase;

public class CollectMetadata extends BaseClusterIdBase {

    private CollectType collectType;

    private Long saveTime;

    private Long collectInterval;

    private Long storageClusterId;

    private Long storageClusterName;

    private Boolean enable;

    private Boolean defaultStorage;

    @Override
    public String nodeUnique() {
        return "";
    }

    public CollectType getCollectType() {
        return collectType;
    }

    public void setCollectType(CollectType collectType) {
        this.collectType = collectType;
    }

    public Long getSaveTime() {
        return saveTime;
    }

    public void setSaveTime(Long saveTime) {
        this.saveTime = saveTime;
    }

    public Long getCollectInterval() {
        return collectInterval;
    }

    public void setCollectInterval(Long collectInterval) {
        this.collectInterval = collectInterval;
    }

    public Long getStorageClusterId() {
        return storageClusterId;
    }

    public void setStorageClusterId(Long storageClusterId) {
        this.storageClusterId = storageClusterId;
    }

    public Long getStorageClusterName() {
        return storageClusterName;
    }

    public void setStorageClusterName(Long storageClusterName) {
        this.storageClusterName = storageClusterName;
    }

    public Boolean getEnable() {
        return enable;
    }

    public void setEnable(Boolean enable) {
        this.enable = enable;
    }

    public Boolean getDefaultStorage() {
        return defaultStorage;
    }

    public void setDefaultStorage(Boolean defaultStorage) {
        this.defaultStorage = defaultStorage;
    }
}
