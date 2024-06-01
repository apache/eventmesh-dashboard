package org.apache.eventmesh.dashboard.console.function;

import com.alibaba.fastjson.JSON;
import org.apache.eventmesh.dashboard.common.model.metadata.ClusterMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.ClusterRelationshipMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterEntity;
import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterRelationshipEntity;
import org.apache.eventmesh.dashboard.console.entity.runtime.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterRelationshipService;
import org.apache.eventmesh.dashboard.console.service.cluster.ClusterService;
import org.apache.eventmesh.dashboard.console.service.runtime.RuntimeService;
import org.apache.eventmesh.dashboard.core.cluster.ColonyDO;
import org.apache.eventmesh.dashboard.core.remoting.RemotingManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ClusterInfoManager {

    @Autowired
    private ClusterService clusterService;

    @Autowired
    private RuntimeService runtimeService;


    @Autowired
    private ClusterRelationshipService clusterRelationshipService;

    @Autowired
    private RemotingManager remotingManager;

    private Map<Long/** clusterId **/, ColonyDO> clusterEntityMap = new HashMap<>();

    private Map<Long/** clusterId **/, Map<Long/** clusterId **/, ColonyDO>> relationshipMap = new HashMap<>();


    private Map<Long/** clusterId **/,  ColonyDO> runtimeCollectMap = new HashMap<>();


    @PostConstruct
    public void initSync() throws Exception {
        this.sync(this.clusterService.selectAll(),this.runtimeService.selectAll(),this.clusterRelationshipService.selectAll());
        remotingManager.loadingCompleted();

    }

    private void sync(List<ClusterEntity> clusterEntityList, List<RuntimeEntity> runtimeEntityList, List<ClusterRelationshipEntity> clusterRelationshipEntityList){
        if(!clusterEntityList.isEmpty()) {
            String jsonString = JSON.toJSONString(clusterEntityList);
            List<ClusterMetadata> clusterMetadataList = JSON.parseArray(jsonString, ClusterMetadata.class);
            remotingManager.cacheCluster(clusterMetadataList);
        }
        if(!runtimeEntityList.isEmpty()) {
            String jsonString = JSON.toJSONString(runtimeEntityList);
            List<RuntimeMetadata> runtimeMetadataList = JSON.parseArray(jsonString, RuntimeMetadata.class);
            remotingManager.cacheRuntime(runtimeMetadataList);
        }
        if(!clusterRelationshipEntityList.isEmpty()) {
            String jsonString = JSON.toJSONString(runtimeEntityList);
            List<ClusterRelationshipMetadata> runtimeMetadataList = JSON.parseArray(jsonString, ClusterRelationshipMetadata.class);
            remotingManager.cacheClusterRelationship(runtimeMetadataList);
        }
    }


    @Scheduled(initialDelay = 50, fixedDelay = 30 * 1000)
    public void sync() {
        LocalDateTime  updatedDateTime =  LocalDateTime.now().minusSeconds(30);
        ClusterEntity clusterEntity = new ClusterEntity();
        clusterEntity.setUpdateTime(updatedDateTime);
        RuntimeEntity runtimeEntity = new RuntimeEntity();
        clusterEntity.setUpdateTime(updatedDateTime);
        ClusterRelationshipEntity clusterRelationshipEntity = new ClusterRelationshipEntity();
        clusterEntity.setUpdateTime(updatedDateTime);
        this.sync(clusterService.selectNewlyIncreased(clusterEntity),this.runtimeService.selectNewlyIncreased(runtimeEntity),this.clusterRelationshipService.selectNewlyIncreased(clusterRelationshipEntity));
    }
}
