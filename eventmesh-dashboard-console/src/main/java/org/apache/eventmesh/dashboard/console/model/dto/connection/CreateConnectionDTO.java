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

<<<<<<<< HEAD:eventmesh-dashboard-console/src/main/java/org/apache/eventmesh/dashboard/console/model/dto/connection/CreateConnectionDTO.java

package org.apache.eventmesh.dashboard.console.model.dto.connection;
========

package org.apache.eventmesh.dashboard.common.model.remoting.topic;

import org.apache.eventmesh.dashboard.common.model.remoting.Global2Request;
>>>>>>>> main/main:eventmesh-dashboard-common/src/main/java/org/apache/eventmesh/dashboard/common/model/remoting/topic/GetTopics2Request.java

import lombok.Data;
import lombok.EqualsAndHashCode;

<<<<<<<< HEAD:eventmesh-dashboard-console/src/main/java/org/apache/eventmesh/dashboard/console/model/dto/connection/CreateConnectionDTO.java
@NoArgsConstructor
@AllArgsConstructor
@Data
public class CreateConnectionDTO {
========
@Data
@EqualsAndHashCode(callSuper = true)
public class GetTopics2Request extends Global2Request {
>>>>>>>> main/main:eventmesh-dashboard-common/src/main/java/org/apache/eventmesh/dashboard/common/model/remoting/topic/GetTopics2Request.java

    private Long clusterId;

    private AddConnectionDTO addConnectionDTO;

    private AddConnectorConfigDTO addConnectorConfigDTO;
}
