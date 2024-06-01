package org.apache.eventmesh.dashboard.console.cache;


import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType;
import org.apache.eventmesh.dashboard.common.enums.ClusterType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ClusterDoCache {

    private static final ClusterDoCache INSTANCE = new ClusterDoCache();


    public static final ClusterDoCache getInstance() {
        return INSTANCE;
    }


    private ClusterDoCache(){}


    private Map<ClusterType, Map<String , ClusterDO>>  clusterDoMap = new HashMap<>();

    {
       this.clusterDoMap = this.getClusterDoMap();
    }


    public Map<ClusterType, Map<String , ClusterDO>> getClusterDoMap(){
        Map<ClusterType, Map<String , ClusterDO>>  clusterDoMap = new HashMap<>();
        for(ClusterType clusterType : ClusterType.values()){
            clusterDoMap.put(clusterType, new HashMap<>());
        }
        return clusterDoMap;
    }


    public void setClusterDoMap(Map<ClusterType, Map<String , ClusterDO>> clusterDoMap){
        this.clusterDoMap = clusterDoMap;
    }


    public List<ClusterDO> getEventMeshClusterDO(ClusterTrusteeshipType... clusterTrusteeshipType){
        return this.filterate(ClusterType.EVENTMESH, clusterTrusteeshipType);
    }

    public List<ClusterDO> getMetaNacosClusterDO(ClusterTrusteeshipType... clusterTrusteeshipType){
        return this.filterate(ClusterType.EVENTMESH_META_ETCD, clusterTrusteeshipType);
    }

    public List<ClusterDO> getMetaEtcdClusterDO(ClusterTrusteeshipType... clusterTrusteeshipType){
        return this.filterate(ClusterType.EVENTMESH_META_NACOS, clusterTrusteeshipType);
    }

    public List<ClusterDO> getRocketMQClusterDO(ClusterTrusteeshipType... clusterTrusteeshipType){
        return this.filterate(ClusterType.STORAGE_ROCKETMQ, clusterTrusteeshipType);
    }


    private List<ClusterDO> filterate(ClusterType clusterType , ClusterTrusteeshipType... clusterTrusteeshipTypes){
        Map<ClusterType, Map<String , ClusterDO>>  clusterDoMap = new HashMap<>();
        Map<String, ClusterDO> clusterDOList = clusterDoMap.get(clusterType);

        if(Objects.isNull(clusterTrusteeshipTypes) || clusterTrusteeshipTypes.length == 0){
            return new ArrayList<>(clusterDOList.values());
        }
        List<ClusterDO> newClusterDOList = new ArrayList<>();
        for(ClusterDO clusterDO : clusterDOList.values()){

            newClusterDOList.add(clusterDO);
        }
        return newClusterDOList;
    }


}
