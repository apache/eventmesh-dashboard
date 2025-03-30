package org.apache.eventmesh.dashboard.core.metadata.result;

import org.apache.eventmesh.dashboard.common.model.base.BaseSyncBase;

import java.util.List;

public interface MetadataSyncResultHandler {


    void register(List<MetadataSyncResult> metadataSyncResults);

    void unregister(BaseSyncBase baseSyncBase);

    void handleMetadataSyncResult(MetadataSyncResult metadataSyncResult);



}
