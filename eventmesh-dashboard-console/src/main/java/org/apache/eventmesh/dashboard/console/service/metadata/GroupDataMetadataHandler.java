package org.apache.eventmesh.dashboard.console.service.metadata;

import org.apache.eventmesh.dashboard.console.entity.message.GroupEntity;
import org.apache.eventmesh.dashboard.console.mapper.message.GroupMapper;
import org.apache.eventmesh.dashboard.core.metadata.DataMetadataHandler;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class GroupDataMetadataHandler implements DataMetadataHandler<GroupEntity> {

    private GroupMapper groupMapper;

    @Override
    public void handleAll(List<GroupEntity> addData, List<GroupEntity> updateData, List<GroupEntity> deleteData) {

    }

    @Override
    public List<GroupEntity> getData() {
        return Collections.emptyList();
    }
}
