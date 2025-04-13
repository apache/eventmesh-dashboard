package org.apache.eventmesh.dashboard.core.metadata;

import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType;
import org.apache.eventmesh.dashboard.common.enums.ClusterTrusteeshipType.FirstToWhom;
import org.apache.eventmesh.dashboard.common.model.base.BaseSyncBase;
import org.apache.eventmesh.dashboard.core.metadata.MetadataSyncManage.MetadataSyncConfig;
import org.apache.eventmesh.dashboard.core.metadata.difference.BodyDataDifference;
import org.apache.eventmesh.dashboard.core.metadata.difference.BothDifference;
import org.apache.eventmesh.dashboard.core.metadata.difference.BufferDifference;
import org.apache.eventmesh.dashboard.core.metadata.difference.NotDifference;
import org.apache.eventmesh.dashboard.core.metadata.result.MetadataSyncResult;
import org.apache.eventmesh.dashboard.core.metadata.result.MetadataSyncResultHandler;

import java.util.Objects;
import java.util.Random;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class MetadataSyncWrapper implements Runnable {

    private MetadataSyncResultHandler metadataSyncResultHandler;

    private MetadataSyncConfig metadataSyncConfig;

    private BaseSyncBase baseSyncBase;

    private NotDifference notDifference;

    private BodyDataDifference bodyDataDifference;

    private BufferDifference bufferDifference;

    private BothDifference bothDifference;


    private volatile boolean check = false;

    private long intervalTime;

    private long checkTime;

    private int fistSyncRetry = 0;


    public void createDifference() {
        this.baseSyncBase = this.metadataSyncConfig.getBaseSyncBase();
        if (this.readOnly()) {
            NotDifference notDifference = new NotDifference();
            notDifference.setSourceHandler(this.metadataSyncConfig.getClusterService());
            notDifference.setTargetHandler(this.metadataSyncConfig.getDataBasesHandler());
            this.notDifference = notDifference;
            return;
        }
        this.intervalTime = 1000 * 60 * 60 + new Random().nextInt(10000);

        this.checkTime = System.currentTimeMillis() - intervalTime;
        BodyDataDifference bodyDataDifference = new BodyDataDifference();
        bodyDataDifference.setSourceHandler(this.metadataSyncConfig.getClusterService());
        bodyDataDifference.setTargetHandler(this.metadataSyncConfig.getDataBasesHandler());
        this.bodyDataDifference = bodyDataDifference;

        BufferDifference bufferDifference = new BufferDifference();
        bufferDifference.setSourceHandler(this.metadataSyncConfig.getDataBasesHandler());
        bufferDifference.setTargetHandler(this.metadataSyncConfig.getClusterService());
        this.bufferDifference = bufferDifference;

        BothDifference bothDifference = new BothDifference();
        if (this.metadataSyncConfig.getClusterServiceType().isSelf()) {
            bothDifference.setSourceHandler(this.metadataSyncConfig.getDataBasesHandler());
            bothDifference.setTargetHandler(this.metadataSyncConfig.getClusterService());
        } else {
            bothDifference.setSourceHandler(this.metadataSyncConfig.getClusterService());
            bothDifference.setTargetHandler(this.metadataSyncConfig.getDataBasesHandler());
        }
        this.bufferDifference = bufferDifference;
    }


    @Override
    public void run() {
        MetadataSyncResult metadataSyncResult = this.metadataSyncConfig.getMetadataSyncResult();
        try {
            long lastTime = System.currentTimeMillis();
            if (this.fistSyncRetry >= 5) {
                log.error("baseSyncBase {} fistSyncRetry 5", baseSyncBase.getId());
            }
            if (this.readOnly()) {
                this.notDifference.difference();
            } else if (this.check()) {
                this.bothDifference.difference();
            } else if (this.isSyncClusterData()) {
                this.bufferDifference.difference();
            } else if (this.isSyncDatabasesData()) {
                this.bodyDataDifference.difference();
            }
            if (Objects.equals(baseSyncBase.getFirstSyncState(), FirstToWhom.UNDER_WAY)) {
                baseSyncBase.setFirstSyncState(FirstToWhom.COMPLETE);
                metadataSyncResult.setFirstToWhom(FirstToWhom.COMPLETE);
            }
        } catch (Exception e) {
            // 失败三次，停止同步吗？
            if (Objects.equals(baseSyncBase.getFirstSyncState(), FirstToWhom.UNDER_WAY)) {
                this.fistSyncRetry++;
                baseSyncBase.setFirstSyncState(FirstToWhom.WAIT_START);
                metadataSyncResult.setFirstToWhom(FirstToWhom.FAIL);
            }
            log.error(e.getMessage(), e);
        } finally {
            MetadataSyncManage.this.metadataSyncResultHandler.handleMetadataSyncResult(metadataSyncResult);
        }
    }


    private boolean readOnly() {
        return this.metadataSyncConfig.getMetadataType().isReadOnly();
    }

    private boolean check() {
        if (!this.check) {
            return false;
        }
        if (System.currentTimeMillis() < this.checkTime) {
            return false;
        }
        this.checkTime = System.currentTimeMillis() + this.intervalTime;
        return true;
    }


    private boolean isSyncClusterData() {
        if (Objects.equals(baseSyncBase.getFirstToWhom(), FirstToWhom.RUNTIME)
            && Objects.equals(baseSyncBase.getFirstSyncState(), FirstToWhom.WAIT_START)) {
            baseSyncBase.setFirstSyncState(FirstToWhom.UNDER_WAY);
            return true;
        }
        if (Objects.equals(baseSyncBase.getTrusteeshipType(), ClusterTrusteeshipType.SELF)) {
            return true;
        }
        return Objects.equals(baseSyncBase.getTrusteeshipType(), ClusterTrusteeshipType.TRUSTEESHIP);

    }

    private boolean isSyncDatabasesData() {
        if (Objects.equals(baseSyncBase.getFirstToWhom(), FirstToWhom.DASHBOARD)
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