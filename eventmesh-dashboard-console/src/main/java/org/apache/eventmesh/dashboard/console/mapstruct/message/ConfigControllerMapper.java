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

<<<<<<<< HEAD:eventmesh-dashboard-console/src/main/java/org/apache/eventmesh/dashboard/console/mapstruct/message/ConfigControllerMapper.java
package org.apache.eventmesh.dashboard.console.mapstruct.message;
========

package org.apache.eventmesh.dashboard.console.modle.domain;
>>>>>>>> main/main:eventmesh-dashboard-console/src/main/java/org/apache/eventmesh/dashboard/console/model/domain/RuntimeEntityDO.java

import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.entity.function.ConfigEntity;
<<<<<<<< HEAD:eventmesh-dashboard-console/src/main/java/org/apache/eventmesh/dashboard/console/mapstruct/message/ConfigControllerMapper.java
import org.apache.eventmesh.dashboard.console.model.function.config.QueryByInstanceIdDTO;
========
import org.apache.eventmesh.dashboard.core.cluster.RuntimeBaseDO;
>>>>>>>> main/main:eventmesh-dashboard-console/src/main/java/org/apache/eventmesh/dashboard/console/model/domain/RuntimeEntityDO.java

import lombok.Data;
import lombok.EqualsAndHashCode;

<<<<<<<< HEAD:eventmesh-dashboard-console/src/main/java/org/apache/eventmesh/dashboard/console/mapstruct/message/ConfigControllerMapper.java
/**
 *
 */
@Mapper
public interface ConfigControllerMapper {
========

@Data
@EqualsAndHashCode(callSuper = true)
public class RuntimeEntityDO extends RuntimeBaseDO<RuntimeEntity, Object, ConfigEntity> {
>>>>>>>> main/main:eventmesh-dashboard-console/src/main/java/org/apache/eventmesh/dashboard/console/model/domain/RuntimeEntityDO.java


<<<<<<<< HEAD:eventmesh-dashboard-console/src/main/java/org/apache/eventmesh/dashboard/console/mapstruct/message/ConfigControllerMapper.java
    ConfigEntity queryByInstanceId(QueryByInstanceIdDTO queryByInstanceIdDTO);

========
>>>>>>>> main/main:eventmesh-dashboard-console/src/main/java/org/apache/eventmesh/dashboard/console/model/domain/RuntimeEntityDO.java
}
