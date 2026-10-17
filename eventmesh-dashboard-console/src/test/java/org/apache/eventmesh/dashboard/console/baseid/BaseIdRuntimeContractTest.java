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


package org.apache.eventmesh.dashboard.console.baseid;

import org.apache.eventmesh.dashboard.console.entity.message.TopicEntity;
import org.apache.eventmesh.dashboard.console.mapper.SyncDataHandlerMapper;
import org.apache.eventmesh.dashboard.console.service.metadata.AbstractDBDataMetadataHandler;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class BaseIdRuntimeContractTest {
    @Test
    @SuppressWarnings("unchecked")
    public void databaseQueryFailurePreservesCursorAndRetryReadsTombstone() {
        TopicEntity cursor = new TopicEntity();
        LocalDateTime old = LocalDateTime.of(2026, 1, 1, 0, 0);
        cursor.setUpdateTime(old);
        TopicEntity tombstone = new TopicEntity();
        tombstone.setIsDelete(1);
        SyncDataHandlerMapper<TopicEntity> mapper = mock(SyncDataHandlerMapper.class);
        when(mapper.syncGet(cursor)).thenThrow(new IllegalStateException("read failure")).thenAnswer(call -> {
            assertEquals(old, cursor.getUpdateTime());
            return List.of(tombstone);
        });
        TopicHandler handler = new TopicHandler();
        ReflectionTestUtils.setField(handler, "baseRuntimeIdBase", cursor);
        ReflectionTestUtils.setField(handler, "syncDataHandlerMapper", mapper);
        assertThrows(IllegalStateException.class, handler::getData);
        assertEquals(old, cursor.getUpdateTime());
        assertSame(tombstone, handler.getData().get(0));
        assertTrue(cursor.getUpdateTime().isAfter(old));
        assertEquals(0, cursor.getUpdateTime().getNano());
    }

    static class TopicHandler extends AbstractDBDataMetadataHandler<TopicEntity> {
    }
}
