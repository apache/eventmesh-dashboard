/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.eventmesh.dashboard.console.controller;

import org.apache.eventmesh.dashboard.console.function.report.ReportHandlerManage;
import org.apache.eventmesh.dashboard.console.function.report.model.MultiGeneralReportDO;
import org.apache.eventmesh.dashboard.console.function.report.model.SingleGeneralReportDO;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("report")
public class ReportController {


    private ReportHandlerManage reportHandlerManage;

    @RequestMapping("reportByHome")
    public void reportByHome(@RequestBody MultiGeneralReportDO multiGeneralReportDO) {
        //
        List<SingleGeneralReportDO> singleGeneralReportDOList = new ArrayList<>();
        multiGeneralReportDO.getReportNameList().forEach(reportName -> {
            SingleGeneralReportDO singleGeneralReportDO = new SingleGeneralReportDO();
            singleGeneralReportDOList.add(singleGeneralReportDO);


        });

        singleGeneralReportDOList.forEach(singleGeneralReportDO -> {

        });
    }

    public void reportBySingle(@RequestBody SingleGeneralReportDO singleGeneralReportDO) {

    }

    public void reportByMulti() {
    }

}
