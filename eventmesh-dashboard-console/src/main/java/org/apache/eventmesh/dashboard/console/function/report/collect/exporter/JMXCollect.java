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

import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;
import org.apache.eventmesh.dashboard.console.function.report.collect.AbstractCollect;
import org.apache.eventmesh.dashboard.console.function.report.collect.DataSyncHandler.DataSyncHandlerWrapper;
import org.apache.eventmesh.dashboard.console.function.report.collect.RestoreData;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import javax.management.JMException;
import javax.management.JMRuntimeException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import lombok.extern.slf4j.Slf4j;

/** 提供节点 JMX 连接、数值读取与分组查询，复用父类采集及结果缓冲流程。 */
@Slf4j
public abstract class JMXCollect extends AbstractCollect {

    protected static final long ERROR_LONG = -1L;

    protected static final float ERROR_FLOAT = -1F;

    private static final int IDLE = 0;

    private static final int COLLECTING = 1;

    private static final int DELIVERING = 2;

    private static final int CONNECT_TIMEOUT_SECONDS = 5;

    private static final AtomicInteger CONNECT_THREAD_INDEX = new AtomicInteger();

    // RMI 返回的地址也可能不可达，JNDI 超时不能约束握手；限制等待与后台连接数量。
    private static final ThreadPoolExecutor CONNECT_EXECUTOR = new ThreadPoolExecutor(0, 20, 30, TimeUnit.SECONDS,
        new SynchronousQueue<>(), task -> {
            Thread worker = new Thread(task, "jmx-connect-" + CONNECT_THREAD_INDEX.incrementAndGet());
            worker.setDaemon(true);
            return worker;
        }, new ThreadPoolExecutor.AbortPolicy());

    private static final Map<String, Object> JMX_ENVIRONMENT = Map.of(
        "com.sun.jndi.rmi.connect.timeout", "5000",
        "com.sun.jndi.rmi.read.timeout", "5000",
        "jmx.remote.x.request.waiting.timeout", 5000L);

    private final AtomicInteger state = new AtomicInteger(IDLE);

    private RuntimeMetadata runtimeMetadata;

    @Override
    public final void collect(int index, DataSyncHandlerWrapper wrapper) {
        // 仅在入口控制重入，采集缓冲、维度补齐和正常结果交付沿用父类实现。
        if (!state.compareAndSet(IDLE, COLLECTING)) {
            syncEmpty(index, wrapper);
            return;
        }
        try {
            super.collect(index, wrapper);
        } finally {
            try {
                // 采集异常尚未交付，使用既有 sync 契约完成无样本轮次；入库回调失败不能再次交付。
                if (state.get() == COLLECTING) {
                    syncEmpty(index, wrapper);
                }
            } finally {
                state.set(IDLE);
            }
        }
    }

    @Override
    protected final void doCollect() {
        doJmxCollect();
        state.set(DELIVERING);
    }

    protected abstract void doJmxCollect();

    private void syncEmpty(int index, DataSyncHandlerWrapper wrapper) {
        RestoreData result = createRestoreData();
        result.setIndex(index);
        wrapper.sync(result);
    }

    @Override
    public void setRuntimeMetadata(RuntimeMetadata runtimeMetadata) {
        super.setRuntimeMetadata(runtimeMetadata);
        // 父类元数据保持封装，JMX 仅保留连接所需的节点引用。
        this.runtimeMetadata = runtimeMetadata;
    }

    private RuntimeMetadata getRuntimeMetadata() {
        return this.runtimeMetadata;
    }

    protected final void withConnection(Consumer<MBeanServerConnection> collection) {
        RuntimeMetadata broker = getRuntimeMetadata();
        if (broker == null || broker.getHost() == null || broker.getHost().isBlank()
            || broker.getJmxPort() == null || broker.getJmxPort() <= 0 || broker.getJmxPort() > 65535) {
            return;
        }
        String address = String.format("service:jmx:rmi:///jndi/rmi://%s:%d/jmxrmi", broker.getHost(), broker.getJmxPort());
        // 每轮独立连接并按阿里规约关闭资源，读取失败不占用下一轮连接。
        try (JMXConnector connector = connect(new JMXServiceURL(address))) {
            // RMI 属性读取也可能永久等待，整个读取阶段共享一个期限，避免逐属性累计超时。
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(CONNECT_TIMEOUT_SECONDS);
            collection.accept(boundedConnection(connector.getMBeanServerConnection(), deadline));
        } catch (IOException | SecurityException e) {
            log.warn("JMX connection failed for broker {} in cluster {}", broker.getName(), broker.getClusterId(), e);
        }
    }

    protected JMXConnector connect(JMXServiceURL url) throws IOException {
        AtomicBoolean abandoned = new AtomicBoolean();
        AtomicReference<JMXConnector> opened = new AtomicReference<>();
        Future<JMXConnector> task;
        try {
            task = CONNECT_EXECUTOR.submit(() -> {
                JMXConnector connector = openConnector(url);
                opened.set(connector);
                if (abandoned.get()) {
                    closeAbandoned(opened);
                    throw new IOException("JMX connection completed after timeout");
                }
                return connector;
            });
        } catch (RejectedExecutionException e) {
            throw new IOException("JMX connection capacity reached", e);
        }
        boolean received = false;
        try {
            JMXConnector connector = task.get(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            received = true;
            return connector;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("JMX connection interrupted", e);
        } catch (ExecutionException | TimeoutException e) {
            throw new IOException("JMX connection failed or timed out", e);
        } finally {
            if (!received) {
                abandoned.set(true);
                task.cancel(true);
                closeAbandoned(opened);
            }
        }
    }

    protected JMXConnector openConnector(JMXServiceURL url) throws IOException {
        return JMXConnectorFactory.connect(url, JMX_ENVIRONMENT);
    }

    private MBeanServerConnection boundedConnection(MBeanServerConnection connection, long deadline) {
        return (MBeanServerConnection) Proxy.newProxyInstance(MBeanServerConnection.class.getClassLoader(),
            new Class<?>[] {MBeanServerConnection.class}, (proxy, method, args) -> {
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0) {
                    throw new SocketTimeoutException("JMX collection read deadline exceeded");
                }
                Future<Object> request;
                try {
                    request = CONNECT_EXECUTOR.submit(() -> method.invoke(connection, args));
                } catch (RejectedExecutionException e) {
                    throw new IOException("JMX request capacity reached", e);
                }
                try {
                    return request.get(remaining, TimeUnit.NANOSECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("JMX read interrupted", e);
                } catch (TimeoutException e) {
                    throw new SocketTimeoutException("JMX collection read deadline exceeded");
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    throw cause instanceof InvocationTargetException invocation ? invocation.getCause() : cause;
                } finally {
                    request.cancel(true);
                }
            });
    }

    private static void closeAbandoned(AtomicReference<JMXConnector> opened) {
        JMXConnector connector = opened.getAndSet(null);
        if (connector != null) {
            try {
                connector.close();
            } catch (IOException e) {
                log.warn("Unable to close timed out JMX connection", e);
            }
        }
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

    /** 一次查询后按标签分组并读取指定指标，调用方无需遍历 Topic 或 MBean。 */
    protected final Map<String, Map<String, Long>> readGroupedLongs(MBeanServerConnection connection, String pattern,
        String groupKey, String metricKey, String attribute, Set<String> metrics) {
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
            groups.computeIfAbsent(group, key -> new HashMap<>()).put(metric, readLong(connection, name.toString(), attribute));
        }
        return groups;
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
