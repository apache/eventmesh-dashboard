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

import org.apache.eventmesh.dashboard.console.entity.message.GroupEntity;
import org.apache.eventmesh.dashboard.console.mapper.message.GroupMapper;
import org.apache.eventmesh.dashboard.core.metadata.DataMetadataHandler;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class GroupDataMetadataHandler implements DataMetadataHandler<GroupEntity> {

    private GroupMapper groupMapper;

    @Override
    public void handleAll(List<GroupEntity> addData, List<GroupEntity> updateData, List<GroupEntity> deleteData) {

    }

    @Override
    public List<GroupEntity> getData() {
        return Collections.emptyList();
    }
}
