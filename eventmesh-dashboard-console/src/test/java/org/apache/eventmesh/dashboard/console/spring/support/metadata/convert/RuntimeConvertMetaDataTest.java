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
