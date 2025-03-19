package org.apache.eventmesh.dashboard.console.spring.support.metadata;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TopicMetadata {

    private Map<Long, List<TopicMetadata>> metadataMap = new ConcurrentHashMap<>();


    public void syncData(){

    }

}
