package org.apache.eventmesh.dashboard.console.function.health;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.model.base.BaseSyncBase;
import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;
import org.apache.eventmesh.dashboard.console.service.function.HealthDataService;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKManage;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKTypeEnum;
import org.apache.eventmesh.dashboard.core.function.SDK.config.CreateJvmCapConfig;
import org.apache.eventmesh.dashboard.core.function.SDK.config.CreateJvmConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class Health2ServiceTest {

    @Mock
    private HealthDataService dataService;

    public static BaseSyncBase createJvm() {
        SDKTypeEnum sdkTypeEnum = SDKTypeEnum.ADMIN;
        BaseSyncBase baseSyncBase = new RuntimeMetadata();
        baseSyncBase.setId(1L);
        baseSyncBase.setClusterType(ClusterType.STORAGE_JVM_BROKER);
        CreateJvmConfig config = new CreateJvmConfig();
        config.setKey(baseSyncBase.getId().toString());
        SDKManage.getInstance().createClient(sdkTypeEnum, baseSyncBase, config, baseSyncBase.getClusterType());
        return baseSyncBase;
    }

    public static BaseSyncBase createJvmCap() {
        SDKTypeEnum sdkTypeEnum = SDKTypeEnum.ADMIN;
        BaseSyncBase baseSyncBase = new RuntimeMetadata();
        baseSyncBase.setId(1L);
        baseSyncBase.setClusterType(ClusterType.STORAGE_JVM_CAP_BROKER);
        CreateJvmCapConfig config = new CreateJvmCapConfig();
        config.setKey(baseSyncBase.getId().toString());
        SDKManage.getInstance().createClient(sdkTypeEnum, baseSyncBase, config, baseSyncBase.getClusterType());
        return baseSyncBase;
    }

    private Health2Service health2Service = new Health2Service();



    @Before
    public void init(){
        health2Service.setDataService(dataService);
    }

    @Test
    public void test(){
        BaseSyncBase baseSyncBase = createJvm();
        health2Service.register(baseSyncBase);
        health2Service.executeAll();
    }

}
