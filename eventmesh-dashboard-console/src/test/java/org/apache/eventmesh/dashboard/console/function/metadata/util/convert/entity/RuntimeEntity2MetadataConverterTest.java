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

package org.apache.eventmesh.dashboard.console.function.metadata.util.convert.entity;

import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;
import org.apache.eventmesh.dashboard.console.entity.runtime.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.function.metadata.util.convert.Converter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RuntimeEntity2MetadataConverterTest {

    static Converter<RuntimeEntity, RuntimeMetadata> converter = new RuntimeEntity2MetadataConverter();

    @BeforeAll
    public static void init() {
        converter = new RuntimeEntity2MetadataConverter();
    }

    @Test
    public void testConvert() {
        RuntimeEntity runtimeEntity = new RuntimeEntity(1L, "runtime1", 2L, 1019, 1099, 1L, "null", 1, null, null, "null");
        RuntimeMetadata runtimeMetadata = converter.convert(runtimeEntity);
        Assertions.assertNotNull(runtimeMetadata);
    }
}