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
import org.apache.rocketmq.remoting.netty.ResponseFuture;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/** Admission is nonblocking. Permits belong to transport termination, not caller cancellation. */
public final class RocketMQAsync {
    private static final Semaphore GLOBAL = new Semaphore(128);
    private static final Map<DefaultRemotingClient, Semaphore> BROKERS = Collections.synchronizedMap(new WeakHashMap<>());
    private static final AtomicInteger THREADS = new AtomicInteger();
    public static final ThreadPoolExecutor PROCESSING = executor("rocketmq-decode", 4, 512);
    private static final ThreadPoolExecutor SEND = executor("rocketmq-send", 4, 256);

    private RocketMQAsync() {
    }

    private static ThreadPoolExecutor executor(String name, int threads, int capacity) {
        return new ThreadPoolExecutor(threads, threads, 0, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(capacity), task -> {
            Thread thread = new Thread(task, name + "-" + THREADS.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }, new ThreadPoolExecutor.AbortPolicy());
    }

    public static CompletableFuture<RemotingCommand> invoke(DefaultRemotingClient client, RemotingCommand request, long timeout) {
        CompletableFuture<RemotingCommand> result = new CompletableFuture<>();
        Semaphore broker;
        synchronized (BROKERS) {
            broker = BROKERS.computeIfAbsent(client, key -> new Semaphore(8));
        }
        if (!GLOBAL.tryAcquire()) {
            return CompletableFuture.failedFuture(new IllegalStateException("Global RPC capacity exhausted"));
        }
        if (!broker.tryAcquire()) {
            GLOBAL.release();
            return CompletableFuture.failedFuture(new IllegalStateException("Broker RPC capacity exhausted"));
        }
        AtomicBoolean released = new AtomicBoolean();
        Runnable release = () -> {
            if (released.compareAndSet(false, true)) {
                broker.release();
                GLOBAL.release();
            }
        };
        long deadline = System.currentTimeMillis() + timeout;
        try {
            SEND.execute(() -> {
                try {
                    long remaining = deadline - System.currentTimeMillis();
                    if (remaining <= 0 || result.isCancelled()) {
                        throw new TimeoutException("RPC expired before send");
                    }
                    client.invokeAsync(request, remaining, new InvokeCallback() {
                        @Override
                        public void operationSucceed(RemotingCommand response) {
                            release.run();
                            result.complete(response);
                        }

                        @Override
                        public void operationFail(Throwable cause) {
                            release.run();
                            result.completeExceptionally(cause);
                        }

                        @Override
                        public void operationComplete(ResponseFuture responseFuture) {
                            // RocketMQ invokes success/failure before this notification.
                        }
                    });
                } catch (Exception e) {
                    if (e instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                    }
                    release.run();
                    result.completeExceptionally(e);
                }
            });
        } catch (RuntimeException e) {
            release.run();
            result.completeExceptionally(e);
        }
        return result;
    }
}
