package org.apache.eventmesh.dashboard.console.service.deploy.impl;

import org.apache.eventmesh.dashboard.console.entity.cases.PortEntity;
import org.apache.eventmesh.dashboard.console.mapper.deploy.PortMapper;
import org.apache.eventmesh.dashboard.console.modle.deploy.GetPortsDO;
import org.apache.eventmesh.dashboard.console.service.deploy.PortService;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PortServiceImpl implements PortService {

    @Autowired
    private PortMapper portMapper;

    @Override
    public List<String> getPorts(GetPortsDO getPortsDO) {
        PortEntity portEntity = new PortEntity();
        PortEntity newPort = this.portMapper.lockPort(portEntity);
        return List.of();
    }
}
