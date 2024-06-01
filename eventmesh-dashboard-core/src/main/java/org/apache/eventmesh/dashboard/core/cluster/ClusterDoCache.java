package org.apache.eventmesh.dashboard.core.cluster;


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


    private Map<ClusterType, Map<String , ColonyDO>>  clusterDoMap = new HashMap<>();

    {
       this.clusterDoMap = this.getClusterDoMap();
    }


    public Map<ClusterType, Map<String , ColonyDO>> getClusterDoMap(){
        Map<ClusterType, Map<String , ColonyDO>>  clusterDoMap = new HashMap<>();
        for(ClusterType clusterType : ClusterType.values()){
            clusterDoMap.put(clusterType, new HashMap<>());
        }
        return clusterDoMap;
    }


    public void setClusterDoMap(Map<ClusterType, Map<String , ColonyDO>> clusterDoMap){
        this.clusterDoMap = clusterDoMap;
    }


    public List<ColonyDO> getEventMeshClusterDO(ClusterTrusteeshipType... clusterTrusteeshipType){
        return this.filterate(ClusterType.EVENTMESH, clusterTrusteeshipType);
    }

    public List<ColonyDO> getMetaNacosClusterDO(ClusterTrusteeshipType... clusterTrusteeshipType){
        return this.filterate(ClusterType.EVENTMESH_META_NACOS, clusterTrusteeshipType);
    }

    public List<ColonyDO> getMetaEtcdClusterDO(ClusterTrusteeshipType... clusterTrusteeshipType){
        return this.filterate(ClusterType.EVENTMESH_META_ETCD, clusterTrusteeshipType);
    }

    public List<ColonyDO> getRocketMQClusterDO(ClusterTrusteeshipType... clusterTrusteeshipType){
        return this.filterate(ClusterType.STORAGE_ROCKETMQ, clusterTrusteeshipType);
    }


    private List<ColonyDO> filterate(ClusterType clusterType , ClusterTrusteeshipType... clusterTrusteeshipTypes){
        Map<ClusterType, Map<String , ColonyDO>>  clusterDoMap = new HashMap<>();
        Map<String, ColonyDO> clusterDOList = clusterDoMap.get(clusterType);

        if(Objects.isNull(clusterTrusteeshipTypes) || clusterTrusteeshipTypes.length == 0){
            return new ArrayList<>(clusterDOList.values());
        }
        List<ColonyDO> newClusterDOList = new ArrayList<>();
        for(ColonyDO clusterDO : clusterDOList.values()){

            newClusterDOList.add(clusterDO);
        }
        return newClusterDOList;
    }


}
