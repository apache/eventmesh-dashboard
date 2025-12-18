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

package org.apache.eventmesh.dashboard.core.function.multinetwork;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import lombok.Getter;

@Getter
public enum ClusterPort {

    STORAGE_ROCKETMQ_NAMESERVER,

    STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE,

    STORAGE_ROCKETMQ_BROKER_RAFT;


    static {
          {
            PortMetadata portMetadata = new PortMetadata();
            portMetadata.setVirtual(true);
            portMetadata.setExplanation("RocketMQ 的 virtual 端口是  port - 2");
            STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE.setPortMetadata(portMetadata);

            portMetadata = new PortMetadata();
            portMetadata.setExplanation("RocketMQ virtual 端口 与 port 端口之间的是空的");
            portMetadata.setNullPort(true);
            STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE.setPortMetadata(portMetadata);

            portMetadata = new PortMetadata();
            portMetadata.setConfigName("");
            STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE.setPortMetadata(portMetadata);

            portMetadata = new PortMetadata();
            portMetadata.setConfigName("");
            portMetadata.setEffect("复制端口");
            STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE.setPortMetadata(portMetadata);

            STORAGE_ROCKETMQ_BROKER_RAFT.setClusterPort(STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE);

            portMetadata = new PortMetadata();
            portMetadata.setConfigName("");
            STORAGE_ROCKETMQ_NAMESERVER.setPortMetadata(portMetadata);
          }

    }


    private List<PortMetadata> portMetadataList = new ArrayList<>();

    private PortMetadata clientMetadata;


    private ClusterType clusterType;


    void setClusterPort(ClusterPort clusterPort) {
        if (Objects.isNull(clusterType)) {
            this.clusterType = ClusterType.valueOf(this.name());
        }
        this.clientMetadata = clusterPort.getClientMetadata();
        this.portMetadataList = clusterPort.getPortMetadataList();
    }

    void setPortMetadata(PortMetadata portMetadata) {
        if (Objects.isNull(clusterType)) {
            this.clusterType = ClusterType.valueOf(this.name());
        }
        if (!portMetadata.isVirtual()
            && !portMetadata.isNullPort()
            && Objects.equals(portMetadata.getEffect(), "client")
        ) {
            if (Objects.isNull(this.clientMetadata)) {
                String exceptionString = String.format(" current is %s , now is %s", this.clientMetadata, portMetadata);
                throw new RuntimeException(exceptionString);
            }
        }
        portMetadata.setClusterType(this.clusterType);
        this.portMetadataList.add(portMetadata);
    }

    void compact() {
        List<PortMetadata> portMetadataList = new ArrayList<>(this.portMetadataList.size());
        portMetadataList.addAll(this.portMetadataList);
        this.portMetadataList = portMetadataList;
    }
}
