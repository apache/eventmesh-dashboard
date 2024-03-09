package org.apache.eventmesh.dashboard.common.properties;

import lombok.Data;

@Data
public class RocketmqProperties {
    private String namesrvAddr;

    private String clusterName;

    private String brokerUrl;

    private String endPoint;

    private int writeQueueNums;

    private int readQueueNums;

    private String accessKey;

    private String secretKey;

    private Long requestTimeoutMillis = 10000L;
}
