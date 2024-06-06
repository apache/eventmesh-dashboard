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

package org.apache.eventmesh.dashboard.console.function.metadata.handler.db;

import org.apache.eventmesh.dashboard.common.enums.RecordStatus;
import org.apache.eventmesh.dashboard.common.model.metadata.ConnectionMetadata;
import org.apache.eventmesh.dashboard.common.model.remoting.GlobalRequest;
import org.apache.eventmesh.dashboard.console.entity.client.ClientEntity;
import org.apache.eventmesh.dashboard.console.entity.connection.ConnectionEntity;
import org.apache.eventmesh.dashboard.console.entity.connector.ConnectorEntity;
import org.apache.eventmesh.dashboard.console.service.client.ClientDataService;
import org.apache.eventmesh.dashboard.console.service.connection.ConnectionDataService;
import org.apache.eventmesh.dashboard.console.service.connector.ConnectorDataService;
import org.apache.eventmesh.dashboard.core.metadata.MetadataHandler;

import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ConnectionMetadataHandlerToDbImpl implements MetadataHandler<ConnectionMetadata> {

    @Autowired
    private ClientDataService clientDataService;

    @Autowired
    private ConnectionDataService connectionService;

    @Autowired
    private ConnectorDataService connectorDataService;

    @Override
    public void addMetadata(ConnectionMetadata meta) {
        if (Objects.equals(meta.getSinkType(), "connector")) {
            ConnectorEntity query = ConnectorEntity.builder()
                .host(meta.getSinkHost())
                .port(meta.getSinkPort())
                .build();
            List<ConnectorEntity> sink = connectorDataService.selectByHostPort(query);
            if (sink.size() == 1) {
                meta.setSinkId(sink.get(0).getId());
            } else if (sink.isEmpty()) {
                log.info("sink connector not found, sinkHost:{}, sinkPort:{}.creating one", meta.getSinkHost(), meta.getSinkPort());
                ConnectorEntity connectorEntity = new ConnectorEntity(meta.getClusterId(), meta.getSinkName(), "", "", 1, meta.getSinkHost(),
                    meta.getSinkPort(), 4, "");
                connectorDataService.createConnector(connectorEntity);
                meta.setSinkId(connectorEntity.getId());
            } else {
                log.error("more than 1 sink connector active, sinkHost:{}, sinkPort:{}", meta.getSinkHost(), meta.getSinkPort());
            }

        } else if (Objects.equals(meta.getSinkType(), "client")) {
            ClientEntity query = new ClientEntity();
            query.setHost(meta.getSinkHost());
            query.setPort(meta.getSinkPort());
            List<ClientEntity> sink = clientDataService.selectByHostPort(query);
            if (sink.size() == 1) {
                meta.setSinkId(sink.get(0).getId());
            } else if (sink.isEmpty()) {
                log.info("sink client not found, sinkHost:{}, sinkPort:{}.creating one", meta.getSinkHost(), meta.getSinkPort());
                ClientEntity clientEntity = new ClientEntity();
                clientEntity.setStatusEntity(RecordStatus.ACTIVE);
                clientEntity.setName("");
                clientEntity.setPlatform("");
                clientEntity.setLanguage("");
                clientEntity.setPid(0L);
                clientEntity.setProtocol("");
                clientEntity.setConfigIds("");
                clientEntity.setDescription("");
                clientEntity.setClusterId(0L);
                clientEntity.setHost(meta.getSinkHost());
                clientEntity.setPort(meta.getSinkPort());
                clientDataService.addClient(clientEntity);
                meta.setSinkId(clientEntity.getId());
            } else {
                log.error("more than 1 sink client active, sinkHost:{}, sinkPort:{}", meta.getSinkHost(), meta.getSinkPort());
            }
        }

        if (Objects.equals(meta.getSourceType(), "connector")) {
            ConnectorEntity query = ConnectorEntity.builder()
                .host(meta.getSourceHost())
                .port(meta.getSourcePort())
                .build();
            List<ConnectorEntity> source = connectorDataService.selectByHostPort(query);
            if (source.size() == 1) {
                meta.setSourceId(source.get(0).getId());
            } else if (source.isEmpty()) {
                log.info("source connector not found, sourceHost:{}, sourcePort:{}.creating one", meta.getSourceHost(), meta.getSourcePort());
                ConnectorEntity connectorEntity = new ConnectorEntity(meta.getClusterId(), meta.getSourceName(), "", "", 1, meta.getSourceHost(),
                    meta.getSourcePort(), 4, "");
                connectorDataService.createConnector(connectorEntity);
                meta.setSourceId(connectorEntity.getId());
            } else {
                log.error("more than 1 source connector active, sourceHost:{}, sourcePort:{}", meta.getSourceHost(), meta.getSourcePort());
            }
        }

        if (Objects.equals(meta.getSourceType(), "client")) {
            ClientEntity query = new ClientEntity();
            query.setHost(meta.getSourceHost());
            query.setPort(meta.getSourcePort());
            List<ClientEntity> source = clientDataService.selectByHostPort(query);
            if (source.size() == 1) {
                meta.setSourceId(source.get(0).getId());
            } else if (source.isEmpty()) {
                log.info("source client not found, sourceHost:{}, sourcePort:{}.creating one", meta.getSourceHost(), meta.getSourcePort());

                ClientEntity clientEntity = new ClientEntity();
                clientEntity.setStatusEntity(RecordStatus.ACTIVE);
                clientEntity.setName("");
                clientEntity.setPlatform("");
                clientEntity.setLanguage("");
                clientEntity.setPid(0L);
                clientEntity.setProtocol("");
                clientEntity.setConfigIds("");
                clientEntity.setDescription("");
                clientEntity.setClusterId(0L);
                clientEntity.setHost(meta.getSinkHost());
                clientEntity.setPort(meta.getSinkPort());
                clientDataService.addClient(clientEntity);
                meta.setSourceId(clientEntity.getId());
            } else {
                log.error("more than 1 source client active, sourceHost:{}, sourcePort:{}", meta.getSourceHost(), meta.getSourcePort());
            }
        }

        connectionService.insert(new ConnectionEntity(meta));
    }

    @Override
    public void deleteMetadata(ConnectionMetadata meta) {

    }

    @Override
    public List<ConnectionMetadata> getData() {
        return null;
    }

    @Override
    public List<ConnectionMetadata> getData(GlobalRequest globalRequest) {
        return null;
    }
}
