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

package org.apache.eventmesh.dashboard.console.function.report.elasticsearch;


import org.apache.eventmesh.dashboard.console.function.report.AbstractReportEngine;
import org.apache.eventmesh.dashboard.console.function.report.annotation.AbstractReportMetaHandler;
import org.apache.eventmesh.dashboard.console.function.report.annotation.ReportMetaData;
import org.apache.eventmesh.dashboard.console.function.report.model.SingleGeneralReportDO;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ElasticsearchReportEngine extends AbstractReportEngine {

    @Override
    protected void doInit() {

    }

    @Override
    protected AbstractReportMetaHandler doCreateReportHandler(ReportMetaData reportMetaData, List<Field> fieldList) {
        return null;
    }

    @Override
    public CompletableFuture<List<Map<String, Object>>> query(SingleGeneralReportDO singleGeneralReportDO) {
        return null;
    }

    @Override
    public void createReport(String tableName) {

    }

    @Override
    public void batchInsert(String tableName, List<Object> data) {

    }

//    private ElasticsearchClient esClient;
//
//
//
//    public void handler() throws IOException {
//        ScriptsPainlessExecuteRequest.Builder builder = new ScriptsPainlessExecuteRequest.Builder();
//        Script.Builder script = new Script.Builder();
//
//        ScriptSource.Builder scriptSourceBuilder = new ScriptSource.Builder();
//        scriptSourceBuilder.scriptString("");
//        script.source(scriptSourceBuilder.build());
//        builder.script(script.build());
//
//        esClient.scriptsPainlessExecute(builder.build());
//    }
//
//    @Override
//    protected void doInit() {
//        try {
//            URL url = new URL(this.reportConfig.getEngineAddress());
//            HttpHost httpHost = new HttpHost(url.getHost(), url.getPort(), url.getProtocol());
//            RestClient restClient = RestClient.builder(httpHost).build();
//            this.esClient = new ElasticsearchClient(
//                new RestClientTransport(restClient, new JacksonJsonpMapper())
//            );
//        } catch (MalformedURLException e) {
//            throw new RuntimeException(e);
//        }
//
//    }
//
//    @Override
//    public CompletableFuture<List<Map<String, Object>>> query(SingleGeneralReportDO singleGeneralReportDO) {
//        return null;
//    }
//
//    @Override
//    public void batchInsert(String tableName, Map<String, Object> data) {
//
//    }
}
