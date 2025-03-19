package org.apache.eventmesh.dashboard.console.spring.support.metadata;

import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType.FirstToWhom;
import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.enums.MetadataType;
import org.apache.eventmesh.dashboard.console.entity.BaseEntity;
import org.apache.eventmesh.dashboard.core.metadata.result.MetadataSyncResult;
import org.apache.eventmesh.dashboard.core.metadata.result.MetadataSyncResultHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Component;

@Component
public class DefaultMetadataSyncResultHandler implements MetadataSyncResultHandler {

    private Map<ClusterType, Map<Long, Dimension>> dimensionMap = new ConcurrentHashMap<>();


    public void register(Dimension dimension) {

    }

    /**
     *
     */
    public static class Dimension {

        /**
         * 如果第一次同步，需要修改对应的， dimension 对象。runtime cluster
         * @see org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType.FirstToWhom
         *
         * 每次完成同步，进行整理，如果有失败的，进行记录并且对 dimension 队形进行标记
         */
        private BaseEntity dimensionEntity;

        private FirstToWhom firstToWhom;

        private Map<MetadataType, MetadataSyncResult> metadataSyncResultMap = new ConcurrentHashMap<>();

        private AtomicLong num = new AtomicLong(0);


        public void record(MetadataType metadataType, MetadataSyncResult metadataSyncResult){

        }
    }
}
