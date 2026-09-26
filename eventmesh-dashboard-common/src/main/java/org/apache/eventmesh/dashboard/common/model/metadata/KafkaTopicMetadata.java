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

import org.apache.eventmesh.dashboard.common.model.base.BaseRuntimeIdBase;

import java.util.Map;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** Kafka topic mutation parameters; these additional fields are not persisted by console mappers. */
@Data
@EqualsAndHashCode(callSuper = true)
public class KafkaTopicMetadata extends BaseRuntimeIdBase {

    private String topicName;

    /** Required on creation; optional target total partition count on update. */
    private Integer partitionCount;

    /** Required on creation (1..32767); replica reassignment is not supported by update. */
    private Integer replicationFactor;

    /** Explicit SET operations only; omitted keys retain their current values. Null values are invalid. */
    private Map<String, String> configs;

    @Override
    public String nodeUnique() {
        return this.topicName;
    }
}
