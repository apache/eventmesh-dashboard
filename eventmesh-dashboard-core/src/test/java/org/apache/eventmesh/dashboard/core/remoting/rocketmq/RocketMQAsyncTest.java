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


package org.apache.eventmesh.dashboard.core.remoting.rocketmq;

import org.apache.eventmesh.dashboard.core.function.SDK.operation.rocketmq.RocketMQRemotingSDKOperation.DefaultRemotingClient;

import org.apache.rocketmq.remoting.InvokeCallback;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class RocketMQAsyncTest {
    @Test
    void successAndBrokerErrorKeepOriginalResponse() throws Exception {
        DefaultRemotingClient client = Mockito.mock(DefaultRemotingClient.class);
        Mockito.doAnswer(invocation -> {
            InvokeCallback callback = invocation.getArgument(2);
            callback.operationSucceed(RemotingCommand.createResponseCommand(17, "original error"));
            return null;
        }).when(client).invokeAsync(Mockito.any(), Mockito.anyLong(), Mockito.any());
        RemotingCommand reply = RocketMQAsync.invoke(client, RemotingCommand.createRequestCommand(1, null), 1000).get(2, TimeUnit.SECONDS);
        Assertions.assertEquals(17, reply.getCode());
        Assertions.assertEquals("original error", reply.getRemark());
    }

    @Test
    void sendExceptionCompletesFutureAndReleasesPermit() throws Exception {
        DefaultRemotingClient client = Mockito.mock(DefaultRemotingClient.class);
        Mockito.doThrow(new IllegalStateException("send failure")).when(client).invokeAsync(Mockito.any(), Mockito.anyLong(), Mockito.any());
        for (int i = 0; i < 12; i++) {
            ExecutionException error = Assertions.assertThrows(ExecutionException.class, () ->
                RocketMQAsync.invoke(client, RemotingCommand.createRequestCommand(1, null), 1000).get(2, TimeUnit.SECONDS));
            Assertions.assertEquals("send failure", error.getCause().getMessage());
        }
    }

    @Test
    void cancellationDoesNotReleasePhysicalPermitsAndLateCallbackReleasesOnce() throws Exception {
        DefaultRemotingClient client = Mockito.mock(DefaultRemotingClient.class);
        BlockingQueue<InvokeCallback> callbacks = new LinkedBlockingQueue<>();
        Mockito.doAnswer(invocation -> {
            callbacks.add(invocation.getArgument(2));
            return null;
        }).when(client).invokeAsync(Mockito.any(), Mockito.anyLong(), Mockito.any());
        List<InvokeCallback> pending = new ArrayList<>();
        try {
            for (int i = 0; i < 8; i++) {
                CompletableFuture<RemotingCommand> future = RocketMQAsync.invoke(client, RemotingCommand.createRequestCommand(1, null), 1000);
                InvokeCallback callback = callbacks.poll(2, TimeUnit.SECONDS);
                Assertions.assertNotNull(callback);
                pending.add(callback);
                future.cancel(false);
            }
            CompletableFuture<RemotingCommand> rejected = RocketMQAsync.invoke(client, RemotingCommand.createRequestCommand(1, null), 1000);
            Assertions.assertTrue(rejected.isCompletedExceptionally());
            InvokeCallback first = pending.remove(0);
            first.operationFail(new IllegalStateException("physical timeout"));
            first.operationFail(new IllegalStateException("duplicate callback"));
            CompletableFuture<RemotingCommand> admitted = RocketMQAsync.invoke(client, RemotingCommand.createRequestCommand(1, null), 1000);
            InvokeCallback callback = callbacks.poll(2, TimeUnit.SECONDS);
            Assertions.assertNotNull(callback);
            pending.add(callback);
            Assertions.assertFalse(admitted.isDone());
            Assertions.assertTrue(RocketMQAsync.invoke(client, RemotingCommand.createRequestCommand(1, null), 1000).isCompletedExceptionally());
        } finally {
            pending.forEach(callback -> callback.operationFail(new IllegalStateException("test cleanup")));
        }
    }

    @Test
    void convertsBrokerBinaryByteUnits() {
        Assertions.assertEquals(1536, RocketMQMetricsRemotingService.bytes("1.5 KiB"));
        Assertions.assertEquals(0, RocketMQMetricsRemotingService.bytes("0 B"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> RocketMQMetricsRemotingService.bytes("1 bananas"));
    }
}
