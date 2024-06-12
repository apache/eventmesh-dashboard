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

package org.apache.eventmesh.dashboard.console.controller.cluster;

import org.apache.eventmesh.dashboard.console.entity.cluster.InstanceUserEntity;
import org.apache.eventmesh.dashboard.console.service.cluster.InstanceUserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/instanceUser")
public class InstanceUserController {

    @Autowired
    private InstanceUserService instanceUserService;

    @PostMapping("/insertInstanceUser")
    public void insertInstanceUser(@RequestBody InstanceUserEntity instanceUserEntity) {
        this.instanceUserService.insert(instanceUserEntity);
    }

    @PostMapping("/deleteInstanceUserByCluster")
    public void deleteInstanceUserByCluster(@RequestBody InstanceUserEntity instanceUserEntity) {
        this.instanceUserService.deleteInstanceUserByCluster(instanceUserEntity);
    }

    @PostMapping("/updateNameById")
    public void updateNameById(@RequestBody InstanceUserEntity instanceUserEntity) {
        this.instanceUserService.updatePasswordById(instanceUserEntity);
    }

    @PostMapping("/selectAll")
    public void selectAll() {

    }

    @PostMapping("/selectById")
    public void selectById(@RequestBody InstanceUserEntity instanceUserEntity) {
        this.instanceUserService.selectById(instanceUserEntity);
    }

    @PostMapping("/selectByName")
    public void selectByName(@RequestBody InstanceUserEntity instanceUserEntity) {
        this.instanceUserService.selectByName(instanceUserEntity);
    }

}
