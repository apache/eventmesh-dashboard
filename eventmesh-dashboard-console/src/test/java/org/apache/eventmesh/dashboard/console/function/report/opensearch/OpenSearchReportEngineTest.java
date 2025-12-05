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

package org.apache.eventmesh.dashboard.console.function.report.opensearch;

import org.apache.eventmesh.dashboard.console.function.report.DataIdModel;
import org.apache.eventmesh.dashboard.console.function.report.DataValueModel;
import org.apache.eventmesh.dashboard.console.function.report.ReportConfig.ReportEngineConfig;
import org.apache.eventmesh.dashboard.console.function.report.elasticsearch.DataModel;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class OpenSearchReportEngineTest {

    private OpenSearchReportEngine engine = new OpenSearchReportEngine();

    @Before
    public void init() {
        ReportEngineConfig reportEngineConfig = new ReportEngineConfig();
        reportEngineConfig.setEngineAddress("http://localhost:9200");
        engine.setReportEngineConfig(reportEngineConfig);
        engine.init();
    }

    @Test
    public void addData() {
        DataIdModel dataIdModel = new DataIdModel();
        dataIdModel.setCount(3);
        List<DataModel> list = dataIdModel.getRuntimeDataModel();
        DataValueModel dataValueModel = new DataValueModel();
        List<? extends Object> objectList = list;
        this.engine.batchInsert("tstttt", (List<Object>) objectList);


    }

}
