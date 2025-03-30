package org.apache.eventmesh.dashboard.console.spring.support.metadata.convert;

import org.apache.eventmesh.dashboard.common.model.ConvertMetaData;
import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;

import org.junit.Test;

public class RuntimeConvertMetaDataTest {


    @Test
    public void test() {
        ConvertMetaData<RuntimeEntity, RuntimeMetadata> convertMetaData = RuntimeConvertMetaData.INSTANCE;
        RuntimeEntity runtimeEntity = new RuntimeEntity();
        runtimeEntity.setId(1L);
        runtimeEntity.setName("test");
        runtimeEntity.setClusterId(2L);
        RuntimeMetadata runtimeMetadata = convertMetaData.toMetaData(runtimeEntity);
        System.out.println(runtimeMetadata);
    }
}
