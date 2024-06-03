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

package org.apache.eventmesh.dashboard.console.entity.config;

import org.apache.eventmesh.dashboard.common.model.metadata.ConfigMetadata;
import org.apache.eventmesh.dashboard.console.entity.base.BaseEntity;

import java.sql.Timestamp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ConfigEntity extends BaseEntity {

    private Long id;

    private Long clusterId;

    private String businessType;

    /**
     * config type 0:runtime,1:storage,2:connector,3:topic
     */
    private Integer instanceType;

    private Long instanceId;

    private String configName;

    private String configValue;

    private String startVersion;

    private String eventmeshVersion;

    private Integer status;

    private String endVersion;

    private Integer diffType;

    private String description;

    private Integer edit;

    private Timestamp createTime;

    private Timestamp updateTime;

    private Integer isDefault;

    private Integer isModify;

    private Integer alreadyUpdate;

    public boolean matchVersion(String eventmeshVersion) {
        return (xiaoyu(eventmeshVersion) && dayu(eventmeshVersion));
    }


    public boolean xiaoyu(String eventmeshVersion) {
        String[] em = eventmeshVersion.split(".");
        String[] startVersion = this.getStartVersion().split(".");
        boolean flag = true;
        for (int i = 0; i < em.length; i++) {
            if (Integer.valueOf(em[i]) < Integer.valueOf(startVersion[i])) {
                flag = false;
                break;
            } else if (Integer.valueOf(em[i]) == Integer.valueOf(startVersion[i])) {
                continue;
            } else {
                break;
            }
        }
        return flag;
    }

    public boolean dayu(String eventmeshVersion) {
        String[] em = eventmeshVersion.split(".");
        String[] endVersion = this.getEndVersion().split(".");
        boolean flag = true;
        for (int i = 0; i < em.length; i++) {
            if (Integer.valueOf(em[i]) < Integer.valueOf(endVersion[i])) {
                break;
            } else if (Integer.valueOf(em[i]) == Integer.valueOf(endVersion[i])) {
                continue;
            } else {
                flag = false;
                break;
            }
        }
        return flag;
    }

    public ConfigEntity(ConfigMetadata source) {
        setConfigName(source.getConfigKey());
        setConfigValue(source.getConfigValue());
        setClusterId(source.getClusterId());
        setEdit(1);
        setBusinessType("");
        setInstanceId(source.getInstanceId());
        setDescription("");
        setInstanceType(source.getInstanceType());
        setIsDefault(0);
        setStartVersion("");
        setEndVersion("");
        setEventmeshVersion("");
        setDiffType(0);
        setIsModify(0);
        setStatus(1);
    }
}
