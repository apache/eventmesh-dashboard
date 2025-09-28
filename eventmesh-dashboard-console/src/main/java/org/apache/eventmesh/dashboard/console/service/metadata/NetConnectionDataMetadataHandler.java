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

import org.apache.eventmesh.dashboard.console.entity.cluster.NetConnectionEntity;
import org.apache.eventmesh.dashboard.core.metadata.DataMetadataHandler;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class NetConnectionDataMetadataHandler implements DataMetadataHandler<NetConnectionEntity> {

    @Override
    public void handleAll(Collection<NetConnectionEntity> allData, List<NetConnectionEntity> addData, List<NetConnectionEntity> updateData, List<NetConnectionEntity> deleteData) {

    }

    @Override
    public List<NetConnectionEntity> getData() {
        return Collections.emptyList();
    }
}
