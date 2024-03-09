package org.apache.eventmesh.dashboard.common.properties;

import lombok.Data;

@Data
public class RocketmqProperties {
    private String namesrvAddr;

    private String clusterName;

    private String brokerUrl;

    private String endPoint;

    private int readQueueNums;

    private int writeQueueNums;

    private String accessKey;

    private String secretKey;
}
