/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.eventmesh.dashboard.console.function.report.collect.active;

import org.apache.eventmesh.dashboard.common.model.metadata.ClusterMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;
import org.apache.eventmesh.dashboard.console.function.report.collect.Collect;
import org.apache.eventmesh.dashboard.console.function.report.model.base.Time;
import org.apache.eventmesh.dashboard.core.function.SDK.AbstractClientInfo;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKTypeEnum;

import java.util.ArrayList;
import java.util.List;

import lombok.Setter;

public abstract class  AbstractCollect<C> extends AbstractClientInfo<C> implements Collect {

    protected List<Time> times = new ArrayList<>();

    protected List<Time> standby = new ArrayList<>();


    @Override
    protected SDKTypeEnum getSDKTypeEnum(){
        return SDKTypeEnum.ADMIN;
    }

    public List<Time> collect() {
        List<Time> list;
        this.standby.clear();
        synchronized (this) {
            list = this.times;
            this.times = this.standby;
            this.standby = list;
        }
        return list;
    }


    @Setter
    public static  abstract class AbstractClusterCollect<C> extends AbstractClientInfo<C> {

        private ClusterMetadata clusterMetadata;

    }

    @Setter
    public static  abstract class AbstractRuntimeCollect<C> extends AbstractClusterCollect<C> {

        private List<RuntimeMetadata> runtimeMetadataList;
    }
}