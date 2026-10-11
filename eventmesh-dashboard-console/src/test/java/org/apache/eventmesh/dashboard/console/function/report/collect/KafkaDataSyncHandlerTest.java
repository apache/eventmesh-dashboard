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

import org.apache.eventmesh.dashboard.console.function.report.ReportEngine;
import org.apache.eventmesh.dashboard.console.function.report.collect.padding.PaddingService;
import org.apache.eventmesh.dashboard.console.function.report.model.kafka.KafkaClusterMetrics;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

public class KafkaDataSyncHandlerTest {

    @Test
    public void outOfOrderResultsAreDeliveredAsAnIterableSampleList() {
        ReportEngine engine = mock(ReportEngine.class);
        AtomicReference<Map<Class<?>, List<Object>>> written = new AtomicReference<>();
        doAnswer(invocation -> {
            written.set(invocation.getArgument(0));
            return null;
        }).when(engine).batchInsertByClass(anyMap());
        DataSyncHandler handler = new DataSyncHandler();
        handler.setReportEngine(engine);
        handler.setPaddingService(mock(PaddingService.class));
        DataSyncHandler.DataSyncHandlerWrapper wrapper = handler.getDataSyncHandlerWrapper(2);
        wrapper.sync(result(1, 2L));
        wrapper.sync(result(0, 1L));
        List<Object> samples = written.get().get(KafkaClusterMetrics.class);
        assertEquals(2, samples.size());
        assertEquals(2, samples.stream().count());
        assertEquals(Long.valueOf(2), ((KafkaClusterMetrics) samples.iterator().next()).getValueBrokerCount());
    }

    private RestoreData result(int index, long count) {
        RestoreData result = new RestoreData();
        result.setIndex(index);
        result.setAbstractCollect(mock(AbstractCollect.class));
        KafkaClusterMetrics sample = new KafkaClusterMetrics();
        sample.setValueBrokerCount(count);
        result.setData(sample);
        return result;
    }
}
