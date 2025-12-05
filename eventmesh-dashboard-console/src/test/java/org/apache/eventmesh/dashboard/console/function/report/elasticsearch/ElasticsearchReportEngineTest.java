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

package org.apache.eventmesh.dashboard.console.function.report.elasticsearch;

public class ElasticsearchReportEngineTest {

//    private Long clusterId = System.currentTimeMillis();
//
//
//    private ElasticsearchReportEngine elasticsearchReportEngine = new ElasticsearchReportEngine();
//
//    private ElasticsearchClient esClient;
//
//    private OpenSearchClient client;
//
//    private List<DataModelBuilder> builders;
//
//    @Before
//    public void init() {
//        HttpHost httpHost = new HttpHost("localhost", 9200, "http");
//        RestClient restClient = RestClient.builder(httpHost).build();
//        this.esClient = new ElasticsearchClient(
//            new RestClientTransport(restClient, new JacksonJsonpMapper())
//        );
//        ReportConfig reportConfig = new ReportConfig();
//        reportConfig.setEngineAddress("http://localhost:9200");
//        elasticsearchReportEngine.setReportConfig(reportConfig);
//        elasticsearchReportEngine.init();
//        this.init2();
//    }
//
//
//    public void init2() {
//        // 使用 RestClient 传输层
//        org.opensearch.client.RestClient restClient =
//            org.opensearch.client.RestClient.builder(new org.apache.hc.core5.http.HttpHost("localhost", 9200))
//                .setDefaultHeaders(new Header[] {
//                new BasicHeader("Accept", "application/json"),
//                new BasicHeader("Content-Type", "application/json")
//            }).build();
//        client = new OpenSearchClient(
//            new org.opensearch.client.transport.rest_client.RestClientTransport(restClient,
//                new org.opensearch.client.json.jackson.JacksonJsonpMapper())
//        );
//
//    }
//
//    /**
//     * 数据模拟，每个维度三到五个数据。数据集类型四个，in out  size byte 每秒一条数据，一个小时，3600条。 100
//     */
//    public void mockData() {
//        for (long i = 0; i < 3; i++) {
//
//            DataModelBuilder builder = DataModel.builder().organizationId(i);
//
//        }
//    }
//
//    public void mockOrganization(String reportType) {
//        this.builders = new ArrayList<>();
//        for (long i = 0; i < 3; i++) {
//
//        }
//    }
//
//    public void mockCluster(DataModelBuilder builder) {
//        for (long i = 0; i < 3; i++) {
//
//        }
//    }
//
//    @Test
//    public void test() {
//
//        DataIdModel dataIdModel = new DataIdModel();
//        dataIdModel.setCount(3);
//        List<DataModel> list = dataIdModel.getRuntimeDataModel();
//        DataValueModel dataValueModel = new DataValueModel();
//        Collection<Object> valueList = dataValueModel.value(list).values();
//
//        BulkRequest.Builder bulkRequestBuilder = new BulkRequest.Builder();
//        try {
//            valueList.forEach(v -> {
//                bulkRequestBuilder.operations((op) -> {
//                    return op
//                        .index(idx -> idx
//                            .index("test___1111")
//                            .id("p1")
//                            .document(v)
//                        );
//                });
//            });
//            this.client.bulk(bulkRequestBuilder.build());
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//
///*        try {
//            valueList.forEach(v -> {
//                bulkRequestBuilder.operations((op) -> {
//                    return op
//                        .index(idx -> idx
//                            .index("test___1111")
//                            .id("p1")
//                            .document(v)
//                        );
//                });
//            });
//            this.esClient.bulk(bulkRequestBuilder.build());
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }*/
//
//    }

}
