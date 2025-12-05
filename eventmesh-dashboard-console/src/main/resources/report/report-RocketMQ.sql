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

create
database if not exists eventmesh_dashboard with(TTL=2592000000);

create table rocketmq_messages_in_total
(
    time              timestamp time,
    organization_id   string tag,
    organization_name string attribute,
    clusters_id       string tag,
    cluster_name      string attribute,
    runtime_type      string attribute,
    runtime_id        string tag,
    runtime_name      string attribute,
    topic_id          string tag,
    topic_name        string attribute,
    message_type      string attribute,
    value             int64 field
)comment '消息in总数统计表'  with(ttl=default);

create table rocketmq_messages_out_total
(
    time              timestamp time,
    organization_id   string tag,
    organization_name string attribute,
    clusters_id       string tag,
    cluster_name      string attribute,
    runtime_type      string attribute,
    runtime_id        string tag,
    runtime_name      string attribute,
    topic_id          string tag,
    topic_name        string attribute,
    message_type      string attribute,
    value             int64 field
)comment '消息out总数统计表'  with(ttl=default);

create table rocketmq_throughput_in_total
(
    time              timestamp time,
    organization_id   string tag,
    organization_name string attribute,
    clusters_id       string tag,
    cluster_name      string attribute,
    runtime_type      string attribute,
    runtime_id        string tag,
    runtime_name      string attribute,
    topic_id          string tag,
    topic_name        string attribute,
    message_type      string attribute,
    value             int64 field
)comment '消息体 in 总数统计表'  with(ttl=default);

create table rocketmq_throughput_out_total
(
    time              timestamp time,
    organization_id   string tag,
    organization_name string attribute,
    clusters_id       string tag,
    cluster_name      string attribute,
    runtime_type      string attribute,
    runtime_id        string tag,
    runtime_name      string attribute,
    topic_id          string tag,
    topic_name        string attribute,
    message_type      string attribute,
    value             int64 field
)comment '消息体 out 总数统计表'  with(ttl=default);


create table rocketmq_consumer_ready_messages
(
    time              timestamp time,
    organization_id   string tag,
    organization_name string attribute,
    clusters_id       string tag,
    cluster_name      string attribute,
    runtime_type      string attribute,
    runtime_id        string tag,
    runtime_name      string attribute,
    topic_id          string tag,
    topic_name        string attribute,
    group_id          string tag,
    group_name        string attribute,
    value             int64 field
)comment '已就绪消息量'  with(ttl=default);

create table rocketmq_consumer_inflight_messages
(
    time              timestamp time,
    organization_id   string tag,
    organization_name string attribute,
    clusters_id       string tag,
    cluster_name      string attribute,
    runtime_type      string attribute,
    runtime_id        string tag,
    runtime_name      string attribute,
    topic_id          string tag,
    topic_name        string attribute,
    group_id          string tag,
    group_name        string attribute,
    value             int64 field
)comment '处理中消息量'  with(ttl=default);

create table rocketmq_consumer_queueing_latency
(
    time              timestamp time,
    organization_id   string tag,
    organization_name string attribute,
    clusters_id       string tag,
    cluster_name      string attribute,
    runtime_type      string attribute,
    runtime_id        string tag,
    runtime_name      string attribute,
    topic_id          string tag,
    topic_name        string attribute,
    group_id          string tag,
    group_name        string attribute,
    value             int64 field
)comment '已就绪消息排队延迟时间'  with(ttl=default);

create table rocketmq_consumer_lag_latency
(
    time              timestamp time,
    organization_id   string tag,
    organization_name string attribute,
    clusters_id       string tag,
    cluster_name      string attribute,
    runtime_type      string attribute,
    runtime_id        string tag,
    runtime_name      string attribute,
    topic_id          string tag,
    topic_name        string attribute,
    group_id          string tag,
    group_name        string attribute,
    value             int64 field
)comment '消费处理延迟时间'  with(ttl=default);

create table rocketmq_send_to_dlq_messages_total
(
    time              timestamp time,
    organization_id   string tag,
    organization_name string attribute,
    clusters_id       string tag,
    cluster_name      string attribute,
    runtime_type      string attribute,
    runtime_id        string tag,
    runtime_name      string attribute,
    topic_id          string tag,
    topic_name        string attribute,
    group_id          string tag,
    group_name        string attribute,
    value             int64 field
)comment '转为死信状态的消息量'  with(ttl=default);

create table rocketmq_storage_message_reserve_time
(
    time              timestamp time,
    organization_id   string tag,
    organization_name string attribute,
    clusters_id       string tag,
    cluster_name      string attribute,
    runtime_type      string attribute,
    runtime_id        string tag,
    runtime_name      string attribute,
    value             int64 field
)comment '储存层消息保存时间'  with(ttl=default);

create table rocketmq_storage_dispatch_behind_bytes
(
    time              timestamp time,
    organization_id   string tag,
    organization_name string attribute,
    clusters_id       string tag,
    cluster_name      string attribute,
    runtime_type      string attribute,
    runtime_id        string tag,
    runtime_name      string attribute,
    value             int64 field
)comment 'dispatch 落后大小'  with(ttl=default);

create table rocketmq_storage_flush_behind_bytes
(
    time              timestamp time,
    organization_id   string tag,
    organization_name string attribute,
    clusters_id       string tag,
    cluster_name      string attribute,
    runtime_type      string attribute,
    runtime_id        string tag,
    runtime_name      string attribute,
    value             int64 field
)comment '刷盘落后大小'  with(ttl=default);

create table rocketmq_thread_pool_wartermark
(
    time              timestamp time,
    organization_id   string tag,
    organization_name string attribute,
    clusters_id       string tag,
    cluster_name      string attribute,
    runtime_type      string attribute,
    runtime_id        string tag,
    runtime_name      string attribute,
    name              string attribute,
    value             int64 field
)comment '线程池排队数'  with(ttl=default);

create table rocketmq_topic_number
(
    time              timestamp time,
    organization_id   string tag,
    organization_name string attribute,
    clusters_id       string tag,
    cluster_name      string attribute,
    runtime_type      string attribute,
    runtime_id        string tag,
    runtime_name      string attribute,
    value             int64 field
)comment '主题数量'  with(ttl=default);


create table rocketmq_consumer_group_number
(
    time              timestamp time,
    organization_id   string tag,
    organization_name string attribute,
    clusters_id       string tag,
    cluster_name      string attribute,
    runtime_type      string attribute,
    runtime_id        string tag,
    runtime_name      string attribute,
    value             int64 field
)comment '消费者组数量'  with(ttl=default);

create table rocketmq_message_size
(
    time              timestamp time,
    organization_id   string tag,
    organization_name string attribute,
    clusters_id       string tag,
    cluster_name      string attribute,
    runtime_type      string attribute,
    runtime_id        string tag,
    runtime_name      string attribute,
    topic_id          string tag,
    topic_name        string attribute,
    message_type      string attribute,
    value_le_1_kb     int64 field,
    value_le_4_kb     int64 field,
    value_le_512_kb   int64 field,
    value_le_1_mb     int64 field,
    value_le_2_mb     int64 field,
    value_le_4_mb     int64 field,
    value_le_overflow int64 field
)comment '消息大小的分布情况，发送成功时统计'  with(ttl=default);

create table rocketmq_rpc_latency
(
    time                timestamp time,
    organization_id     string tag,
    organization_name   string attribute,
    clusters_id         string tag,
    cluster_name        string attribute,
    runtime_type        string attribute,
    runtime_id          string tag,
    runtime_name        string attribute,
    protocol_type       string attribute,
    request_code        string attribute,
    response_code       string attribute,
    value_le_1_ms       int32 field,
    value_le_3_ms       int32 field,
    value_le_5_ms       int32 field,
    value_le_10_ms      int32 field,
    value_le_100_ms     int32 field,
    value_le_1_s        int32 field,
    value_le_3_s        int32 field,
    value_le_overflow_s int32 field

)comment '调用耗时'  with(ttl=default);

create table rocketmq_topic_create_execution_time
(
    time                timestamp time,
    organization_id     string tag,
    organization_name   string attribute,
    clusters_id         string tag,
    cluster_name        string attribute,
    runtime_type        string attribute,
    runtime_id          string tag,
    runtime_name        string attribute,
    value_le_10_ms      int32 field,
    value_le_100_ms     int32 field,
    value_le_1_s        int32 field,
    value_le_3_s        int32 field,
    value_le_5_s        int32 field,
    value_le_overflow_s int32 field
)comment '创建主题执行耗时'  with(ttl=default);

create table rocketmq_consumer_group_create_execution_time
(
    time                timestamp time,
    organization_id     string tag,
    organization_name   string attribute,
    clusters_id         string tag,
    cluster_name        string attribute,
    runtime_type        string attribute,
    runtime_id          string tag,
    runtime_name        string attribute,
    value_le_10_ms      int32 field,
    value_le_100_ms     int32 field,
    value_le_1_s        int32 field,
    value_le_3_s        int32 field,
    value_le_5_s        int32 field,
    value_le_overflow_s int32 field
)comment '创建消费者组执行耗时'  with(ttl=default);
