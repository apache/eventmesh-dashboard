package org.apache.eventmesh.dashboard.console.service.metadata;

import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.mapper.cluster.RuntimeMapper;
import org.apache.eventmesh.dashboard.core.metadata.DataMetadataHandler;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RuntimeDataMetadataHandler implements DataMetadataHandler<RuntimeEntity> {

    private RuntimeMapper runtimeMapper;

    @Override
    public void handleAll(List<RuntimeEntity> addData, List<RuntimeEntity> updateData, List<RuntimeEntity> deleteData) {

    }

    @Override
    public List<RuntimeEntity> getData() {
        return Collections.emptyList();
    }
}
