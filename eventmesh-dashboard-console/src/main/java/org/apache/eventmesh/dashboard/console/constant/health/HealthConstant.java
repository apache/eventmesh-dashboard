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

package org.apache.eventmesh.dashboard.console.constant.health;

public class HealthConstant {
    public static final String NEW_LINE_ENDING = "\n";

    public static final String RUNTIME_CHECK_TOPIC = "eventmesh-dashboard-healthcheck-topic";

    public static final String RUNTIME_CHECK_CONTENT_KEY = "eventmesh-dashboard-healthcheck-content";

    public static final String RUNTIME_CHECK_CONTENT_BODY = "eventmesh-dashboard-healthcheck-content-body";

    public static final String CLOUDEVENT_CONTENT_TYPE = "application/cloudevents+json";


    public static final String ENV = "test";
    public static final String HOST = "localhost";
    public static final String PASSWORD = "PASSWORD";
    public static final String USER_NAME = "PU4283";
    public static final String GROUP = "EventmeshTestGroup";
    public static final String PATH = "/data/app/umg_proxy";
    public static final Integer PORT = 8362;
    public static final String VERSION = "2.0.11";
    public static final String IDC = "FT";
    public static final String SUBSYSTEM = "5023";

    public static final String ROCKETMQ_CHECK_PRODUCER_GROUP = "eventmesh-dashboard-healthcheck-rocketmq-producer-group";

    public static final String ROCKETMQ_CHECK_CONSUMER_GROUP = "eventmesh-dashboard-healthcheck-rocketmq-consumer-group";

    public static final String ROCKETMQ_CHECK_TOPIC = "DO-NOT-USE-THIS-TOPIC-"
        + "eventmesh-dashboard-healthcheck-rocketmq-topic-90a78a5d-b803-447e-8c48-1c87ab0c74d9";
    public static final String ROCKETMQ_CHECK_TOPIC_MSG = "eventmesh-dashboard-healthcheck-rocketmq-message";

    public static final String NACOS_CHECK_DATA_ID = "DO-NOT-USE-THIS-"
        + "eventmesh-dashboard-healthcheck-nacos-data-id-28e2933f-a47b-439d-b14b-7d9970c37042";

    public static final String NACOS_CHECK_GROUP = "EVENTMESH_DASHBOARD_HEALTH_CHECK_GROUP";

    public static final String NACOS_CHECK_CONTENT = "eventmesh-dashboard";

    public static final String NACOS_CHECK_SERVICE_NAME = "eventmesh-dashboard-healthcheck-nacos-service-name";

    public static final String NACOS_CHECK_SERVICE_CLUSTER_NAME = "eventmesh-dashboard-healthcheck-nacos-service-cluster-name";
}
