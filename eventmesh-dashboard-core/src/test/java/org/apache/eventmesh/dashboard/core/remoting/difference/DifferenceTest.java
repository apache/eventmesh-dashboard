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

package org.apache.eventmesh.dashboard.core.remoting.difference;

import org.apache.eventmesh.dashboard.common.model.base.BaseClusterIdBase;
import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;
import org.apache.eventmesh.dashboard.core.metadata.DataMetadataHandler;
import org.apache.eventmesh.dashboard.core.metadata.difference.BufferDifference;
import org.apache.eventmesh.dashboard.core.metadata.difference.NotDifference;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class DifferenceTest {

    List<BaseClusterIdBase> baseClusterIdBases = new ArrayList<>();

    List<BaseClusterIdBase> baseClusterIdBaseList = new ArrayList<>();

    @Before
    public void before() throws Exception {
        for (long i = 5; i < 10; i++) {
            RuntimeMetadata runtimeMetadata = new RuntimeMetadata();
            runtimeMetadata.setId(i);
            baseClusterIdBases.add(runtimeMetadata);
        }

        for (long i = 2; i < 7; i++) {
            RuntimeMetadata runtimeMetadata = new RuntimeMetadata();
            runtimeMetadata.setId(i);
            baseClusterIdBaseList.add(runtimeMetadata);
        }
        for (long i = 9; i < 15; i++) {
            RuntimeMetadata runtimeMetadata = new RuntimeMetadata();
            runtimeMetadata.setId(i);
            baseClusterIdBaseList.add(runtimeMetadata);
        }
    }


    @Test
    public void test_NotDifference() {
        NotDifference difference = new NotDifference();
        difference.setSourceHandler(new TestDataMetadataHandler() {
            @Override
            public List<BaseClusterIdBase> getData() {
                return baseClusterIdBases;
            }
        });
        difference.setTargetHandler(new TestDataMetadataHandler() {
            @Override
            public void handleAll(Collection<BaseClusterIdBase> allData, List<BaseClusterIdBase> addData, List<BaseClusterIdBase> updateData,
                List<BaseClusterIdBase> deleteData) {
                Assert.assertEquals(baseClusterIdBases, addData);
            }
        });
        difference.difference();
    }


    @Test
    public void test_BufferDifference() {
        AtomicInteger atomicInteger = new AtomicInteger();
        BufferDifference difference = new BufferDifference();
        difference.setSourceHandler(new TestDataMetadataHandler() {
            @Override
            public List<BaseClusterIdBase> getData() {
                return atomicInteger.get() == 0 ? baseClusterIdBases : baseClusterIdBaseList;
            }
        });
        difference.setTargetHandler(new TestDataMetadataHandler() {
            @Override
            public void handleAll(Collection<BaseClusterIdBase> allData, List<BaseClusterIdBase> addData, List<BaseClusterIdBase> updateData,
                List<BaseClusterIdBase> deleteData) {
                if (atomicInteger.get() == 0) {
                    Assert.assertEquals(baseClusterIdBases, addData);
                } else {
                    Assert.assertEquals(2, deleteData.size());
                }
            }
        });
        difference.difference();
        atomicInteger.incrementAndGet();
        difference.difference();
    }


    static class TestDataMetadataHandler implements DataMetadataHandler<BaseClusterIdBase> {

        @Override
        public void handleAll(Collection<BaseClusterIdBase> allData, List<BaseClusterIdBase> addData, List<BaseClusterIdBase> updateData,
            List<BaseClusterIdBase> deleteData) {

        }

        @Override
        public List<BaseClusterIdBase> getData() {
            return List.of();
        }
    }
}
