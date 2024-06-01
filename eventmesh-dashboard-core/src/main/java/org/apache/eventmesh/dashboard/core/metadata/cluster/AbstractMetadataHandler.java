package org.apache.eventmesh.dashboard.core.metadata.cluster;

import lombok.Setter;
import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType;
import org.apache.eventmesh.dashboard.core.metadata.MetadataHandler;
import org.apache.eventmesh.dashboard.core.remoting.RemotingManager;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractMetadataHandler<T,S,RE> implements MetadataHandler<T> ,RemotingManager.RemotingRequestWrapper<S,RE>{


    @Setter
    private RemotingManager remotingManager;

    protected S request;

    public void init(){
        this.request = (S)remotingManager.getProxyObject();
    }


    /**
     * 同步的时候，只同步runtime 的数据，还是会同步 storage 的数据。这个可以进行配置。
     * @return
     */
    @Override
    public List<T> getData(){
        List<RemotingManager.RemotingWrapper> remotingWrapperList = new ArrayList<>();
        remotingWrapperList.addAll(remotingManager.getEventMeshClusterDO(ClusterTrusteeshipType.TRUSTEESHIP, ClusterTrusteeshipType.FIRE_AND_FORGET_TRUSTEESHIP));
        remotingWrapperList.addAll(remotingManager.getStorageCluster(ClusterTrusteeshipType.TRUSTEESHIP, ClusterTrusteeshipType.FIRE_AND_FORGET_TRUSTEESHIP));
        return remotingManager.request(this, remotingManager.getEventMeshClusterDO(ClusterTrusteeshipType.TRUSTEESHIP, ClusterTrusteeshipType.FIRE_AND_FORGET_TRUSTEESHIP));
    }

}
