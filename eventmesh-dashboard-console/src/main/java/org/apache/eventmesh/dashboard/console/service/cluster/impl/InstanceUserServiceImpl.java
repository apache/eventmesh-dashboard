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

package org.apache.eventmesh.dashboard.console.service.cluster.impl;

import org.apache.eventmesh.dashboard.console.entity.cluster.InstanceUserEntity;
import org.apache.eventmesh.dashboard.console.mapper.cluster.InstanceUserMapper;
import org.apache.eventmesh.dashboard.console.service.cluster.InstanceUserService;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional

@Service
public class InstanceUserServiceImpl implements InstanceUserService {

    @Autowired
    private InstanceUserMapper instanceUserMapper;

    @Override
    public void insert(InstanceUserEntity instanceuserEntity) {
        instanceUserMapper.insert(instanceuserEntity);
    }

    @Override
    public void deleteInstanceUserByCluster(InstanceUserEntity instanceuserEntity) {
        instanceUserMapper.deleteInstanceUserById(instanceuserEntity);
    }

    @Override
    public void updatePasswordById(InstanceUserEntity instanceuserEntity) {
        instanceUserMapper.updatePasswordById(instanceuserEntity);
    }

    @Override
    public List<InstanceUserEntity> selectAll() {
        return instanceUserMapper.selectAll();
    }

    @Override
    public InstanceUserEntity selectById(InstanceUserEntity instanceuserEntity) {
        return instanceUserMapper.selectById(instanceuserEntity);
    }

    @Override
    public List<InstanceUserEntity> selectByName(InstanceUserEntity instanceuserEntity) {
        return instanceUserMapper.selectByName(instanceuserEntity);
    }

}
