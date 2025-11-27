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

package org.apache.eventmesh.dashboard.console.function.report.opensearch;


import org.apache.eventmesh.dashboard.console.function.report.AbstractReportEngine;
import org.apache.eventmesh.dashboard.console.function.report.annotation.AbstractReportMetaHandler;
import org.apache.eventmesh.dashboard.console.function.report.annotation.ReportMetaData;
import org.apache.eventmesh.dashboard.console.function.report.model.SingleGeneralReportDO;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.message.BasicHeader;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.CompletableFuture;

import org.opensearch.client.RestClient;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.Script;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.opensearch.client.opensearch.core.ScriptsPainlessExecuteRequest;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.rest_client.RestClientTransport;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

public class OpenSearchReportEngine extends AbstractReportEngine {

    private OpenSearchClient client;


    public void handler() throws IOException {
        ScriptsPainlessExecuteRequest.Builder builder = new ScriptsPainlessExecuteRequest.Builder();
        Script.Builder script = new Script.Builder();

        //script.source(scriptSourceBuilder.build());
        //builder.script(script.build());

        //client.scriptsPainlessExecute(builder.build());
    }

    @Override
    protected void doInit() {
        try {
            URL url = new URL(this.reportEngineConfig.getEngineAddress());
            HttpHost httpHost = new HttpHost(url.getHost(), url.getPort(),url.getProtocol());
            RestClient restClient = RestClient.builder(httpHost)
                .setDefaultHeaders(new Header[] {
                    new BasicHeader("Accept", "application/json"),
                    new BasicHeader("Content-Type", "application/json")
                }).build();
            final OpenSearchTransport transport = new RestClientTransport(restClient, this.createMapper());
            client = new OpenSearchClient(transport);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected AbstractReportMetaHandler doCreateReportHandler(ReportMetaData reportMetaData, List<Field> fieldList) {
        return null;
    }

    public JacksonJsonpMapper createMapper() {
        JacksonJsonpMapper jacksonJsonpMapper = new JacksonJsonpMapper();
        // 注册Java8时间模块
        JavaTimeModule module = new JavaTimeModule();
        // 配置序列化格式
        module.addSerializer(LocalDateTime.class,
            new LocalDateTimeSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        module.addDeserializer(LocalDateTime.class,
            new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        jacksonJsonpMapper.objectMapper().registerModule(module);

        // 禁用时间戳格式
        jacksonJsonpMapper.objectMapper().disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // 设置时区
        jacksonJsonpMapper.objectMapper().setTimeZone(TimeZone.getTimeZone("GMT+8"));
        return jacksonJsonpMapper;
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
        BulkRequest.Builder bulkRequestBuilder = new BulkRequest.Builder();
//        try {
//            data.forEach((k, v) -> {
//                bulkRequestBuilder.operations((op) -> op
//                    .index(idx -> idx.index(tableName).id(k).document(v)));
//            });
//            this.client.bulk(bulkRequestBuilder.build());
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
    }

    @Override
    public void deleteData() {

    }
}
