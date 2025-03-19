package org.apache.eventmesh.dashboard.console.service.metadata;

import org.apache.eventmesh.dashboard.console.entity.cluster.NetConnectionEntity;
import org.apache.eventmesh.dashboard.core.metadata.DataMetadataHandler;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class NetConnectionDataMetadataHandler implements DataMetadataHandler<NetConnectionEntity> {

    @Override
    public void handleAll(List<NetConnectionEntity> addData, List<NetConnectionEntity> updateData, List<NetConnectionEntity> deleteData) {

    }

    @Override
    public List<NetConnectionEntity> getData() {
        return Collections.emptyList();
    }
}
