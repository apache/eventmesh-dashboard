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

import java.util.Objects;

public enum ClusterFramework {

    NOT,

    INDEPENDENCE,

    /**
     *  AP 架构，的 meta 有控制行为，需要逐一通知
     */
    AP,

    CP,

    CAP,

    MAIN_SLAVE,

    PAXOS,

    RAFT,

    ZK,
    ;

    public boolean isIndependence() {
        return this == ClusterFramework.INDEPENDENCE;
    }

    public boolean isAP() {
        return Objects.equals(this, AP);
    }

    public boolean isCP() {
        return Objects.equals(this, CP);
    }

    public boolean isMainSlave() {
        return Objects.equals(this, MAIN_SLAVE);
    }

    public boolean isCAP() {
        return Objects.equals(this, ZK) || Objects.equals(this, RAFT) || Objects.equals(this, PAXOS) || Objects.equals(this, CAP);
    }

    public boolean isNot() {
        return this == NOT;
    }
}