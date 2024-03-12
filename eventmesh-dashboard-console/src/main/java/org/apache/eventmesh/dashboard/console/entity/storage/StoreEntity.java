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

package org.apache.eventmesh.dashboard.console.entity.storage;


import org.apache.eventmesh.dashboard.console.entity.base.BaseEntity;

import java.sql.Timestamp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StoreEntity extends BaseEntity {

    private Long id;

    private Long clusterId;

    private Integer storeId;

    private String storeType;

    private String host;

    private Long runtimeId;

    private String topicList;

    private Short diffType;

    private Integer port;

    private Integer jmxPort;

    private String rack;

    private Short status;

    private Timestamp createTime;

    private Timestamp updateTime;

    private String endpointMap;

    private Long startTimestamp;
}
