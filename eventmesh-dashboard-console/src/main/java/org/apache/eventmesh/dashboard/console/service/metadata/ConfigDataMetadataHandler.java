package org.apache.eventmesh.dashboard.console.service.metadata;

import org.apache.eventmesh.dashboard.console.entity.function.ConfigEntity;
import org.apache.eventmesh.dashboard.core.metadata.DataMetadataHandler;

import java.util.Collections;
import java.util.List;

public class ConfigDataMetadataHandler implements DataMetadataHandler<ConfigEntity> {

    @Override
    public void handleAll(List<ConfigEntity> addData, List<ConfigEntity> updateData, List<ConfigEntity> deleteData) {

    }

    @Override
    public List<ConfigEntity> getData() {
        return Collections.emptyList();
    }
}
