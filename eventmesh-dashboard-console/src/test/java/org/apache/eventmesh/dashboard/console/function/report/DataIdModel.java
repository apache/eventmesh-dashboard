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

package org.apache.eventmesh.dashboard.console.function.report;

import org.apache.eventmesh.dashboard.console.function.report.elasticsearch.DataModel.DataModelBuilder;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class DataIdModel {

    private Long organizationId;

    private Long clusterId;

    private Long runtimeId;

    private Long topicId;

    private Long groupId;

    private Long queueId;

    private Long subscriptionId;

    private Integer count = 3;

    private Integer deep = 3;

    private Integer totalTrequency;

    private boolean init = false;

    private DataModelBuilder dataModelBuilder;

    public void init() {
        if (init) {
            return;
        }
        this.totalTrequency = (int) Math.pow(count, deep);
        this.init = true;
    }

    public List<org.apache.eventmesh.dashboard.console.function.report.elasticsearch.DataModel> getRuntimeDataModel() {
        return this.get(3, () -> this.runtimeId(null, false));
    }

    public List<org.apache.eventmesh.dashboard.console.function.report.elasticsearch.DataModel> get(Integer deep, Runnable runnable) {
        this.deep = deep;
        this.init();
        List<org.apache.eventmesh.dashboard.console.function.report.elasticsearch.DataModel> list = new ArrayList<>();
        for (int i = 0; i < this.totalTrequency; i++) {
            dataModelBuilder = org.apache.eventmesh.dashboard.console.function.report.elasticsearch.DataModel.builder();
            runnable.run();
            this.runtimeId(null, false);
            list.add(dataModelBuilder.build());
        }

        return list;
    }

    public void runtimeId(Long id, boolean newValue) {
        boolean newValue1 = false;
        if (this.runtimeId == null) {
            this.runtimeId = 0L;
        }
        if (this.deep == 3) {
            this.runtimeId++;
            newValue1 = true;
        } else if (id != null) {
            Integer runtimeCount = (int) Math.pow(this.deep, 3);
            if (id != 1 && id % this.deep == 1 && newValue) {
                this.runtimeId++;
                newValue1 = true;
            }
        }
        this.clusterId(this.runtimeId, newValue1);

        dataModelBuilder.runtimeId(this.runtimeId);
        dataModelBuilder.runtimeName("runtimeName-" + this.runtimeId);
    }

    public void clusterId(Long id, boolean newValue) {
        boolean newValue1 = false;
        if (this.clusterId == null) {
            this.clusterId = 0L;
        }
        if (this.deep == 2) {
            this.clusterId++;
        } else {
            //Integer runtimeCount = (int) Math.pow(this.deep, this.deep - 2);
            if (id != 1 && id % this.deep == 1 && newValue) {
                this.clusterId++;
                newValue1 = true;
            }
        }
        if (this.clusterId == 0) {
            this.clusterId++;
        }
        this.organizationId(this.clusterId, newValue1);
        dataModelBuilder.clusterId(this.clusterId);
        dataModelBuilder.clusterName("cluster-" + this.clusterId);
    }

    public void organizationId(Long id, boolean newValue) {
        if (this.organizationId == null) {
            this.organizationId = 0L;
        }
        if (this.deep == 1) {
            this.organizationId++;
        } else {
            if (id != 1 && id % this.deep == 1 && newValue) {
                this.organizationId++;
            }
        }
        if (this.organizationId == 0) {
            this.organizationId++;
        }
        dataModelBuilder.organizationId(this.organizationId);
        dataModelBuilder.organizationName("organization-" + this.organizationId);
    }

}
