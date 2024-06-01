package org.apache.eventmesh.dashboard.core.cluster;

import lombok.Data;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * eventmesh ClusterDO
 *   meta    ClusterDO
 *   runtime ClusterDO
 *   storage ClusterDO
 *      meta（注册中心，zk，） ClusterDO
 *      runtime（broker）   ClusterDO
 *
 */
@Data
public class ColonyDO {

    private Long superiorId;

    private ClusterDO clusterDO;

    // 双活集群 所以是可以是一个list的
    // 可以默认一个集群
    private Map<Long,ColonyDO> runtimeColonyDOList = new ConcurrentHashMap<>();

    // 只有 eventmesh 集群有这个点，其他没有。
    private Map<Long,ColonyDO> storageColonyDOList = new ConcurrentHashMap<>();

    /**
     * A(nameserver cluster) a1 a2 a3
     * B(nameserver cluster) b1 b2 b3
     *
     * rocketmq   a1,a2,a3,b1,b2,b3
     */
    private Map<Long,ColonyDO> metaColonyDOList = new ConcurrentHashMap<>();


    public Long getClusterId(){
        return Objects.nonNull(this.clusterDO.getClusterInfo().getClusterId())?this.clusterDO.getClusterInfo().getClusterId():this.clusterDO.getClusterInfo().getId() ;
    }

}
