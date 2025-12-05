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


package org.apache.eventmesh.dashboard.common.enums.health;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum HealthCheckStatus {

    ING(1L, "ing"),

    SUCCESS(1L, "success"),

    FAILED(0L, "failed"),

    PASSED(1L, "passed"),

    CHECKING(2L, "checking"),

    TIMEOUT(3L, "timeout"),

    NOT_CONNECTED(4L, "not connected"),

    USER_AUTHENTICATION_FAIL(5L, "user authentication fail"),

    REQUEST_AUTHENTICATION_FAIL(6L, "request authentication fail"),
    ;

    private final Long number;

    private final String name;
}
