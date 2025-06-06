/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.apache.eventmesh.dashboard.core.remoting.jvm;

import org.apache.eventmesh.dashboard.common.model.remoting.acl.CreateAclRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.acl.CreateAclResult;
import org.apache.eventmesh.dashboard.common.model.remoting.acl.DeleteAclRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.acl.DeleteAclResult;
import org.apache.eventmesh.dashboard.common.model.remoting.acl.GetAcls2Request;
import org.apache.eventmesh.dashboard.common.model.remoting.acl.GetAclsResult;
import org.apache.eventmesh.dashboard.service.remoting.AclRemotingService;


public class JvmAclRemotingService extends AbstractJvmRemotingService implements AclRemotingService {


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
    public GetAclsResult getAllAcls(GetAcls2Request getAclsRequest) {
        return null;
    }
}
