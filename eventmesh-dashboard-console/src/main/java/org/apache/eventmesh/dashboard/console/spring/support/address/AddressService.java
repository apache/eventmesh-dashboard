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

package org.apache.eventmesh.dashboard.console.spring.support.address;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;

import java.util.List;

/**
 *
 */
public interface AddressService {


    List<ClusterType> clusterType();

    /**
     * 获得 client 地址
     */
    AddressServiceResult createClientAddress(AddressServiceIPDO data);


    /**
     * cap 架构 获得
     */
    AddressServiceResult createCapAddress(AddressServiceIPDO data);

    /**
     * 获得 集群 的 注册中心地址
     */
    AddressServiceResult createRegisterAddress(AddressServiceIPDO data);

    /**
     * 依赖其他 大集群 地址
     */
    AddressServiceResult createDependAddress(AddressServiceIPDO data);


}
