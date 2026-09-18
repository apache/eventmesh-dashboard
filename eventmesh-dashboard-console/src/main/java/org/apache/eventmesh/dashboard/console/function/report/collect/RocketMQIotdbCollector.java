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


package org.apache.eventmesh.dashboard.console.function.report.collect;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/** Run only the five metric schedules, without Spring Boot, MySQL or unrelated Dashboard initializers. */
public final class RocketMQIotdbCollector {
    private RocketMQIotdbCollector() {
    }

    public static void main(String[] args) {
        if (args.length != 6) {
            throw new IllegalArgumentException("Arguments: brokerHost brokerPort iotdbHost:port organizationId clusterId runtimeId");
        }
        String[] properties = {"broker-host", "broker-port", "iotdb-address", "organization-id", "cluster-id", "runtime-id"};
        System.setProperty("rocketmq.metrics.enabled", "true");
        for (int i = 0; i < properties.length; i++) {
            System.setProperty("rocketmq.metrics." + properties[i], args[i]);
        }
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(RocketMQMetricsConfiguration.class);
        context.registerShutdownHook();
    }
}
