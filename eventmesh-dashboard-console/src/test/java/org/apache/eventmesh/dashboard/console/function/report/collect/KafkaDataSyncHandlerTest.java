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

package org.apache.eventmesh.dashboard.console.function.report.collect;

import org.apache.eventmesh.dashboard.common.model.metadata.ClusterMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.CollectMetadata;
import org.apache.eventmesh.dashboard.console.function.report.ReportEngine;
import org.apache.eventmesh.dashboard.console.function.report.collect.exporter.KafkaAdminCollect;
import org.apache.eventmesh.dashboard.console.function.report.collect.padding.PaddingService;
import org.apache.eventmesh.dashboard.console.function.report.iotdb.IotDBReportEngine;
import org.apache.eventmesh.dashboard.console.function.report.model.kafka.KafkaClusterMetrics;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import com.alibaba.druid.pool.DruidDataSource;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class KafkaDataSyncHandlerTest {

    @Test
    public void writesAllSamplesAfterReturningTheCollectorBuffer() {
        ReportEngine engine = mock(ReportEngine.class);
        AtomicReference<Map<Class<?>, List<Object>>> written = new AtomicReference<>();
        doAnswer(invocation -> {
            written.set(invocation.getArgument(0));
            return null;
        }).when(engine).batchInsertByClass(anyMap());
        KafkaDataSyncHandler handler = new KafkaDataSyncHandler(mock(PaddingService.class), engine);
        RestoreData result = result(7L);
        KafkaClusterMetrics second = new KafkaClusterMetrics();
        second.setValueBrokerCount(8L);
        result.setData(second);
        handler.getDataSyncHandlerWrapper(1).sync(result);
        assertTrue(result.getDataMap().isEmpty());
        assertEquals(2, written.get().get(KafkaClusterMetrics.class).size());
        assertEquals(7L, ((KafkaClusterMetrics) written.get().get(KafkaClusterMetrics.class).get(0)).getValueBrokerCount());
        assertEquals(8L, ((KafkaClusterMetrics) written.get().get(KafkaClusterMetrics.class).get(1)).getValueBrokerCount());
    }

    @Test
    public void independentConcurrentCollectorsDeliverEverySample() {
        ReportEngine engine = mock(ReportEngine.class);
        Set<Long> written = ConcurrentHashMap.newKeySet();
        doAnswer(invocation -> {
            Map<Class<?>, List<Object>> samples = invocation.getArgument(0);
            written.add(((KafkaClusterMetrics) samples.get(KafkaClusterMetrics.class).get(0)).getValueBrokerCount());
            return null;
        }).when(engine).batchInsertByClass(anyMap());
        KafkaDataSyncHandler handler = new KafkaDataSyncHandler(mock(PaddingService.class), engine);
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < 64; i++) {
            long value = i;
            DataSyncHandler.DataSyncHandlerWrapper wrapper = handler.getDataSyncHandlerWrapper(1);
            futures.add(CompletableFuture.runAsync(() -> wrapper.sync(result(value))));
        }
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
        assertEquals(64, written.size());
        verify(engine, times(64)).batchInsertByClass(anyMap());
    }

    @Test
    public void failedIotdbWriteStillReleasesCollectorBuffer() throws Exception {
        IotDBReportEngine engine = new IotDBReportEngine();
        DruidDataSource source = mock(DruidDataSource.class);
        when(source.getConnection()).thenThrow(new SQLException("write failed"));
        ReflectionTestUtils.setField(engine, "dataSource", source);
        KafkaDataSyncHandler handler = new KafkaDataSyncHandler(mock(PaddingService.class), engine);
        RestoreData result = result(7L);

        assertThrows(IllegalStateException.class, () -> handler.getDataSyncHandlerWrapper(1).sync(result));
        assertTrue(result.getDataMap().isEmpty());
    }

    @Test
    public void kafkaCollectorUsesIndependentBuffersAfterReturn() {
        KafkaAdminCollect collector = new KafkaAdminCollect() {
            private long count;
            @Override
            protected void doCollect() {
                KafkaClusterMetrics sample = new KafkaClusterMetrics();
                sample.setValueBrokerCount(++count);
                setData(sample);
            }
        };
        ClusterMetadata cluster = new ClusterMetadata();
        cluster.setId(1L);
        cluster.setOrganizationId(1L);
        collector.setClusterMetadata(cluster);
        collector.setCollectMetadata(new CollectMetadata());
        List<RestoreData> results = new ArrayList<>();
        DataSyncHandler.DataSyncHandlerWrapper wrapper = mock(DataSyncHandler.DataSyncHandlerWrapper.class);
        doAnswer(invocation -> {
            RestoreData result = invocation.getArgument(0);
            results.add(result);
            if (results.size() == 1) {
                result.restore();
            }
            return null;
        }).when(wrapper).sync(any(RestoreData.class));
        collector.collect(0, wrapper);
        collector.collect(0, wrapper);
        collector.collect(0, wrapper);
        assertNotSame(results.get(1), results.get(2));
        assertEquals(2L, ((KafkaClusterMetrics) results.get(1).getDataMap().get(KafkaClusterMetrics.class).get(0))
            .getValueBrokerCount());
    }

    private RestoreData result(long value) {
        RestoreData result = new RestoreData();
        result.setIndex(0);
        result.setAbstractCollect(mock(AbstractCollect.class));
        KafkaClusterMetrics sample = new KafkaClusterMetrics();
        sample.setValueBrokerCount(value);
        result.setData(sample);
        return result;
    }
}
