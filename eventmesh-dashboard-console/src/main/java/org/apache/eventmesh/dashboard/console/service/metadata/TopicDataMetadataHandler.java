package org.apache.eventmesh.dashboard.console.service.metadata;

import org.apache.eventmesh.dashboard.console.entity.message.TopicEntity;
import org.apache.eventmesh.dashboard.console.mapper.message.TopicMapper;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TopicDataMetadataHandler extends AbstractDBDataMetadataHandler<TopicEntity> {

    @Autowired
    private TopicMapper topicMapper;

    @Override
    public void handleAll(List<TopicEntity> addData, List<TopicEntity> updateData, List<TopicEntity> deleteData) {
        topicMapper.batchInsert(addData);

    }


    @Override
    List<TopicEntity> doGetData() {
        TopicEntity topicEntity = this.getEntity();
        return List.of();
    }
}
