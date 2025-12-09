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


package org.apache.eventmesh.dashboard.core.metadata;

import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType;
import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType.FirstToWhom;
import org.apache.eventmesh.dashboard.common.enums.SyncType;
import org.apache.eventmesh.dashboard.common.model.base.BaseClusterIdBase;
import org.apache.eventmesh.dashboard.common.model.base.BaseSyncBase;
import org.apache.eventmesh.dashboard.core.metadata.MetadataSyncManage.MetadataSyncConfig;
import org.apache.eventmesh.dashboard.core.metadata.difference.AbstractDifference;
import org.apache.eventmesh.dashboard.core.metadata.difference.BodyDataDifference;
import org.apache.eventmesh.dashboard.core.metadata.difference.BothCacheDifference;
import org.apache.eventmesh.dashboard.core.metadata.difference.BothNotCacheDifference;
import org.apache.eventmesh.dashboard.core.metadata.difference.BufferDifference;
import org.apache.eventmesh.dashboard.core.metadata.difference.NotDifference;
import org.apache.eventmesh.dashboard.core.metadata.result.MetadataSyncResult;
import org.apache.eventmesh.dashboard.core.metadata.result.MetadataSyncResultHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class MetadataSyncWrapper implements Runnable {

    private Integer period = 200;

    private MetadataSyncResultHandler metadataSyncResultHandler;

    private MetadataSyncConfig metadataSyncConfig;

    private BaseSyncBase baseSyncBase;

    private AbstractDifference execute;

    /**
     * 只读 difference
     */
    private NotDifference readOnlyDifference;

    /**
     * 定时 同步
     */
    private AbstractDifference timingSyncDifference;

    /**
     * FirstToWhom 第一次 同步,
     */
    private BufferDifference firstTimeDifference;

    /**
     * 用于 第一次 数据 check
     */
    private BothNotCacheDifference firstTimeCheckDifference;

    /**
     * 用于 间隙星 check
     */
    private BothCacheDifference checkDifference;

    private volatile boolean check = true;

    private long intervalTime;

    private long intervalBaseNumber = 100L;

    private long syncTime;

    private long checkTime;

    private long errorNextTime = 0L;

    private long currentTime;

    private int fistSyncRetry = 0;

    private AtomicBoolean runStatus = new AtomicBoolean(false);

    private AtomicBoolean initStatus = new AtomicBoolean(false);


    public void createDifference() {
        this.baseSyncBase = this.metadataSyncConfig.getBaseSyncBase();
        if (this.readOnly()) {
            NotDifference notDifference = new NotDifference();
            notDifference.setSourceHandler(this.metadataSyncConfig.getClusterService());
            notDifference.setTargetHandler(this.metadataSyncConfig.getDataBasesHandler());
            this.readOnlyDifference = notDifference;
        }

        this.intervalBaseNumber = this.metadataSyncConfig.getClusterServiceType().isSelf() ? 0 : 1000 * 60 * 10 + new Random().nextInt(1000);
        this.syncTime = System.currentTimeMillis() + this.intervalBaseNumber;

        this.intervalTime = 1000 * 60 * 60 + new Random().nextInt(10000);
        this.intervalTime = 200L;
        this.checkTime = System.currentTimeMillis() + intervalTime;

        // 共享内存
        Map<String, BaseClusterIdBase> allData = new HashMap<>();

        //第一次 同步
        BufferDifference firstTimeDifference = new BufferDifference();
        if (Objects.equals(this.baseSyncBase.getFirstToWhom(), FirstToWhom.RUNTIME)) {
            firstTimeDifference.setSourceHandler(this.metadataSyncConfig.getClusterService());
            firstTimeDifference.setTargetHandler(this.metadataSyncConfig.getDataBasesHandler());
        } else if (Objects.equals(this.baseSyncBase.getFirstToWhom(), FirstToWhom.DASHBOARD)) {
            firstTimeDifference.setSourceHandler(this.metadataSyncConfig.getDataBasesHandler());
            firstTimeDifference.setTargetHandler(this.metadataSyncConfig.getClusterService());
        }
        firstTimeDifference.setAllData(allData);
        this.firstTimeDifference = firstTimeDifference;

        List<AbstractDifference> differences = new ArrayList<>();
        // 定时 同步
        timingSyncDifference =
            this.metadataSyncConfig.getClusterServiceType().isSelf() ? new BodyDataDifference() : new BufferDifference();
        differences.add(timingSyncDifference);
        // 用于 第一次 数据 check
        firstTimeCheckDifference = new BothNotCacheDifference();
        differences.add(firstTimeCheckDifference);
        // 用于 间隙性 check
        checkDifference = new BothCacheDifference();
        differences.add(checkDifference);

        DataMetadataHandler<BaseClusterIdBase> source =
            this.metadataSyncConfig.getClusterServiceType().isSelf() ? this.metadataSyncConfig.getDataBasesHandler()
                : this.metadataSyncConfig.getClusterService();

        DataMetadataHandler<BaseClusterIdBase> target =
            !this.metadataSyncConfig.getClusterServiceType().isSelf() ? this.metadataSyncConfig.getDataBasesHandler()
                : this.metadataSyncConfig.getClusterService();

        differences.forEach((diff) -> {
            diff.setSourceHandler(source);
            diff.setTargetHandler(target);
            diff.setAllData(allData);
        });
        // 在 SyncMetadataCreateFactory 的 DataMetadataHandler 不会缓存所有数据。以集群为主的时候需要查询一次
        if (Objects.equals(this.metadataSyncConfig.getClusterServiceType(), ClusterTrusteeshipType.TRUSTEESHIP_FIND_REVERSE)) {
            DataMetadataHandler<BaseClusterIdBase> dataMetadataHandler = new DataMetadataHandler<>() {
                @Override
                public void handleAll(Collection<BaseClusterIdBase> allData, List<BaseClusterIdBase> addData, List<BaseClusterIdBase> updateData,
                    List<BaseClusterIdBase> deleteData) {
                    target.handleAll(allData, addData, updateData, deleteData);
                }

                @Override
                public List<BaseClusterIdBase> getData() {
                    return target.getData(null);
                }
            };
            firstTimeCheckDifference.setTargetHandler(dataMetadataHandler);
            checkDifference.setTargetHandler(dataMetadataHandler);
        }
    }

    /**
     * TODO 第一阶段：
     *          1. 创建，第一次选择从什么地方同步
     *          2. console 启动时，就对数据进行 check，
     *          3. 激活 cluster or  runtime 对数据进行 check。通过同步状态识别如何操作，必须  没有 条件 一 的同步要求
     *      第二阶段：
     *          1. 如果 从 db 进行 sync，使用 增量模式。BodyDataDifference
     *          2. 如果 从 cluster 同步 ，先与 内存（cache）数据 求差 BufferDifference
     *      第三阶段：
     *          1. 定时 check
     *      check：
     *          1. 先取 source 写入  内存，
     *          2. 拉取 target 与 内存 求差
     *          3. 同步 cluster 数据是否需要 check，
     *              如何保证 database 的数据不会有操作
     *              需要 如何得到这个 runtime 或则 cluster 的信息，不传递 id，复用性不好
     *              cluster 的 check，可以1小时，或则 几个小时一次。应为 check cluster 本省对感知要求不高
     *              database 的 check 可以 一分钟一次，压力会不会很大，这个需要进行计算。
     */
    @Override
    public void run() {
        MetadataSyncResult metadataSyncResult = this.metadataSyncConfig.getMetadataSyncResult();

        try {
            this.currentTime = System.currentTimeMillis();
            if (!this.runStatus.compareAndSet(false, true)) {
                log.info("metadata sync wrapper is already running , metadataType is {} , id is {} , runStatus is {}",
                    metadataSyncConfig.getMetadataType(),
                    baseSyncBase.getId(),
                    this.runStatus.get());
                metadataSyncResult.setSyncType(SyncType.NOT);
                metadataSyncResult.setFirstToWhom(FirstToWhom.NOT_EXECUTED);
                return;
            }
            if (this.isError()) {
                metadataSyncResult.setSyncType(SyncType.NOT);
                metadataSyncResult.setFirstToWhom(FirstToWhom.ERROR_ING);
                return;
            }
            long startTime = System.currentTimeMillis();
            this.doRun(metadataSyncResult);
            if (this.timeout(startTime)) {
                log.warn("metadata sync wrapper is timeout , metadataType is {} , id is {} , runStatus is {}",
                    metadataSyncConfig.getMetadataType(),
                    baseSyncBase.getId(),
                    this.runStatus.get());
            }
        } catch (Exception e) {
            log.error("metadata sync wrapper run exception", e);
        } finally {
            this.runStatus.set(false);
            this.metadataSyncResultHandler.handleMetadataSyncResult(metadataSyncResult);
        }
    }


    private void doRun(MetadataSyncResult metadataSyncResult) {
        try {
            this.calculate(metadataSyncResult);
            if (log.isTraceEnabled()) {
                log.trace("#sync  metadata type {} , source type {} , source id {}, cluster type {}, syncType is {}",
                    metadataSyncConfig.getMetadataType(),
                    metadataSyncConfig.getBaseSyncBase().getClass().getSimpleName(),
                    metadataSyncConfig.getBaseSyncBase().getId(),
                    metadataSyncConfig.getBaseSyncBase().getClusterType(),
                    metadataSyncResult.getSyncType()
                );
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        if (Objects.isNull(this.execute)) {
            return;
        }
        try {
            this.baseSyncBase.setFirstSyncState(FirstToWhom.UNDER_WAY);
            this.execute.difference();
            if (this.isFirstToWhom()) {
                baseSyncBase.setFirstSyncState(FirstToWhom.COMPLETE);
            }
            metadataSyncResult.setFirstToWhom(FirstToWhom.COMPLETE);
            this.resetErrorTime();
        } catch (Exception e) {
            if (this.isFirstToWhom()) {
                baseSyncBase.setFirstSyncState(FirstToWhom.ERROR_ING);
            }
            // 失败三次，停止同步吗？
            this.errorHandler(metadataSyncResult);
            log.error(e.getMessage(), e);
        }

    }

    private void calculate(MetadataSyncResult metadataSyncResult) {
        if (this.readOnly()) {
            this.execute = this.readOnlyDifference;
            metadataSyncResult.setSyncType(SyncType.READONLY);
            return;
        }
        if (this.isFirstToWhom()) {
            this.execute = this.firstTimeDifference;
            metadataSyncResult.setSyncType(SyncType.FIRSTTOWHOM);
            this.initStatus.set(true);
            return;
        }
        if (this.isInit()) {
            this.execute = this.firstTimeCheckDifference;
            metadataSyncResult.setSyncType(SyncType.INIT);
            return;
        }
        if (this.check()) {
            this.execute = this.checkDifference;
            metadataSyncResult.setSyncType(SyncType.CHECK);
            return;
        }
        if (this.isTimingSync()) {
            this.execute = this.timingSyncDifference;
            metadataSyncResult.setSyncType(SyncType.TIMINGSYNC);
            return;
        }

        metadataSyncResult.setSyncType(SyncType.NOT);
    }


    private boolean isError() {
        return this.currentTime < this.errorNextTime;
    }

    private void resetErrorTime() {
        if (this.isError()) {
            this.errorNextTime = 0;
            this.fistSyncRetry = 0;
        }
    }

    private void errorHandler(MetadataSyncResult metadataSyncResult) {
        this.fistSyncRetry++;
        int multiple = this.fistSyncRetry / 3;
        int baseNum = 1;
        for (int i = 1; i <= multiple; i++) {
            if (multiple > 5) {
                break;
            }
            baseNum = baseNum * 10 * i;
        }
        this.errorNextTime = this.currentTime + baseNum * 100L;

        baseSyncBase.setFirstSyncState(FirstToWhom.FAIL);
        metadataSyncResult.setFirstToWhom(FirstToWhom.FAIL);
    }


    /**
     * databases 同步 到 cluster 的频率要快 cluster 同步到  databases 10分钟一次
     */
    private boolean isTimingSync() {
        if (this.check()) {
            return false;
        }
        if (this.metadataSyncConfig.getClusterServiceType().isSelf()) {
            return true;
        }
        if (this.currentTime < this.syncTime) {
            return false;
        }
        this.syncTime = this.currentTime + this.intervalBaseNumber;
        return true;
    }

    private boolean isInit() {
        if (this.readOnly()) {
            return false;
        }
        if (this.initStatus.get()) {
            return false;
        }
        return this.initStatus.compareAndSet(false, true);
    }

    private boolean readOnly() {
        return this.metadataSyncConfig.getMetadataType().isReadOnly();
    }

    private boolean check() {
        if (!this.check) {
            return false;
        }
        if (this.currentTime < this.checkTime) {
            return false;
        }
        this.checkTime = this.currentTime + this.intervalTime;
        return true;
    }

    private boolean isFirstToWhom() {
        if (Objects.equals(baseSyncBase.getFirstSyncState(), FirstToWhom.COMPLETE)) {
            return false;
        }
        // firstToWhom 只有三个状态 NOT ， RUNTIME ， STORAGE
        if (Objects.equals(baseSyncBase.getFirstToWhom(), FirstToWhom.NOT)) {
            return false;
        }
        if (Objects.equals(baseSyncBase.getFirstSyncState(), FirstToWhom.WAIT_START)) {
            return true;
        }
        return Objects.equals(baseSyncBase.getFirstSyncState(), FirstToWhom.ERROR_ING);
    }


    private boolean timeout(long startTime) {
        long endTime = System.currentTimeMillis();
        return endTime - startTime > this.period.longValue();
    }

    @Deprecated
    private boolean isSyncDatabasesData() {
        if (Objects.equals(baseSyncBase.getFirstToWhom(), FirstToWhom.DASHBOARD)
            && Objects.equals(baseSyncBase.getFirstSyncState(), FirstToWhom.WAIT_START)) {
            baseSyncBase.setFirstSyncState(FirstToWhom.UNDER_WAY);
            return true;
        }
        if (Objects.equals(baseSyncBase.getTrusteeshipType(), ClusterTrusteeshipType.SELF)) {
            return true;
        }
        return Objects.equals(baseSyncBase.getTrusteeshipType(), ClusterTrusteeshipType.TRUSTEESHIP);
    }

    @Deprecated
    private boolean isSyncClusterData() {
        if (Objects.equals(baseSyncBase.getFirstToWhom(), FirstToWhom.RUNTIME)
            && !Objects.equals(baseSyncBase.getFirstSyncState(), FirstToWhom.WAIT_START)) {
            baseSyncBase.setFirstSyncState(FirstToWhom.UNDER_WAY);
            return true;
        }
        if (Objects.equals(baseSyncBase.getTrusteeshipType(), ClusterTrusteeshipType.TRUSTEESHIP_FIND)) {
            return true;
        }
        return Objects.equals(baseSyncBase.getTrusteeshipType(), ClusterTrusteeshipType.TRUSTEESHIP_FIND_REVERSE);
    }

}