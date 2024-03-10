package org.apache.eventmesh.dashboard.console.entity.meta;

import lombok.Data;

import org.apache.eventmesh.dashboard.console.entity.base.BaseEntity;

import java.time.LocalDateTime;

@Data
public class RuntimeEntity1 extends BaseEntity {


    private String serviceAddress;

    private String servicePost;

    private String serviceVersion;

    private String serviceName;

    private String status;

    private LocalDateTime beOnlineTime;

    private LocalDateTime beOfflineTime;

}
