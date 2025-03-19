package org.apache.eventmesh.dashboard.console.service.metadata;

import org.apache.eventmesh.dashboard.console.entity.cluster.ClientEntity;
import org.apache.eventmesh.dashboard.console.mapper.cluster.ClientMapper;
import org.apache.eventmesh.dashboard.core.metadata.DataMetadataHandler;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ClientDataMetadataHandler implements DataMetadataHandler<ClientEntity> {

    @Autowired
    private ClientMapper clientMapper;

    @Override
    public void handleAll(List<ClientEntity> addData, List<ClientEntity> updateData, List<ClientEntity> deleteData) {

    }

    @Override
    public List<ClientEntity> getData() {
        return Collections.emptyList();
    }
}
