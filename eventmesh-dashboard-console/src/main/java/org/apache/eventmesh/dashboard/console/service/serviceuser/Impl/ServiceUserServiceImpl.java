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

package org.apache.eventmesh.dashboard.console.service.serviceuser.Impl;

import org.apache.eventmesh.dashboard.console.entity.serviceuser.ServiceUserEntity;
import org.apache.eventmesh.dashboard.console.mapper.serviceuser.ServiceUserMapper;
import org.apache.eventmesh.dashboard.console.service.serviceuser.ServiceUserService;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional

@Service
public class ServiceUserServiceImpl implements ServiceUserService {

    @Autowired
    private ServiceUserMapper serviceUserMapper;

    @Override
    public void insert(ServiceUserEntity serviceuserEntity) {
        serviceUserMapper.insert(serviceuserEntity);
    }

    @Override
    public void deleteServiceUserByCluster(ServiceUserEntity serviceuserEntity) {
        serviceUserMapper.deleteServiceUserByCluster(serviceuserEntity);
    }

    @Override
    public void updatePasswordById(ServiceUserEntity serviceuserEntity) {
        serviceUserMapper.updatePasswordById(serviceuserEntity);
    }

    @Override
    public List<ServiceUserEntity> selectAll() {
        return serviceUserMapper.selectAll();
    }

    @Override
    public ServiceUserEntity selectById(ServiceUserEntity serviceuserEntity) {
        return serviceUserMapper.selectById(serviceuserEntity);
    }

    @Override
    public List<ServiceUserEntity> selectByName(ServiceUserEntity serviceuserEntity) {
        return serviceUserMapper.selectByName(serviceuserEntity);
    }

}
