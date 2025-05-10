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


package org.apache.eventmesh.dashboard.console.controller.deploy;


import org.apache.eventmesh.dashboard.console.entity.cases.CaseEntity;
import org.apache.eventmesh.dashboard.console.mapstruct.deploy.CaseControllerMapper;
import org.apache.eventmesh.dashboard.console.modle.IdDTO;
import org.apache.eventmesh.dashboard.console.modle.deploy.cases.QueryCaseByObjectTypeDTO;

import java.util.List;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("case")
public class CaseController {


    public List<CaseEntity> queryCaseByObjectType(@RequestBody QueryCaseByObjectTypeDTO queryCaseByObjectTypeDTO) {
        CaseControllerMapper.INSTANCE.queryCaseByObjectType(queryCaseByObjectTypeDTO);
        return null;
    }

    public Integer deleteCase(@RequestBody IdDTO idDTO) {
        return 0;
    }

    public List<CaseEntity> queryCaseByOrganization() {
        return null;
    }


    public void backups(){

    }

    public void createCase(@RequestBody CaseEntity caseEntity) {
        // 新增，状态进行中信

        try {
            // clone object 数据

            // 标记成功
        } catch (Exception e) {
            // 回滚 标记失败
            throw new RuntimeException(e);
        }


    }

}
