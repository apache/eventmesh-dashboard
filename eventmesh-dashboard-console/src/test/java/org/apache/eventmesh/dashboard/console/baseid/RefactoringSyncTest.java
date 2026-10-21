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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.enums.MetadataType;
import org.apache.eventmesh.dashboard.common.model.ConvertMetaData;
import org.apache.eventmesh.dashboard.common.model.remoting.Global2Request;
import org.apache.eventmesh.dashboard.common.model.base.BaseClusterIdBase;
import org.apache.eventmesh.dashboard.common.model.metadata.ClusterMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.TopicMetadata;
import org.apache.eventmesh.dashboard.console.entity.message.TopicEntity;
import org.apache.eventmesh.dashboard.console.mapper.message.TopicMapper;
import org.apache.eventmesh.dashboard.console.service.metadata.TopicDataMetadataHandler;
import org.apache.eventmesh.dashboard.core.cluster.ClusterDO;
import org.apache.eventmesh.dashboard.core.cluster.ColonyDO;
import org.apache.eventmesh.dashboard.core.metadata.DataMetadataHandler;
import org.apache.eventmesh.dashboard.core.metadata.MetadataHandler;
import org.apache.eventmesh.dashboard.core.metadata.SyncMetadataCreateFactory;
import org.apache.eventmesh.dashboard.core.metadata.difference.BufferDifference;
import org.apache.eventmesh.dashboard.core.metadata.difference.BodyDataDifference;
import org.apache.eventmesh.dashboard.core.metadata.difference.BothNotCacheDifference;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

public class RefactoringSyncTest {

    @Test
    public void queryCursorMovesOnlyOnAcknowledgementAndFailureRequeries() {
        TopicMapper mapper = mock(TopicMapper.class);
        TopicDataMetadataHandler source = new TopicDataMetadataHandler();
        TopicEntity criteria = new TopicEntity();
        LocalDateTime original = LocalDateTime.of(2020, 1, 1, 0, 0);
        criteria.setUpdateTime(original);
        ReflectionTestUtils.setField(source, "baseRuntimeIdBase", criteria);
        ReflectionTestUtils.setField(source, "syncDataHandlerMapper", mapper);
        when(mapper.syncGet(criteria)).thenThrow(new IllegalStateException("db offline")).thenReturn(List.of(new TopicEntity()));
        assertThrows(IllegalStateException.class, source::getData);
        assertEquals(original, criteria.getUpdateTime());
        var first = source.getData();
        assertSame(first, source.getData());
        assertEquals(original, criteria.getUpdateTime());
        source.retryData();
        source.getData();
        verify(mapper, times(3)).syncGet(criteria);
        source.acknowledgeData();
        assertTrue(criteria.getUpdateTime().isAfter(original));
        assertEquals(0, criteria.getUpdateTime().getNano());
    }

    @Test
    public void invalidSecondRowDoesNotPublishFirstRowOrAcknowledgeBatch() {
        Fixture f = new Fixture();
        Object bad = new Object();
        when(f.source.getData()).thenReturn(List.of(f.row, bad));
        when(f.converter.toMetaData(bad)).thenThrow(new IllegalArgumentException("bad row"));
        assertThrows(IllegalArgumentException.class, f.factory::loadData);
        verify(f.source, never()).acknowledgeData();
        verify(f.source).retryData();
        assertTrue(f.target.getData().isEmpty());
    }

    @Test
    public void successfulLoadAcknowledgesTheSourceOnceWithoutRetryingIt() {
        Fixture f = new Fixture();
        f.factory.loadData();
        verify(f.source, times(1)).acknowledgeData();
        verify(f.source, never()).retryData();
    }

    @Test
    public void missingTargetRetainsDatabaseCheckpoint() {
        Fixture f = new Fixture();
        f.metadata.setClusterId(999L);
        assertThrows(IllegalStateException.class, f.factory::loadData);
        verify(f.source, never()).acknowledgeData();
        verify(f.source).retryData();
    }

    @Test
    public void invalidLiveRowsDoNotPublishOrAcknowledgeAndRetryTheBatch() {
        Fixture f = new Fixture();
        f.metadata.setStatus(null);
        assertThrows(IllegalArgumentException.class, f.factory::loadData);
        f.metadata.setStatus(1L);
        f.metadata.setCreateTime(null);
        assertThrows(IllegalArgumentException.class, f.factory::loadData);
        f.metadata.setCreateTime(LocalDateTime.of(2026, 10, 21, 0, 0));
        f.metadata.setUpdateTime(null);
        assertThrows(IllegalArgumentException.class, f.factory::loadData);
        f.metadata.setUpdateTime(f.metadata.getCreateTime());
        f.metadata.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE);
        assertThrows(IllegalArgumentException.class, f.factory::loadData);

        assertTrue(f.target.getData().isEmpty());
        verify(f.source, never()).acknowledgeData();
        verify(f.source, times(4)).retryData();
    }

    @Test
    public void missingRuntimeRowDoesNotPublishOrAcknowledgeTheBatch() {
        RuntimeFixture f = new RuntimeFixture();
        f.firstMetadata.setRuntimeId(null);

        assertThrows(IllegalStateException.class, f.factory::loadData);
        f.firstMetadata.setRuntimeId(999L);
        assertThrows(IllegalStateException.class, f.factory::loadData);

        assertTrue(f.firstTarget.getData().isEmpty());
        verify(f.source, never()).acknowledgeData();
        verify(f.source, times(2)).retryData();
    }

    @Test
    public void nullClusterTypeIsInferredFromLoadedColonyBeforeEnqueue() {
        Fixture f = new Fixture();
        f.metadata.setClusterType(null);

        f.factory.loadData();

        assertEquals(ClusterType.STORAGE_KAFKA_BROKER, f.target.getData().get(0).getClusterType());
        verify(f.source).acknowledgeData();
    }

    @Test
    public void repeatedLoadsDeduplicateAndOldAckCannotEraseNewerVersion() {
        Fixture f = new Fixture();
        f.factory.loadData();
        f.factory.loadData();
        assertEquals(1, f.target.getData().size());
        TopicMetadata replacement = topic();
        replacement.setName("new-version");
        when(f.converter.toMetaData(f.row)).thenReturn(replacement);
        f.factory.loadData();
        f.target.acknowledgeData();
        assertSame(replacement, f.target.getData().get(0));
        f.target.acknowledgeData();
        assertTrue(f.target.getData().isEmpty());
    }

    @Test
    public void acknowledgedOverlapIsNotSentAgainButUnregisterClearsItsHistory() {
        Fixture f = new Fixture();
        f.factory.loadData();
        f.target.getData();
        f.target.acknowledgeData();
        TopicMetadata sameContent = topic();
        sameContent.setUpdateTime(sameContent.getUpdateTime().plusSeconds(2));
        when(f.converter.toMetaData(f.row)).thenReturn(sameContent);
        f.factory.loadData();
        assertTrue(f.target.getData().isEmpty());
        f.target.acknowledgeData();
        ClusterMetadata target = new ClusterMetadata();
        target.setId(10L);
        f.factory.forgetTarget(target);
        f.factory.loadData();
        assertEquals(1, f.target.getData().size());
    }

    @Test
    public void equivalentReloadDuringRpcDoesNotCreateAnotherPendingWrite() {
        Fixture f = new Fixture();
        f.factory.loadData();
        f.target.getData();
        when(f.converter.toMetaData(f.row)).thenReturn(topic());
        f.factory.loadData();
        f.target.acknowledgeData();
        assertTrue(f.target.getData().isEmpty());
    }

    @Test
    public void correctedDesiredStateCanReplaceFailedRemoteBatch() {
        Fixture f = new Fixture();
        f.factory.loadData();
        DataMetadataHandler<BaseClusterIdBase> remote = mock(DataMetadataHandler.class);
        doThrow(new IllegalStateException("invalid desired config")).doNothing().when(remote).handleAll(any(), anyList(), anyList(), anyList());
        BodyDataDifference difference = new BodyDataDifference();
        difference.setSourceHandler(f.target);
        difference.setTargetHandler(remote);
        assertThrows(IllegalStateException.class, difference::difference);
        TopicMetadata corrected = topic();
        corrected.setName("corrected");
        when(f.converter.toMetaData(f.row)).thenReturn(corrected);
        f.factory.loadData();
        difference.difference();
        verify(remote).handleAll(any(), eq(List.of(corrected)), argThat(List::isEmpty), argThat(List::isEmpty));
        assertTrue(f.target.getData().isEmpty());
    }

    @Test
    public void failedRemoteApplyRestoresCacheAndRetriesPendingBatch() {
        Fixture f = new Fixture();
        f.factory.loadData();
        DataMetadataHandler<BaseClusterIdBase> remote = mock(DataMetadataHandler.class);
        doThrow(new IllegalStateException("broker offline")).doNothing().when(remote).handleAll(any(), anyList(), anyList(), anyList());
        BodyDataDifference difference = new BodyDataDifference();
        Map<String, BaseClusterIdBase> cache = new HashMap<>();
        TopicMetadata existing = topic();
        existing.setName("existing");
        existing.setUpdateTime(existing.getCreateTime().plusSeconds(1));
        cache.put(existing.nodeUnique(), existing);
        difference.setAllData(cache);
        difference.setSourceHandler(f.target);
        difference.setTargetHandler(remote);
        assertThrows(IllegalStateException.class, difference::difference);
        assertEquals(Map.of("existing", existing), cache);
        assertEquals(1, f.target.getData().size());
        difference.difference();
        assertSame(existing, cache.get("existing"));
        assertTrue(cache.containsKey("orders"));
        assertTrue(f.target.getData().isEmpty());
        verify(remote, times(2)).handleAll(any(), argThat(rows -> rows.size() == 1), argThat(List::isEmpty), argThat(List::isEmpty));
    }

    @Test
    public void bufferDifferenceDeletesTombstoneOnceAndUsesNameIdentityWithoutDatabaseIds() {
        TopicMetadata live = topic();
        live.setId(null);
        live.setName("new-without-id");
        TopicMetadata tombstone = topic();
        tombstone.setId(null);
        tombstone.setName("removed-without-id");
        tombstone.setIsDelete(1);
        TopicMetadata cached = topic();
        cached.setId(null);
        cached.setName("removed-without-id");

        DataMetadataHandler<BaseClusterIdBase> source = mock(DataMetadataHandler.class);
        DataMetadataHandler<BaseClusterIdBase> remote = mock(DataMetadataHandler.class);
        when(source.getData()).thenReturn(List.of(live, tombstone));
        doThrow(new IllegalStateException("broker offline")).doNothing().when(remote)
            .handleAll(any(), anyList(), anyList(), anyList());
        Map<String, BaseClusterIdBase> cache = new HashMap<>();
        cache.put(cached.nodeUnique(), cached);
        Map<String, BaseClusterIdBase> originalCache = new HashMap<>(cache);
        BufferDifference difference = new BufferDifference();
        difference.setAllData(cache);
        difference.setSourceHandler(source);
        difference.setTargetHandler(remote);

        assertThrows(IllegalStateException.class, difference::difference);
        assertEquals(originalCache, cache);
        verify(source).retryData();

        difference.difference();

        verify(remote, times(2)).handleAll(eq(List.of(live)), eq(List.of(live)), argThat(List::isEmpty), eq(List.of(tombstone)));
        verify(source).acknowledgeData();
        verify(source, times(1)).retryData();
        assertEquals(Map.of(live.nodeUnique(), live), cache);
    }

    @Test
    public void uncachedTwoWayDifferenceUsesNodeIdentityForNullIdResources() {
        TopicMetadata sharedSource = topic();
        sharedSource.setId(null);
        sharedSource.setName("shared-without-id");
        TopicMetadata newSource = topic();
        newSource.setId(null);
        newSource.setName("new-without-id");
        TopicMetadata sharedTarget = topic();
        sharedTarget.setId(null);
        sharedTarget.setName("shared-without-id");
        TopicMetadata staleTarget = topic();
        staleTarget.setId(null);
        staleTarget.setName("stale-without-id");

        DataMetadataHandler<BaseClusterIdBase> source = mock(DataMetadataHandler.class);
        DataMetadataHandler<BaseClusterIdBase> remote = mock(DataMetadataHandler.class);
        when(source.getData()).thenReturn(List.of(sharedSource, newSource));
        when(remote.getData()).thenReturn(List.of(sharedTarget, staleTarget));
        BothNotCacheDifference difference = new BothNotCacheDifference();
        difference.setSourceHandler(source);
        difference.setTargetHandler(remote);

        difference.difference();

        verify(remote).handleAll(argThat(rows -> rows.size() == 2 && rows.containsAll(List.of(sharedSource, newSource))),
            eq(List.of(newSource)), argThat(List::isEmpty), eq(List.of(staleTarget)));
        verify(source).acknowledgeData();
        verify(source, never()).retryData();
    }

    @Test
    public void deleteWinsOverEqualTimestampsAndNullLiveStateFailsExplicitly() {
        Fixture f = new Fixture();
        f.metadata.setIsDelete(1);
        f.factory.loadData();
        DataMetadataHandler<BaseClusterIdBase> remote = mock(DataMetadataHandler.class);
        BodyDataDifference difference = new BodyDataDifference();
        difference.setSourceHandler(f.target);
        difference.setTargetHandler(remote);
        difference.difference();
        verify(remote).handleAll(any(), argThat(List::isEmpty), argThat(List::isEmpty), argThat(rows -> rows.size() == 1));
        TopicMetadata invalid = topic();
        invalid.setStatus(null);
        when(f.converter.toMetaData(f.row)).thenReturn(invalid);
        assertThrows(IllegalArgumentException.class, f.factory::loadData);
        invalid.setStatus(1L);
        invalid.setCreateTime(null);
        assertThrows(IllegalArgumentException.class, f.factory::loadData);
    }

    @Test
    public void bodyDifferenceRejectsNullIdentityAndLiveTimestampWithoutRemoteApply() {
        TopicMetadata missingName = topic();
        missingName.setName(null);
        assertBodyDifferenceRejects(missingName);

        TopicMetadata missingUpdateTime = topic();
        missingUpdateTime.setUpdateTime(null);
        assertBodyDifferenceRejects(missingUpdateTime);
    }

    @Test
    public void bodyDifferenceClearsSuccessfulUpdateBeforeAnEmptyBatch() {
        TopicMetadata updated = topic();
        updated.setUpdateTime(updated.getCreateTime().plusSeconds(1));
        DataMetadataHandler<BaseClusterIdBase> source = mock(DataMetadataHandler.class);
        DataMetadataHandler<BaseClusterIdBase> remote = mock(DataMetadataHandler.class);
        when(source.getData()).thenReturn(List.of(updated), List.of());
        BodyDataDifference difference = new BodyDataDifference();
        difference.setSourceHandler(source);
        difference.setTargetHandler(remote);

        difference.difference();
        difference.difference();

        verify(remote).handleAll(eq(List.of(updated)), argThat(List::isEmpty), eq(List.of(updated)), argThat(List::isEmpty));
        verify(remote).handleAll(eq(List.of(updated)), argThat(List::isEmpty), argThat(List::isEmpty), argThat(List::isEmpty));
        verify(source, times(2)).acknowledgeData();
        verify(source, never()).retryData();
    }

    @Test
    public void reversePersistenceFailureKeepsQueuedWritesAndConvertsUpdates() {
        Fixture f = new Fixture();
        Object entity = new Object();
        when(f.converter.toEntity(f.metadata)).thenReturn(entity);
        f.target.updateMetadata(f.metadata);
        doThrow(new IllegalStateException("db offline")).doNothing().when(f.source).handleAll(isNull(), anyList(), anyList(), anyList());
        assertThrows(IllegalStateException.class, f.factory::persistence);
        f.factory.persistence();
        f.factory.persistence();
        verify(f.source, times(2)).handleAll(isNull(), argThat(List::isEmpty), eq(List.of(entity)), argThat(List::isEmpty));
    }

    @Test
    public void reversePersistenceQueuesAddsAndDeletesAndClearsOnlyAfterSuccess() {
        Fixture f = new Fixture();
        Object entity = new Object();
        when(f.converter.toEntity(f.metadata)).thenReturn(entity);
        f.target.addMetadata(f.metadata);
        f.target.deleteMetadata(f.metadata);

        f.factory.persistence();
        f.factory.persistence();

        verify(f.source, times(1)).handleAll(isNull(), eq(List.of(entity)), argThat(List::isEmpty), eq(List.of(entity)));
        verifyNoMoreInteractions(f.source);
    }

    @Test
    public void runtimeTargetsReceiveOnlyTheirOwnRowsAndForgetDoesNotClearPeers() {
        RuntimeFixture f = new RuntimeFixture();
        f.factory.loadData();

        assertEquals(List.of(f.firstMetadata), f.firstTarget.getData());
        assertEquals(List.of(f.secondMetadata), f.secondTarget.getData());
        f.firstTarget.acknowledgeData();
        f.secondTarget.acknowledgeData();
        f.factory.loadData();
        assertTrue(f.firstTarget.getData().isEmpty());
        assertTrue(f.secondTarget.getData().isEmpty());
        f.firstTarget.acknowledgeData();
        f.secondTarget.acknowledgeData();

        f.factory.forgetTarget(f.firstRuntime);
        f.factory.loadData();
        assertEquals(List.of(f.firstMetadata), f.firstTarget.getData());
        assertTrue(f.secondTarget.getData().isEmpty());
    }

    @Test
    public void runtimeHandlerUsesItsRegisteredRuntimeForExplicitQueries() {
        RuntimeFixture f = new RuntimeFixture();
        when(f.source.getData(any(Global2Request.class))).thenReturn(List.of(f.firstRow));

        List<BaseClusterIdBase> result = f.firstTarget.getData(new Global2Request());

        assertEquals(List.of(f.firstMetadata), result);
        verify(f.source).getData(argThat(request -> request.getRuntimeId().equals(30L) && request.getClusterId() == null));
        verify(f.converter).toMetaData(f.firstRow);
    }

    @Test
    public void schedulerRetriesFailedFirstSyncAndKeepsCompletionOnLaterRuns() {
        org.apache.eventmesh.dashboard.core.metadata.MetadataSyncWrapper wrapper = new org.apache.eventmesh.dashboard.core.metadata.MetadataSyncWrapper();
        org.apache.eventmesh.dashboard.core.metadata.MetadataSyncManage.MetadataSyncConfig config =
            new org.apache.eventmesh.dashboard.core.metadata.MetadataSyncManage.MetadataSyncConfig();
        config.setMetadataType(MetadataType.TOPIC);
        config.setClusterServiceType(org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType.SELF);
        config.setMetadataSyncResult(new org.apache.eventmesh.dashboard.core.metadata.result.MetadataSyncResult());
        ClusterMetadata cluster = new ClusterMetadata();
        cluster.setId(10L);
        cluster.setFirstToWhom(org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType.FirstToWhom.DASHBOARD);
        cluster.setFirstSyncState(org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType.FirstToWhom.WAIT_START);
        wrapper.setMetadataSyncConfig(config);
        wrapper.setBaseSyncBase(cluster);
        wrapper.setMetadataSyncResultHandler(mock(org.apache.eventmesh.dashboard.core.metadata.result.MetadataSyncResultHandler.class));
        var first = mock(org.apache.eventmesh.dashboard.core.metadata.difference.BufferDifference.class);
        var timing = mock(BodyDataDifference.class);
        wrapper.setFirstTimeDifference(first);
        wrapper.setTimingSyncDifference(timing);
        wrapper.setCheck(false);
        doThrow(new IllegalStateException("offline")).doNothing().when(first).difference();
        wrapper.run();
        assertEquals(org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType.FirstToWhom.ERROR_ING, cluster.getFirstSyncState());
        wrapper.setErrorNextTime(0);
        wrapper.run();
        assertEquals(org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType.FirstToWhom.COMPLETE, cluster.getFirstSyncState());
        verify(first, times(2)).difference();
        doThrow(new IllegalStateException("timing sync offline")).doNothing().when(timing).difference();
        wrapper.run();
        assertEquals(org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType.FirstToWhom.COMPLETE, cluster.getFirstSyncState());
        assertTrue(wrapper.getErrorNextTime() > 0L);
        var check = mock(org.apache.eventmesh.dashboard.core.metadata.difference.BothCacheDifference.class);
        wrapper.setCheckDifference(check);
        wrapper.setCheck(true);
        wrapper.setErrorNextTime(System.currentTimeMillis() - 1L);
        wrapper.run();
        verify(timing, times(2)).difference();
        verify(check, never()).difference();
        assertEquals(0L, wrapper.getErrorNextTime());
        assertEquals(org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType.FirstToWhom.COMPLETE, cluster.getFirstSyncState());
        wrapper.getRunStatus().set(true);
        wrapper.run();
        assertTrue(wrapper.getRunStatus().get());
        wrapper.getRunStatus().set(false);
        wrapper.deactivate();
        wrapper.run();
        verify(timing, times(2)).difference();
    }

    private static TopicMetadata topic() {
        TopicMetadata metadata = new TopicMetadata();
        metadata.setId(1L);
        metadata.setClusterId(10L);
        metadata.setClusterType(ClusterType.STORAGE_KAFKA_BROKER);
        metadata.setName("orders");
        metadata.setStatus(1L);
        metadata.setCreateTime(LocalDateTime.of(2026, 10, 21, 0, 0));
        metadata.setUpdateTime(metadata.getCreateTime());
        return metadata;
    }

    private static TopicMetadata runtimeTopic(Long id, Long runtimeId) {
        TopicMetadata metadata = new TopicMetadata();
        metadata.setId(id);
        metadata.setClusterId(20L);
        metadata.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE);
        metadata.setRuntimeId(runtimeId);
        metadata.setName("orders-" + runtimeId);
        metadata.setStatus(1L);
        metadata.setCreateTime(LocalDateTime.of(2026, 10, 21, 0, 0));
        metadata.setUpdateTime(metadata.getCreateTime());
        return metadata;
    }

    private static void assertBodyDifferenceRejects(TopicMetadata invalid) {
        DataMetadataHandler<BaseClusterIdBase> source = mock(DataMetadataHandler.class);
        DataMetadataHandler<BaseClusterIdBase> remote = mock(DataMetadataHandler.class);
        when(source.getData()).thenReturn(List.of(invalid));
        BodyDataDifference difference = new BodyDataDifference();
        difference.setSourceHandler(source);
        difference.setTargetHandler(remote);

        assertThrows(IllegalArgumentException.class, difference::difference);
        verify(source).retryData();
        verify(source, never()).acknowledgeData();
        verify(remote, never()).handleAll(any(), anyList(), anyList(), anyList());
    }

    private static class Fixture {
        final Object row = new Object();
        final TopicMetadata metadata = topic();
        final SyncMetadataCreateFactory factory = new SyncMetadataCreateFactory();
        final DataMetadataHandler<Object> source = mock(DataMetadataHandler.class);
        final ConvertMetaData<Object, BaseClusterIdBase> converter = mock(ConvertMetaData.class);
        final MetadataHandler<BaseClusterIdBase> target;

        Fixture() {
            ClusterMetadata cluster = new ClusterMetadata();
            cluster.setId(10L);
            cluster.setClusterType(ClusterType.STORAGE_KAFKA_BROKER);
            ClusterDO data = new ClusterDO();
            data.setClusterInfo(cluster);
            ColonyDO<ClusterDO> colony = ColonyDO.create(ClusterDO.class, null);
            colony.register(10L, cluster.getClusterType(), data);
            factory.setColonyDO(colony);
            factory.setMetadataType(MetadataType.TOPIC);
            factory.setDataMetadataHandler(source);
            factory.setConvertMetaData(converter);
            when(source.getData()).thenReturn(List.of(row));
            when(converter.toMetaData(row)).thenReturn(metadata);
            target = factory.createDataMetadataHandler(cluster);
        }
    }

    private static class RuntimeFixture {
        final Object firstRow = new Object();
        final Object secondRow = new Object();
        final RuntimeMetadata firstRuntime = runtime(30L);
        final RuntimeMetadata secondRuntime = runtime(31L);
        final TopicMetadata firstMetadata = runtimeTopic(1L, 30L);
        final TopicMetadata secondMetadata = runtimeTopic(2L, 31L);
        final SyncMetadataCreateFactory factory = new SyncMetadataCreateFactory();
        final DataMetadataHandler<Object> source = mock(DataMetadataHandler.class);
        final ConvertMetaData<Object, BaseClusterIdBase> converter = mock(ConvertMetaData.class);
        final MetadataHandler<BaseClusterIdBase> firstTarget;
        final MetadataHandler<BaseClusterIdBase> secondTarget;

        RuntimeFixture() {
            ClusterMetadata cluster = new ClusterMetadata();
            cluster.setId(20L);
            cluster.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE);
            ClusterDO data = new ClusterDO();
            data.setClusterInfo(cluster);
            data.getRuntimeMap().put(30L, new org.apache.eventmesh.dashboard.core.cluster.RuntimeDO());
            data.getRuntimeMap().put(31L, new org.apache.eventmesh.dashboard.core.cluster.RuntimeDO());
            ColonyDO<ClusterDO> colony = ColonyDO.create(ClusterDO.class, null);
            colony.register(20L, cluster.getClusterType(), data);
            factory.setColonyDO(colony);
            factory.setMetadataType(MetadataType.TOPIC);
            factory.setDataMetadataHandler(source);
            factory.setConvertMetaData(converter);
            when(source.getData()).thenReturn(List.of(firstRow, secondRow));
            when(converter.toMetaData(firstRow)).thenReturn(firstMetadata);
            when(converter.toMetaData(secondRow)).thenReturn(secondMetadata);
            firstTarget = factory.createDataMetadataHandler(firstRuntime);
            secondTarget = factory.createDataMetadataHandler(secondRuntime);
        }

        private static RuntimeMetadata runtime(Long id) {
            RuntimeMetadata runtime = new RuntimeMetadata();
            runtime.setId(id);
            runtime.setClusterId(20L);
            runtime.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE);
            return runtime;
        }
    }
}
