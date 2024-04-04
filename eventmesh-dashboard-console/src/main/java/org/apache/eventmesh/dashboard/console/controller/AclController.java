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

import org.apache.eventmesh.dashboard.console.entity.acl.AclEntity;
import org.apache.eventmesh.dashboard.console.service.acl.AclService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/acl")
public class AclController {

    @Autowired
    private AclService aclService;

    @PostMapping("/insertAcl")
    public void insertAcl(@RequestBody AclEntity aclEntity) {
        this.aclService.insert(aclEntity);
    }

    @PostMapping("deleteAcl")
    public void deleteAcl(@RequestBody AclEntity aclEntity) {
        this.aclService.deleteAclById(aclEntity);
    }

    @PostMapping("/updateAcl")
    public void updateAcl(@RequestBody AclEntity aclEntity) {
        this.aclService.updateResourceTypeById(aclEntity);
    }

    @PostMapping("/selectAcl")
    public void selectAcl(@RequestBody AclEntity aclEntity) {
        this.aclService.selectById(aclEntity);
    }

}
