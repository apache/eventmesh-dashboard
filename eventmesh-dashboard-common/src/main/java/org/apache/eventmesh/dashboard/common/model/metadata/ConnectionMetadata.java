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

import lombok.Data;

/**
 * when insert data to db from meta service, connection metadata operation should be called after cluster and client in order to fetch information
 * from them.
 */
@Data
public class ConnectionMetadata extends MetadataConfig {


    /**
     * The type of source. Possible values are "connector" or "client".
     */
    private String sourceType;

    /**
     * The id of the source.<p> It can be connectorId or clientId according to the sourceType.
     */
    private Long sourceId;

    private String sourceHost;

    private Integer sourcePort;

    private String sourceName;

    /**
     * The type of sink. Possible values are "connector" or "client".
     */
    private String sinkType;

    private String sinkHost;

    private Integer sinkPort;

    private String sinkName;

    /**
     * The id of the sink.<p> It can be connectorId or clientId according to the sinkType.
     */
    private Long sinkId;

    private Long runtimeId;

    private String topic;

    private Long groupId;

    private String description;

    @Override
    public String getUnique() {
        return getClusterId() + "/" + sourceId + "/" + sinkId + "/" + topic;
    }
}
