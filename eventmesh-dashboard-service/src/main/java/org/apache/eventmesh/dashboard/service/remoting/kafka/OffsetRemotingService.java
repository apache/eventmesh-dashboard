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


package org.apache.eventmesh.dashboard.service.remoting.kafka;

import org.apache.eventmesh.dashboard.common.model.remoting.offset.GetOffsetRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.offset.GetOffsetResult;
import org.apache.eventmesh.dashboard.common.model.remoting.offset.ResetOffsetRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.offset.ResetOffsetResult;

/** Explicit parameter-driven operations, not metadata synchronization actions. */
public interface OffsetRemotingService {

    /** Omitted topic queries committed partitions; specified topic queries all its partitions. */
    GetOffsetResult getOffsets(GetOffsetRequest request) throws Exception;

    /** Topic is required; omitted partitionId targets all partitions of that topic. */
    ResetOffsetResult resetOffsets(ResetOffsetRequest request) throws Exception;
}
