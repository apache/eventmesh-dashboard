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


package org.apache.eventmesh.dashboard.common.enums;

public enum DeployStatusType {


    RESOURCE_APPLY,

    RESOURCE_APPLY_FAILED,

    CREATE_WAIT,

    CREATE_FULL_WAIT,

    CREATE_WAIT_TIMEOUT,

    CREATE_CAP_UPDATE_WAIT,

    CREATE_CAP_UPDATE_WAIT_TIMEOUT,

    CREATE_ING,

    CREATE_CAP_UPDATE_ING,

    CREATE_SUCCESS,

    CREATE_CAP_UPDATE_SUCCESS,

    UPDATE_WAIT,

    UPDATE_ING,

    PAUSE_WAIT,

    PAUSE_ING,

    PAUSE_FULL_WAIT,

    PAUSE_SUCCESS,

    PAUSE_FAIL,

    RESET_WAIT,

    RESET_ING,

    RESET_SUCCESS,

    UNINSTALL,

    UNINSTALL_FAILED,
    ;
}
