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

package org.apache.eventmesh.dashboard.console.controller;

import org.apache.eventmesh.dashboard.console.entity.serviceuser.ServiceUserEntity;
import org.apache.eventmesh.dashboard.console.service.serviceuser.ServiceUserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/serviceUser")
public class ServiceUserController {

    @Autowired
    private ServiceUserService serviceUserService;

    @PostMapping("/insertServiceUser")
    public void insertServiceUser(@RequestBody ServiceUserEntity serviceUserEntity) {
        this.serviceUserService.insert(serviceUserEntity);
    }

    @PostMapping("/deleteServiceUserByCluster")
    public void deleteServiceUserByCluster(@RequestBody ServiceUserEntity serviceUserEntity) {
        this.serviceUserService.deleteServiceUserByCluster(serviceUserEntity);
    }

    @PostMapping("/updateNameById")
    public void updateNameById(@RequestBody ServiceUserEntity serviceUserEntity) {
        this.serviceUserService.updatePasswordById(serviceUserEntity);
    }

    @PostMapping("/selectAll")
    public void selectAll() {

    }

    @PostMapping("/selectById")
    public void selectById(@RequestBody ServiceUserEntity serviceUserEntity) {
        this.serviceUserService.selectById(serviceUserEntity);
    }

    @PostMapping("/selectByName")
    public void selectByName(@RequestBody ServiceUserEntity serviceUserEntity) {
        this.serviceUserService.selectByName(serviceUserEntity);
    }

}
