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

package org.apache.eventmesh.dashboard.console.function.report.collect.exporter;

import org.apache.eventmesh.dashboard.common.model.metadata.CollectMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;
import org.apache.eventmesh.dashboard.console.function.report.collect.AbstractCollect;
import org.apache.eventmesh.dashboard.console.function.report.collect.DataSyncHandler.DataSyncHandlerWrapper;
import org.apache.eventmesh.dashboard.console.function.report.collect.RestoreData;
import org.apache.eventmesh.dashboard.console.function.report.model.base.OrganizationId;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.management.JMException;
import javax.management.JMRuntimeException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import lombok.extern.slf4j.Slf4j;

/** 提供节点 JMX 连接、数值读取与分组查询，复用父类采集及结果缓冲流程。 */
@Slf4j
public abstract class JMXCollect extends AbstractCollect {

    protected static final long ERROR_LONG = -1L;

    protected static final float ERROR_FLOAT = -1F;

    private static final int STATUS_IDLE = 0;

    private static final int STATUS_COLLECTING = 1;

    private static final int STATUS_DELIVERING = 2;

    private static final int CONNECT_TIMEOUT_SECONDS = 5;

    // RMI 返回的地址也可能不可达，JNDI 超时不能约束握手；限制等待与后台连接数量。
    private static final ThreadPoolExecutor CONNECT_EXECUTOR = new ThreadPoolExecutor(0, 20, 30, TimeUnit.SECONDS,
        new SynchronousQueue<>(), new ThreadFactoryBuilder().setNameFormat("jmx-connect-%d").setDaemon(true).build(),
        new ThreadPoolExecutor.AbortPolicy());

    /** 同一节点的迟到请求未结束前，不再占用新的后台线程。 */
    private static final Set<String> IN_FLIGHT = ConcurrentHashMap.newKeySet();

    private final AtomicInteger state = new AtomicInteger(STATUS_IDLE);

    private CollectMetadata collectMetadata;

    private RuntimeMetadata runtimeMetadata;

    @Override
    public final void collect(int index, DataSyncHandlerWrapper wrapper) {
        // 仅在入口控制重入，采集缓冲、维度补齐和正常结果交付沿用父类实现。
        if (!state.compareAndSet(STATUS_IDLE, STATUS_COLLECTING)) {
            syncEmpty(index, wrapper);
            return;
        }
        try {
            super.collect(index, wrapper);
        } finally {
            try {
                // 采集异常尚未交付，使用既有 sync 契约完成无样本轮次；入库回调失败不能再次交付。
                if (state.get() == STATUS_COLLECTING) {
                    syncEmpty(index, wrapper);
                }
            } finally {
                state.set(STATUS_IDLE);
            }
        }
    }

    @Override
    protected final void doCollect() {
        doJmxCollect();
        // 只有采集阶段正常结束才能进入交付阶段，避免异常路径误判为已由父类 sync。
        if (!state.compareAndSet(STATUS_COLLECTING, STATUS_DELIVERING)) {
            throw new IllegalStateException("Unexpected JMX collection status: " + state.get());
        }
    }

    protected abstract void doJmxCollect();

    private void syncEmpty(int index, DataSyncHandlerWrapper wrapper) {
        RestoreData result = new RestoreData();
        result.setAbstractCollect(this);
        result.setCollectMetadata(collectMetadata);
        result.setIndex(index);
        wrapper.sync(result);
    }

    @Override
    public void setCollectMetadata(CollectMetadata collectMetadata) {
        super.setCollectMetadata(collectMetadata);
        this.collectMetadata = collectMetadata;
    }

    @Override
    public void restore(RestoreData restoreData) {
        // JMX 采集轮次相互独立，不能复用仍可能被其他线程持有的结果容器。
    }

    @Override
    public void setRuntimeMetadata(RuntimeMetadata runtimeMetadata) {
        super.setRuntimeMetadata(runtimeMetadata);
        // 父类元数据保持封装，JMX 仅保留连接所需的节点引用。
        this.runtimeMetadata = runtimeMetadata;
    }

    protected final RuntimeMetadata getRuntimeMetadata() {
        return this.runtimeMetadata;
    }

    protected final void withConnection(BiConsumer<MBeanServerConnection, Consumer<OrganizationId>> collection) {
        RuntimeMetadata broker = getRuntimeMetadata();
        if (broker == null || broker.getHost() == null || broker.getHost().isBlank()
            || broker.getJmxPort() == null || broker.getJmxPort() <= 0 || broker.getJmxPort() > 65535) {
            return;
        }
        String address = String.format("service:jmx:rmi:///jndi/rmi://%s:%d/jmxrmi", broker.getHost(), broker.getJmxPort());
        String key = broker.getClusterId() + "/" + broker.getId() + "/" + address;
        if (!IN_FLIGHT.add(key)) {
            log.warn("Previous JMX request is still running for broker {} in cluster {}", broker.getName(), broker.getClusterId());
            return;
        }
        CompletableFuture<Void> connected = new CompletableFuture<>();
        CompletableFuture<List<OrganizationId>> result = new CompletableFuture<>();
        try {
            JMXServiceURL url = new JMXServiceURL(address);
            CONNECT_EXECUTOR.execute(() -> collectInBackground(key, url, collection, connected, result));
        } catch (IOException | RejectedExecutionException | SecurityException e) {
            IN_FLIGHT.remove(key);
            log.warn("JMX connection failed for broker {} in cluster {}", broker.getName(), broker.getClusterId(), e);
            return;
        }
        try {
            connected.get(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            result.get(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS).forEach(this::setData);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("JMX collection interrupted for broker {} in cluster {}", broker.getName(), broker.getClusterId(), e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException exception) {
                throw exception;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            log.warn("JMX collection failed for broker {} in cluster {}", broker.getName(), broker.getClusterId(), cause);
        } catch (TimeoutException e) {
            log.warn("JMX collection timed out for broker {} in cluster {}", broker.getName(), broker.getClusterId(), e);
        } finally {
            result.cancel(false);
        }
    }

    protected JMXConnector openConnector(JMXServiceURL url) throws IOException {
        return JMXConnectorFactory.connect(url);
    }

    private void collectInBackground(String key, JMXServiceURL url,
        BiConsumer<MBeanServerConnection, Consumer<OrganizationId>> collection, CompletableFuture<Void> connected,
        CompletableFuture<List<OrganizationId>> result) {
        List<OrganizationId> samples = new ArrayList<>();
        Throwable failure = null;
        boolean collected = false;
        try {
            if (result.isCancelled()) {
                return;
            }
            try (JMXConnector connector = openConnector(url)) {
                if (result.isCancelled()) {
                    return;
                }
                MBeanServerConnection connection = connector.getMBeanServerConnection();
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(CONNECT_TIMEOUT_SECONDS);
                connected.complete(null);
                collection.accept(boundedConnection(connection, deadline, result), samples::add);
                collected = true;
            }
        } catch (Throwable e) {
            if (collected && (e instanceof IOException || e instanceof SecurityException)) {
                log.warn("Unable to close JMX connection after collection", e);
            } else {
                failure = e;
            }
        } finally {
            IN_FLIGHT.remove(key);
        }
        if (failure != null) {
            connected.completeExceptionally(failure);
            if (!result.completeExceptionally(failure)) {
                log.warn("JMX task failed after its collection deadline", failure);
            }
            if (failure instanceof Error error) {
                throw error;
            }
        } else {
            result.complete(samples);
        }
    }

    private MBeanServerConnection boundedConnection(MBeanServerConnection connection, long deadline,
        CompletableFuture<?> result) {
        return (MBeanServerConnection) Proxy.newProxyInstance(MBeanServerConnection.class.getClassLoader(),
            new Class<?>[] {MBeanServerConnection.class}, (proxy, method, args) -> {
                if (result.isCancelled() || System.nanoTime() >= deadline) {
                    throw new SocketTimeoutException("JMX collection read deadline exceeded");
                }
                try {
                    return method.invoke(connection, args);
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                }
            });
    }

    protected final long readLong(MBeanServerConnection connection, String name, String attribute) {
        try {
            Object value = connection.getAttribute(new ObjectName(name), attribute);
            if (value instanceof Number number) {
                // 精确转换拒绝截断、NaN、无穷与越界值，零仍是有效采样。
                long result = new BigDecimal(number.toString()).longValueExact();
                return result >= 0 ? result : ERROR_LONG;
            }
        } catch (JMException | JMRuntimeException | IOException | ArithmeticException | NumberFormatException | SecurityException e) {
            log.warn("JMX integer attribute {}:{} is unavailable", name, attribute, e);
        }
        return ERROR_LONG;
    }

    protected final float readFloat(MBeanServerConnection connection, String name, String attribute) {
        try {
            Object value = connection.getAttribute(new ObjectName(name), attribute);
            if (value instanceof Number number) {
                float result = number.floatValue();
                if (Float.isFinite(result) && result >= 0) {
                    return result;
                }
            }
        } catch (JMException | JMRuntimeException | IOException | SecurityException e) {
            log.warn("JMX floating attribute {}:{} is unavailable", name, attribute, e);
        }
        return ERROR_FLOAT;
    }

    protected final Set<ObjectName> queryNames(MBeanServerConnection connection, String pattern) {
        try {
            Set<ObjectName> names = connection.queryNames(new ObjectName(pattern), null);
            return names == null ? Set.of() : names;
        } catch (JMException | JMRuntimeException | IOException | SecurityException e) {
            log.warn("JMX query {} is unavailable", pattern, e);
            return Set.of();
        }
    }

    /** 任一项失败或求和溢出均返回错误值，避免把部分总量当作完整指标。 */
    protected final long sumLong(MBeanServerConnection connection, String pattern, String attribute) {
        Set<ObjectName> names = queryNames(connection, pattern);
        if (names.isEmpty()) {
            return ERROR_LONG;
        }
        long total = 0;
        for (ObjectName name : names) {
            long value = readLong(connection, name.toString(), attribute);
            if (value == ERROR_LONG) {
                return ERROR_LONG;
            }
            try {
                total = Math.addExact(total, value);
            } catch (ArithmeticException e) {
                log.warn("JMX aggregate {}:{} overflowed", pattern, attribute, e);
                return ERROR_LONG;
            }
        }
        return total;
    }

    /** 一次查询后按标签分组并读取指定指标，调用方只处理完整分组，不再遍历 Topic 或 MBean。 */
    protected final void collectGroupedLongs(MBeanServerConnection connection, String pattern, String groupKey,
        String metricKey, String attribute, Set<String> metrics, BiConsumer<String, Map<String, Long>> groupConsumer) {
        Map<String, Map<String, Long>> groups = new HashMap<>();
        for (ObjectName name : queryNames(connection, pattern)) {
            String metric = name.getKeyProperty(metricKey);
            String group = name.getKeyProperty(groupKey);
            if (group == null || !metrics.contains(metric)) {
                continue;
            }
            if (group.startsWith("\"")) {
                group = ObjectName.unquote(group);
            }
            Map<String, Long> values = groups.computeIfAbsent(group, key -> {
                Map<String, Long> errorValues = new HashMap<>();
                metrics.forEach(nameKey -> errorValues.put(nameKey, ERROR_LONG));
                return errorValues;
            });
            values.put(metric, readLong(connection, name.toString(), attribute));
        }
        groups.forEach(groupConsumer);
    }

    protected final boolean hasValue(Number... values) {
        for (Number value : values) {
            if (value != null && Double.isFinite(value.doubleValue()) && value.doubleValue() >= 0) {
                return true;
            }
        }
        return false;
    }

}
