package org.apache.eventmesh.dashboard.service.remoting;

import org.apache.eventmesh.dashboard.common.model.remoting.runtime.GetRuntimeRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.runtime.GetRuntimeResult;

public interface RuntimeRemotingService {

    GetRuntimeResult getRuntimeMetadata(GetRuntimeRequest request);
}
