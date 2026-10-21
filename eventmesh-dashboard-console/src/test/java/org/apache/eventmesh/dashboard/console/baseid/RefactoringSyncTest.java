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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.model.base.BaseClusterIdBase;
import org.apache.eventmesh.dashboard.common.model.metadata.TopicMetadata;
import org.apache.eventmesh.dashboard.console.entity.message.TopicEntity;
import org.apache.eventmesh.dashboard.console.mapper.message.TopicMapper;
import org.apache.eventmesh.dashboard.console.service.metadata.TopicDataMetadataHandler;
import org.apache.eventmesh.dashboard.core.metadata.DataMetadataHandler;
import org.apache.eventmesh.dashboard.core.metadata.difference.BodyDataDifference;
import org.apache.eventmesh.dashboard.core.metadata.difference.BufferDifference;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

public class RefactoringSyncTest {

    @Test
    public void bodyDifferenceClassifiesTombstoneBeforeComparingTimestamps() {
        TopicMetadata tombstone = topic("orders");
        tombstone.setIsDelete(1);
        TopicMetadata cached = topic("orders");
        cached.setUpdateTime(cached.getCreateTime().plusSeconds(1));

        DataMetadataHandler<BaseClusterIdBase> source = mock(DataMetadataHandler.class);
        DataMetadataHandler<BaseClusterIdBase> remote = mock(DataMetadataHandler.class);
        when(source.getData()).thenReturn(List.of(tombstone));
        AtomicReference<List<BaseClusterIdBase>> deletedRows = new AtomicReference<>();
        doAnswer(invocation -> {
            deletedRows.set(new ArrayList<>(invocation.getArgument(3)));
            return null;
        }).when(remote).handleAll(any(), any(), any(), any());
        Map<String, BaseClusterIdBase> cache = new HashMap<>();
        cache.put(cached.nodeUnique(), cached);
        BodyDataDifference difference = new BodyDataDifference();
        difference.setAllData(cache);
        difference.setSourceHandler(source);
        difference.setTargetHandler(remote);

        difference.difference();

        assertFalse(cache.containsKey("orders"));
        assertEquals(List.of(tombstone), deletedRows.get());
    }

    @Test
    public void bufferDifferenceUsesNodeNameIdentityForTombstonesWithoutIds() {
        TopicMetadata live = topic("new-topic");
        live.setId(null);
        TopicMetadata tombstone = topic("removed-topic");
        tombstone.setId(null);
        tombstone.setIsDelete(1);
        TopicMetadata cached = topic("removed-topic");
        cached.setId(null);

        DataMetadataHandler<BaseClusterIdBase> source = mock(DataMetadataHandler.class);
        DataMetadataHandler<BaseClusterIdBase> remote = mock(DataMetadataHandler.class);
        when(source.getData()).thenReturn(List.of(live, tombstone));
        AtomicReference<List<BaseClusterIdBase>> addedRows = new AtomicReference<>();
        AtomicReference<List<BaseClusterIdBase>> deletedRows = new AtomicReference<>();
        doAnswer(invocation -> {
            addedRows.set(new ArrayList<>(invocation.getArgument(1)));
            deletedRows.set(new ArrayList<>(invocation.getArgument(3)));
            return null;
        }).when(remote).handleAll(any(), any(), any(), any());
        Map<String, BaseClusterIdBase> cache = new HashMap<>();
        cache.put(cached.nodeUnique(), cached);
        BufferDifference difference = new BufferDifference();
        difference.setAllData(cache);
        difference.setSourceHandler(source);
        difference.setTargetHandler(remote);

        difference.difference();

        assertEquals(Map.of(live.nodeUnique(), live), cache);
        assertEquals(List.of(live), addedRows.get());
        assertEquals(List.of(tombstone), deletedRows.get());
    }

    @Test
    public void bufferDifferenceTombstoneOnlyBatchKeepsUnrelatedCachedRows() {
        TopicMetadata tombstone = topic("removed-topic");
        tombstone.setId(null);
        tombstone.setIsDelete(1);
        TopicMetadata cachedTombstone = topic("removed-topic");
        cachedTombstone.setId(null);
        TopicMetadata unrelated = topic("unrelated-topic");
        unrelated.setId(null);

        DataMetadataHandler<BaseClusterIdBase> source = mock(DataMetadataHandler.class);
        DataMetadataHandler<BaseClusterIdBase> remote = mock(DataMetadataHandler.class);
        when(source.getData()).thenReturn(List.of(tombstone));
        AtomicReference<List<BaseClusterIdBase>> deletedRows = new AtomicReference<>();
        doAnswer(invocation -> {
            deletedRows.set(new ArrayList<>(invocation.getArgument(3)));
            return null;
        }).when(remote).handleAll(any(), any(), any(), any());
        Map<String, BaseClusterIdBase> cache = new HashMap<>();
        cache.put(cachedTombstone.nodeUnique(), cachedTombstone);
        cache.put(unrelated.nodeUnique(), unrelated);
        BufferDifference difference = new BufferDifference();
        difference.setAllData(cache);
        difference.setSourceHandler(source);
        difference.setTargetHandler(remote);

        difference.difference();

        assertEquals(Map.of(unrelated.nodeUnique(), unrelated), cache);
        assertEquals(List.of(tombstone), deletedRows.get());
    }

    @Test
    public void bufferDifferenceKeepsInactiveButNotDeletedResources() {
        TopicMetadata inactive = topic("inactive-topic");
        inactive.setStatus(0L);
        TopicMetadata deleted = topic("deleted-topic");
        deleted.setIsDelete(1);

        DataMetadataHandler<BaseClusterIdBase> source = mock(DataMetadataHandler.class);
        DataMetadataHandler<BaseClusterIdBase> remote = mock(DataMetadataHandler.class);
        when(source.getData()).thenReturn(List.of(inactive, deleted));
        AtomicReference<List<BaseClusterIdBase>> addedRows = new AtomicReference<>();
        AtomicReference<List<BaseClusterIdBase>> deletedRows = new AtomicReference<>();
        doAnswer(invocation -> {
            addedRows.set(new ArrayList<>(invocation.getArgument(1)));
            deletedRows.set(new ArrayList<>(invocation.getArgument(3)));
            return null;
        }).when(remote).handleAll(any(), any(), any(), any());
        Map<String, BaseClusterIdBase> cache = new HashMap<>();
        BufferDifference difference = new BufferDifference();
        difference.setAllData(cache);
        difference.setSourceHandler(source);
        difference.setTargetHandler(remote);

        difference.difference();

        assertTrue(cache.containsKey(inactive.nodeUnique()));
        assertFalse(cache.containsKey(deleted.nodeUnique()));
        assertEquals(List.of(inactive), addedRows.get());
        assertEquals(List.of(deleted), deletedRows.get());
    }

    @Test
    public void bufferDifferenceMatchesAndRemovesResourcesByNameAgainstNonemptyCache() {
        TopicMetadata matchingSource = topic("matching-topic");
        matchingSource.setId(null);
        TopicMetadata added = topic("added-topic");
        added.setId(null);
        TopicMetadata matchingCached = topic("matching-topic");
        matchingCached.setId(null);
        TopicMetadata removedCached = topic("removed-topic");
        removedCached.setId(null);

        DataMetadataHandler<BaseClusterIdBase> source = mock(DataMetadataHandler.class);
        DataMetadataHandler<BaseClusterIdBase> remote = mock(DataMetadataHandler.class);
        when(source.getData()).thenReturn(List.of(matchingSource, added));
        AtomicReference<List<BaseClusterIdBase>> addedRows = new AtomicReference<>();
        AtomicReference<List<BaseClusterIdBase>> deletedRows = new AtomicReference<>();
        doAnswer(invocation -> {
            addedRows.set(new ArrayList<>(invocation.getArgument(1)));
            deletedRows.set(new ArrayList<>(invocation.getArgument(3)));
            return null;
        }).when(remote).handleAll(any(), any(), any(), any());
        Map<String, BaseClusterIdBase> cache = new HashMap<>();
        cache.put(matchingCached.nodeUnique(), matchingCached);
        cache.put(removedCached.nodeUnique(), removedCached);
        BufferDifference difference = new BufferDifference();
        difference.setAllData(cache);
        difference.setSourceHandler(source);
        difference.setTargetHandler(remote);

        difference.difference();

        assertEquals(List.of(added), addedRows.get());
        assertEquals(List.of(removedCached), deletedRows.get());
        assertEquals(Map.of(matchingSource.nodeUnique(), matchingSource, added.nodeUnique(), added), cache);
    }

    @Test
    public void databaseReadFailureLeavesCursorAndSuccessfulReadAdvancesIt() {
        TopicDataMetadataHandler source = new TopicDataMetadataHandler();
        TopicMapper mapper = mock(TopicMapper.class);
        TopicEntity criteria = new TopicEntity();
        LocalDateTime original = LocalDateTime.of(2020, 1, 1, 0, 0);
        criteria.setUpdateTime(original);
        ReflectionTestUtils.setField(source, "baseRuntimeIdBase", criteria);
        ReflectionTestUtils.setField(source, "syncDataHandlerMapper", mapper);
        when(mapper.syncGet(criteria)).thenThrow(new IllegalStateException("db offline")).thenReturn(List.of(new TopicEntity()));

        assertThrows(IllegalStateException.class, source::getData);
        assertEquals(original, criteria.getUpdateTime());
        assertEquals(1, source.getData().size());
        assertTrue(criteria.getUpdateTime().isAfter(original));
        assertEquals(0, criteria.getUpdateTime().getNano());
    }

    @Test
    public void bodyDifferenceKeepsInactiveRowsAndRejectsInvalidLiveRows() {
        TopicMetadata inactive = topic("inactive-topic");
        inactive.setStatus(0L);
        DataMetadataHandler<BaseClusterIdBase> source = mock(DataMetadataHandler.class);
        DataMetadataHandler<BaseClusterIdBase> remote = mock(DataMetadataHandler.class);
        when(source.getData()).thenReturn(List.of(inactive));
        BodyDataDifference difference = new BodyDataDifference();
        difference.setSourceHandler(source);
        difference.setTargetHandler(remote);

        ReflectionTestUtils.invokeMethod(difference, "doDifference");

        assertEquals(List.of(inactive), difference.getData());
        assertEquals(List.of(inactive), ReflectionTestUtils.getField(difference, "insertData"));

        TopicMetadata missingName = topic("missing-name");
        missingName.setName(null);
        assertBodyDifferenceRejects(missingName);
        TopicMetadata missingStatus = topic("missing-status");
        missingStatus.setStatus(null);
        assertBodyDifferenceRejects(missingStatus);
        TopicMetadata missingTimestamp = topic("missing-timestamp");
        missingTimestamp.setUpdateTime(null);
        assertBodyDifferenceRejects(missingTimestamp);
    }

    @Test
    public void bodyDifferenceTreatsNullAndEmptySourceAsNoChanges() {
        TopicMetadata cached = topic("cached-topic");
        Map<String, BaseClusterIdBase> cache = new HashMap<>();
        cache.put(cached.nodeUnique(), cached);
        DataMetadataHandler<BaseClusterIdBase> source = mock(DataMetadataHandler.class);
        BodyDataDifference difference = new BodyDataDifference();
        difference.setAllData(cache);
        difference.setSourceHandler(source);

        when(source.getData()).thenReturn(null, List.of());
        ReflectionTestUtils.invokeMethod(difference, "doDifference");
        ReflectionTestUtils.invokeMethod(difference, "doDifference");

        assertEquals(Map.of(cached.nodeUnique(), cached), cache);
        assertTrue(((List<?>) ReflectionTestUtils.getField(difference, "insertData")).isEmpty());
        assertTrue(((List<?>) ReflectionTestUtils.getField(difference, "updateData")).isEmpty());
        assertTrue(((List<?>) ReflectionTestUtils.getField(difference, "deleteData")).isEmpty());
    }

    @Test
    public void bodyDifferenceClassifiesExistingLaterTimestampAsUpdateOnly() {
        TopicMetadata existing = topic("existing-topic");
        TopicMetadata updated = topic("existing-topic");
        updated.setUpdateTime(updated.getCreateTime().plusSeconds(5));
        Map<String, BaseClusterIdBase> cache = new HashMap<>();
        cache.put(existing.nodeUnique(), existing);
        DataMetadataHandler<BaseClusterIdBase> source = mock(DataMetadataHandler.class);
        when(source.getData()).thenReturn(List.of(updated));
        BodyDataDifference difference = new BodyDataDifference();
        difference.setAllData(cache);
        difference.setSourceHandler(source);

        ReflectionTestUtils.invokeMethod(difference, "doDifference");

        assertEquals(Map.of(updated.nodeUnique(), updated), cache);
        assertTrue(((List<?>) ReflectionTestUtils.getField(difference, "insertData")).isEmpty());
        assertEquals(List.of(updated), ReflectionTestUtils.getField(difference, "updateData"));
        assertTrue(((List<?>) ReflectionTestUtils.getField(difference, "deleteData")).isEmpty());
    }

    private static void assertBodyDifferenceRejects(TopicMetadata invalid) {
        DataMetadataHandler<BaseClusterIdBase> source = mock(DataMetadataHandler.class);
        when(source.getData()).thenReturn(List.of(invalid));
        BodyDataDifference difference = new BodyDataDifference();
        difference.setSourceHandler(source);

        assertThrows(IllegalArgumentException.class, () -> ReflectionTestUtils.invokeMethod(difference, "doDifference"));
    }

    private static TopicMetadata topic(String name) {
        TopicMetadata metadata = new TopicMetadata();
        metadata.setId(1L);
        metadata.setClusterId(10L);
        metadata.setClusterType(ClusterType.STORAGE_KAFKA_BROKER);
        metadata.setName(name);
        metadata.setStatus(1L);
        metadata.setCreateTime(LocalDateTime.of(2026, 10, 21, 0, 0));
        metadata.setUpdateTime(metadata.getCreateTime());
        return metadata;
    }
}
