package org.apache.eventmesh.dashboard.core.remoting.rocketmq;

import org.apache.eventmesh.dashboard.common.model.remoting.acl.CreateAclRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.acl.CreateAclResult;
import org.apache.eventmesh.dashboard.common.model.remoting.acl.DeleteAclRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.acl.DeleteAclResult;
import org.apache.eventmesh.dashboard.common.model.remoting.acl.GetAclsRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.acl.GetAclsResult;
import org.apache.eventmesh.dashboard.service.remoting.AclRemotingService;

public class RocketMQAclRemotingService extends AbstractRocketMQRemotingService implements AclRemotingService {


    @Override
    public CreateAclResult createAcl(CreateAclRequest createAclRequest) {
        //this.defaultMQAdminExt.createAndUpdatePlainAccessConfig();
        return null;
    }

    @Override
    public DeleteAclResult deleteAcl(DeleteAclRequest deleteAclRequest) {
        return null;
    }

    @Override
    public GetAclsResult getAllAcls(GetAclsRequest getAclsRequest) {
        return null;
    }
}
