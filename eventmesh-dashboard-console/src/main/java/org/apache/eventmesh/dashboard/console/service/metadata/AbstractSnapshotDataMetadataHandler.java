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


package org.apache.eventmesh.dashboard.console.service.metadata;

import org.apache.eventmesh.dashboard.core.metadata.DataMetadataHandler;

import java.util.Collection;
import java.util.List;

/** These sources read complete snapshots, so a failed destination can retry without losing a watermark. */
public abstract class AbstractSnapshotDataMetadataHandler<T> implements DataMetadataHandler<T> {

    @Override
    public void handleAll(Collection<T> allData, List<T> addData, List<T> updateData, List<T> deleteData) {
        throw new UnsupportedOperationException("This metadata path is database-to-destination only");
    }
}
