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

package org.apache.eventmesh.dashboard.console.function.metadata.util.convert.metadata;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;
import org.apache.eventmesh.dashboard.console.cache.ClusterCacheBase;
import org.apache.eventmesh.dashboard.console.entity.runtime.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.function.metadata.util.convert.Converter;

import org.junit.jupiter.api.Test;

class RuntimeMetadata2EntityConverterTest extends ClusterCacheBase {

    private static final Converter<RuntimeMetadata, RuntimeEntity> converter = new RuntimeMetadata2EntityConverter();

    @Test
    public void testConvert() {
        RuntimeMetadata runtimeMetadata = new RuntimeMetadata();
        runtimeMetadata.setRack("rack");
        runtimeMetadata.setPort(1019);
        runtimeMetadata.setHost("runtime1");
        runtimeMetadata.setClusterName("cl1");
        runtimeMetadata.setJmxPort(1099);
        runtimeMetadata.setEndpointMap(null);
        RuntimeEntity runtimeEntity = converter.convert(runtimeMetadata);
        assertNotNull(runtimeEntity);
    }

}