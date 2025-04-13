package org.apache.eventmesh.dashboard.core.remoting;

import org.apache.eventmesh.dashboard.common.model.base.BaseClusterIdBase;
import org.apache.eventmesh.dashboard.common.model.base.BaseSyncBase;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKManageTest;
import org.apache.eventmesh.dashboard.core.metadata.DataMetadataHandler;
import org.apache.eventmesh.dashboard.service.remoting.AclRemotingService;

import org.junit.Test;

public class Remoting2ManageTest {

    Remoting2Manage remoting2Manage = Remoting2Manage.getInstance();

    @Test
    public void test() {
        System.out.println(remoting2Manage);
    }


    @Test
    public void test_createDataMetadataHandler() {
        BaseSyncBase baseSyncBase = SDKManageTest.createJvm();
        DataMetadataHandler<BaseClusterIdBase> dataMetadataHandler =
            remoting2Manage.createDataMetadataHandler(AclRemotingService.class, baseSyncBase);
        dataMetadataHandler.getData();
    }


}
