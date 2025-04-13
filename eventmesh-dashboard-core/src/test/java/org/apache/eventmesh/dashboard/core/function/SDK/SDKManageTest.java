package org.apache.eventmesh.dashboard.core.function.SDK;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.model.base.BaseSyncBase;
import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;
import org.apache.eventmesh.dashboard.core.function.SDK.config.CreateJvmCapConfig;
import org.apache.eventmesh.dashboard.core.function.SDK.config.CreateJvmConfig;

import org.junit.Test;

public class SDKManageTest {

    SDKManage sdkManage = SDKManage.getInstance();

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

    @Test
    public void test_createJVMSDK() {
        createJvm();
    }

    @Test
    public void test_createJVMCapSDK() {
        createJvmCap();
    }

}
