package org.apache.eventmesh.dashboard.console.service.deploy.impl;

import org.apache.eventmesh.dashboard.console.entity.cases.DeployScriptEntity;
import org.apache.eventmesh.dashboard.console.mapper.deploy.DeployScriptMapper;
import org.apache.eventmesh.dashboard.console.service.deploy.DeployScriptService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@Transactional
public class DeployScriptServiceImpl implements DeployScriptService {

    @Autowired
    private DeployScriptMapper deployScriptMapper;

    @Override
    public void insert(DeployScriptEntity deployScriptEntity) {
        this.deployScriptMapper.insert(deployScriptEntity);
    }

    @Override
    public void update(DeployScriptEntity deployScriptEntity) {
        this.deployScriptMapper.update(deployScriptEntity);
    }

    @Override
    public void deleteById(DeployScriptEntity deployScriptEntity) {
        this.deployScriptMapper.deleteById(deployScriptEntity);
    }

    @Override
    public DeployScriptEntity queryById(DeployScriptEntity deployScriptEntity) {
        return null;
    }

    @Override
    public void queryByName(DeployScriptEntity deployScriptEntity) {
        this.deployScriptMapper.queryByName(deployScriptEntity);
    }
}
