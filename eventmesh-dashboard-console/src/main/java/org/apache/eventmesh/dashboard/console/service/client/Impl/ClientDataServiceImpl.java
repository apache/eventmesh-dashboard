package org.apache.eventmesh.dashboard.console.service.client.Impl;

import org.apache.eventmesh.dashboard.console.entity.client.ClientEntity;
import org.apache.eventmesh.dashboard.console.mapper.client.ClientMapper;
import org.apache.eventmesh.dashboard.console.service.client.ClientDataService;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ClientDataServiceImpl implements ClientDataService {

    @Autowired
    private ClientMapper clientMapper;

    @Override
    public List<ClientEntity> selectAll() {
        return clientMapper.selectAll();
    }

    @Override
    public void batchInsert(List<ClientEntity> clientEntityList) {
        clientMapper.batchInsert(clientEntityList);
    }
}
