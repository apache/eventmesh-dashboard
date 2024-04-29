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

package org.apache.eventmesh.dashboard.console.entity.topic;

import org.apache.eventmesh.dashboard.common.model.metadata.TopicMetadata;
import org.apache.eventmesh.dashboard.console.entity.base.BaseEntity;

import java.sql.Timestamp;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true, exclude = "status")
@SuperBuilder
public class TopicEntity extends BaseEntity {

    private Long id;

    private Long clusterId;

    private String topicName;

    private Long storageId;

    @Schema(description = "time to live in milliseconds, -2 unknown, -1 no limit;", example = "1000")
    private Long retentionMs;

    /**
     * topic type, 0: normal, 1: EventMesh internal;
     */
    @Schema(description = "topic type, 0: normal, 1: EventMesh internal;", example = "0")
    private Integer type;

    private String description;

    private Timestamp createTime;

    private Timestamp updateTime;

    private Integer status;

    private Integer createProgress;

    public TopicEntity(TopicMetadata source) {
        setClusterId(source.getClusterId());
        setTopicName(source.getTopicName());
        setStorageId(source.getStorageId());
        setRetentionMs(source.getRetentionMs());
        setType(source.getType());
        setDescription(source.getDescription());
        setStatus(1);
        setCreateProgress(1);
    }
}
