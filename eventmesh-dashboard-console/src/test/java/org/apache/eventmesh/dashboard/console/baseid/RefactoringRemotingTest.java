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

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.annotation.RemotingServiceMethodMapper;
import org.apache.eventmesh.dashboard.common.model.remoting.RemotingActionType;
import org.apache.eventmesh.dashboard.common.model.remoting.GlobalResult;
import org.apache.eventmesh.dashboard.common.model.metadata.TopicMetadata;
import org.apache.eventmesh.dashboard.common.model.remoting.topic.CreateTopic2Request;
import org.apache.eventmesh.dashboard.common.model.remoting.topic.CreateTopicResult;
import org.apache.eventmesh.dashboard.common.model.remoting.topic.DeleteTopicResult;
import org.apache.eventmesh.dashboard.common.model.remoting.topic.GetTopicsResult;
import org.apache.eventmesh.dashboard.core.remoting.Remoting2Manage;
import org.apache.eventmesh.dashboard.service.remoting.TopicRemotingService;

import java.lang.reflect.Constructor;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

public class RefactoringRemotingTest {

    @Test
    public void partialFailureRetriesOnlyUnconfirmedOperations() throws Exception {
        TopicRemotingService rpc = mock(TopicRemotingService.class);
        var handler = handler(rpc);
        TopicMetadata first = new TopicMetadata();
        first.setId(1L);
        first.setName("first");
        TopicMetadata second = new TopicMetadata();
        second.setId(2L);
        second.setName("second");
        CreateTopicResult ok = new CreateTopicResult();
        ok.setCode(200);
        CreateTopicResult failed = new CreateTopicResult();
        failed.setCode(503);
        when(rpc.createTopic(argThat(request -> request != null && request.getMetaData() == first))).thenReturn(ok);
        when(rpc.createTopic(argThat(request -> request != null && request.getMetaData() == second))).thenReturn(failed, ok);
        assertThrows(IllegalStateException.class, () -> handler.handleAll(List.of(), List.of(first, second), List.of(), List.of()));
        handler.handleAll(List.of(), List.of(first, second), List.of(), List.of());
        verify(rpc, times(1)).createTopic(argThat(request -> request != null && request.getMetaData() == first));
        verify(rpc, times(2)).createTopic(argThat(request -> request != null && request.getMetaData() == second));
    }

    @Test
    public void failedQueriesNeverMasqueradeAsEmptySnapshots() throws Exception {
        TopicRemotingService rpc = mock(TopicRemotingService.class);
        var handler = handler(rpc);
        when(rpc.getAllTopics(any())).thenThrow(new IllegalStateException("broker offline"));
        assertThrows(IllegalStateException.class, handler::getData);
        GetTopicsResult result = new GetTopicsResult();
        result.setCode(503);
        doReturn(result).when(rpc).getAllTopics(any());
        assertThrows(IllegalStateException.class, handler::getData);
        result.setCode(200);
        result.setData(List.of());
        assertTrue(handler.getData().isEmpty());
        TopicMetadata topic = new TopicMetadata();
        topic.setName("visible-topic");
        result.setData(List.of(topic));
        assertEquals(List.of(topic), handler.getData());
    }

    @Test
    public void nullOptionalWriteCollectionsAreIgnored() throws Exception {
        TopicRemotingService rpc = mock(TopicRemotingService.class);
        var handler = handler(rpc);

        handler.handleAll(null, null, null, null);

        verify(rpc, org.mockito.Mockito.never()).createTopic(any());
        verify(rpc, org.mockito.Mockito.never()).deleteTopic(any());
    }

    @Test
    public void updateAndDeleteAreInvokedAgainForTheSameResourceOnTheNextBatch() throws Exception {
        TopicRemotingService rpc = mock(TopicRemotingService.class);
        var handler = handler(rpc);
        TopicMetadata updated = new TopicMetadata();
        updated.setId(10L);
        updated.setName("updated");
        TopicMetadata deleted = new TopicMetadata();
        deleted.setId(11L);
        deleted.setName("deleted");
        CreateTopicResult updateResult = new CreateTopicResult();
        updateResult.setCode(200);
        DeleteTopicResult deleteResult = new DeleteTopicResult();
        deleteResult.setCode(200);
        when(rpc.createTopic(any())).thenReturn(updateResult);
        when(rpc.deleteTopic(any())).thenReturn(deleteResult);

        for (int attempt = 0; attempt < 2; attempt++) {
            handler.handleAll(List.of(), List.of(), List.of(updated), List.of(deleted));
        }

        verify(rpc, times(2)).createTopic(argThat(request -> request != null && request.getMetaData() == updated));
        verify(rpc, times(2)).deleteTopic(argThat(request -> request != null && request.getMetaData() == deleted));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void nullNonVoidResponseIsAVisibleRemoteFailure() throws Exception {
        TopicRemotingService rpc = mock(TopicRemotingService.class);
        var handler = handler(rpc);
        TopicMetadata topic = new TopicMetadata();
        topic.setId(12L);
        topic.setName("missing-response");
        when(rpc.createTopic(any())).thenReturn(null);
        var hook = mock(org.apache.eventmesh.dashboard.core.remoting.RemotingResultHook.class);
        List<org.apache.eventmesh.dashboard.core.remoting.RemotingResultHook> hooks =
            (List<org.apache.eventmesh.dashboard.core.remoting.RemotingResultHook>) ReflectionTestUtils.getField(
                Remoting2Manage.getInstance(), "remotingResultHookList");
        hooks.add(hook);

        try {
            assertThrows(IllegalStateException.class,
                () -> handler.handleAll(List.of(), List.of(topic), List.of(), List.of()));
            verify(hook).fail(eq(RemotingActionType.ADD), eq(topic), isNull(), any());
        } finally {
            hooks.remove(hook);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void failedRemoteResponseIsReportedToFailureHooks() throws Exception {
        TopicRemotingService rpc = mock(TopicRemotingService.class);
        var handler = handler(rpc);
        TopicMetadata topic = new TopicMetadata();
        topic.setId(15L);
        topic.setName("failed-response");
        CreateTopicResult result = new CreateTopicResult();
        result.setCode(503);
        when(rpc.createTopic(any())).thenReturn(result);
        var hook = mock(org.apache.eventmesh.dashboard.core.remoting.RemotingResultHook.class);
        List<org.apache.eventmesh.dashboard.core.remoting.RemotingResultHook> hooks =
            (List<org.apache.eventmesh.dashboard.core.remoting.RemotingResultHook>) ReflectionTestUtils.getField(
                Remoting2Manage.getInstance(), "remotingResultHookList");
        hooks.add(hook);
        try {
            assertThrows(IllegalStateException.class,
                () -> handler.handleAll(List.of(), List.of(topic), List.of(), List.of()));
            ArgumentCaptor<GlobalResult<Object>> resultCaptor = ArgumentCaptor.forClass((Class) GlobalResult.class);
            verify(hook).fail(eq(RemotingActionType.ADD), eq(topic), resultCaptor.capture(), any());
            assertSame(result, resultCaptor.getValue());
        } finally {
            hooks.remove(hook);
        }
    }

    @Test
    public void interruptedRemoteCallRestoresThreadInterruptFlag() throws Exception {
        TopicRemotingService rpc = mock(TopicRemotingService.class);
        var handler = handler(rpc);
        TopicMetadata topic = new TopicMetadata();
        topic.setId(13L);
        topic.setName("interrupted");
        when(rpc.createTopic(any())).thenThrow(new InterruptedException("cancelled"));

        try {
            assertThrows(IllegalStateException.class,
                () -> handler.handleAll(List.of(), List.of(topic), List.of(), List.of()));
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    public void voidRemoteActionCanSucceedWithoutAResultEnvelope() throws Exception {
        Object methodWrapper = newPrivateInstance("org.apache.eventmesh.dashboard.core.remoting.Remoting2Manage$RemotingServiceMethodMapperWrapper");
        var method = VoidAction.class.getMethod("write", CreateTopic2Request.class);
        ReflectionTestUtils.setField(methodWrapper, "remotingServiceMethod", method);
        ReflectionTestUtils.setField(methodWrapper, "parameterTypes", CreateTopic2Request.class);
        ReflectionTestUtils.setField(methodWrapper, "targetMethodName", "write");
        ReflectionTestUtils.setField(methodWrapper, "notResult", true);

        Object metadataWrapper = newPrivateInstance("org.apache.eventmesh.dashboard.core.remoting.Remoting2Manage$RemotingServiceMetadataWrapper");
        Map<RemotingActionType, Object> actionMap = new EnumMap<>(RemotingActionType.class);
        actionMap.put(RemotingActionType.ADD, methodWrapper);
        ReflectionTestUtils.setField(metadataWrapper, "actionMap", actionMap);
        ReflectionTestUtils.setField(metadataWrapper, "remotingServiceType", VoidAction.class);
        VoidActionImplementation execution = new VoidActionImplementation();
        Remoting2Manage.RemotingService<TopicMetadata> handler = Remoting2Manage.getInstance().new RemotingService<>();
        ReflectionTestUtils.setField(handler, "wrapper", metadataWrapper);
        ReflectionTestUtils.setField(handler, "execution", execution);
        TopicMetadata topic = new TopicMetadata();
        topic.setId(14L);
        topic.setName("void-response");

        handler.handleAll(List.of(), List.of(topic), List.of(), List.of());

        assertEquals(1, execution.calls.get());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void resultHookFailureDoesNotRepeatConfirmedBrokerMutation() throws Exception {
        TopicRemotingService rpc = mock(TopicRemotingService.class);
        var handler = handler(rpc);
        TopicMetadata metadata = new TopicMetadata();
        metadata.setId(3L);
        metadata.setName("reporting-retry");
        CreateTopicResult result = new CreateTopicResult();
        result.setCode(200);
        when(rpc.createTopic(any())).thenReturn(result);
        var hook = mock(org.apache.eventmesh.dashboard.core.remoting.RemotingResultHook.class);
        org.mockito.Mockito.doThrow(new IllegalStateException("audit offline")).doNothing().when(hook)
            .success(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(metadata), org.mockito.ArgumentMatchers.any());
        List<org.apache.eventmesh.dashboard.core.remoting.RemotingResultHook> hooks =
            (List<org.apache.eventmesh.dashboard.core.remoting.RemotingResultHook>) ReflectionTestUtils.getField(
                Remoting2Manage.getInstance(), "remotingResultHookList");
        hooks.add(hook);
        try {
            assertThrows(IllegalStateException.class, () -> handler.handleAll(List.of(), List.of(metadata), List.of(), List.of()));
            handler.handleAll(List.of(), List.of(metadata), List.of(), List.of());
            verify(rpc, times(1)).createTopic(any());
            verify(hook, times(2)).success(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(metadata),
                org.mockito.ArgumentMatchers.any());
        } finally {
            hooks.remove(hook);
        }
    }

    @SuppressWarnings("unchecked")
    private Remoting2Manage.RemotingService<TopicMetadata> handler(TopicRemotingService rpc) {
        Remoting2Manage manager = Remoting2Manage.getInstance();
        Map<ClusterType, Map<Class<?>, Object>> registry = (Map<ClusterType, Map<Class<?>, Object>>) ReflectionTestUtils.getField(
            Remoting2Manage.class, "REMOTING_SERVICE_METADATA_WRAPPER_MAP");
        Remoting2Manage.RemotingService<TopicMetadata> handler = manager.new RemotingService<>();
        ReflectionTestUtils.setField(handler, "wrapper", registry.get(ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE)
            .get(TopicRemotingService.class));
        ReflectionTestUtils.setField(handler, "execution", rpc);
        return handler;
    }

    private static Object newPrivateInstance(String className) throws Exception {
        Constructor<?> constructor = Class.forName(className).getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    public interface VoidAction {
        @RemotingServiceMethodMapper(RemotingActionType.ADD)
        void write(CreateTopic2Request request) throws Exception;
    }

    public static class VoidActionImplementation implements VoidAction {
        private final AtomicInteger calls = new AtomicInteger();

        @Override
        public void write(CreateTopic2Request request) {
            calls.incrementAndGet();
        }
    }
}
