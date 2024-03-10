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
drop table if exists client;
create table client
(
    id          bigint auto_increment comment 'id'
        primary key,
    cluster_id  bigint           default -1                not null comment '集群ID',
    name        varchar(192)     default ''                not null comment '客户端名称',
    platform    varchar(192)     default ''                not null comment '客户端平台',
    language    varchar(192)     default ''                not null comment '客户端语言',
    pid         bigint           default -1                not null comment '客户端进程ID',
    host        varchar(128)     default ''                not null comment '客户端地址',
    port        int              default -1                not null comment '客户端端口',
    protocol    varchar(192)     default ''                not null comment '协议类型',
    status      tinyint unsigned default '0'               not null comment '状态: 1启用，0未启用',
    config_ids  varchar(1024)    default ''                not null comment 'csv config id list, like:1,3,7',
    description varchar(1024)    default ''                not null comment '客户端描述',
    create_time timestamp        default CURRENT_TIMESTAMP not null comment '创建时间',
    end_time    timestamp        default CURRENT_TIMESTAMP not null comment '结束时间',
    update_time timestamp        default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '修改时间'
)
    comment 'client is an SDK application that can produce or consume events.';

create index idx_cluster_id
    on client (cluster_id);

drop table if exists cluster;
create table cluster
(
    id                 bigint unsigned auto_increment comment '集群id'
        primary key,
    name               varchar(128)  default ''                not null comment '集群名称',
    register_name_list varchar(4096) default ''                not null comment '注册中心名字',
    bootstrap_servers  varchar(2048) default ''                not null comment 'server地址',
    eventmesh_version  varchar(32)   default ''                not null comment 'eventmesh版本',
    client_properties  text                                    null comment 'EventMesh客户端配置',
    jmx_properties     text                                    null comment 'JMX配置',
    reg_properties     text                                    null comment '注册中心配置',
    description        text                                    null comment '备注',
    auth_type          int           default 0                 not null comment '认证类型，-1未知，0:无认证，',
    run_state          tinyint       default 1                 not null comment '运行状态, 0表示未监控, 1监控中，有注册中心，2:监控中，无注册中心',
    create_time        timestamp     default CURRENT_TIMESTAMP not null comment '接入时间',
    update_time        timestamp     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '修改时间',
    status             int           default 1                 not null comment '0',
    store_type         int           default 0                 not null,
    constraint uniq_name
        unique (name)
)
    comment '物理集群信息表';

create index idx_uniq_name
    on cluster (name);

drop table if exists config;
create table config
(
    id                bigint unsigned auto_increment
        primary key,
    cluster_id        bigint        default -1                not null comment '集群ID',
    business_type     varchar(64)   default ''                not null comment '业务类型',
    instance_type     tinyint                                 not null comment '配置类型 0:runtime,1:storage,2:connector,3:topic',
    instance_id       bigint        default -1                not null comment '实例ID，上面配置对应的(比如runtime)的id',
    config_name       varchar(192)  default ''                not null comment '配置名称',
    config_value      text                                    null comment '配置值',
    start_version     varchar(64)   default ''                not null comment '配置开始使用的版本',
    status            int           default 1                 not null comment '0 关闭 1 开启 ',
    is_default        int           default 1                 null,
    end_version       varchar(64)   default ''                not null comment '配置结束使用的版本',
    diff_type         int           default -1                not null comment '差异类型',
    description       varchar(1000) default ''                not null comment '备注',
    edit              int           default 1                 not null comment '是否可以编辑 1 不可编辑（程序获取） 2 可编辑',
    create_time       timestamp     default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time       timestamp     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '修改时间',
    is_modify         int           default 0                 not null,
    eventmesh_version varchar(64)   default ' '               not null,
    constraint uniq_instance_type_instance_id_config_name
        unique (instance_id, config_name, instance_type)
)
    comment '配置信息表';

create index idx_phy_id_instance_id
    on config (cluster_id, instance_id);

drop table if exists connection;
create table connection
(
    id          bigint auto_increment comment 'id'
        primary key,
    cluster_id  bigint           default -1                not null comment '集群ID',
    source_type varchar(64)      default ''                not null comment 'source类型,可以为client或source connector',
    source_id   bigint           default -1                not null comment 'client或source connector ID',
    sink_type   varchar(64)      default ''                not null comment 'sink类型,可以为client或sink connector',
    sink_id     bigint           default -1                not null comment 'client或sink connector ID',
    runtime_id  bigint           default -1                not null comment '对应runtime id',
    status      tinyint unsigned default '0'               not null comment '状态: 1启用，0未启用',
    topic       varchar(192)     default ''                not null comment 'topic name',
    group_id    bigint           default -1                not null comment 'GroupID',
    description varchar(1024)    default ''                not null comment '客户端描述',
    create_time timestamp        default CURRENT_TIMESTAMP not null comment '创建时间',
    end_time    timestamp        default CURRENT_TIMESTAMP not null comment '结束时间',
    update_time timestamp        default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '修改时间'
)
    comment 'connection from event source to event sink. event source can be a source connector or a producer client.';

create index idx_cluster_id
    on connection (cluster_id);

create index idx_group_id
    on connection (group_id);

create index idx_sink_id
    on connection (sink_id);

create index idx_source_id
    on connection (source_id);

create index idx_topic
    on connection (topic);

drop table if exists connector;
create table connector
(
    id          bigint unsigned auto_increment comment 'id'
        primary key,
    cluster_id  bigint           default -1                not null comment '集群ID',
    name        varchar(512)     default ''                not null comment 'Connector名称',
    class_name  varchar(512)     default ''                not null comment 'Connector类',
    type        varchar(32)      default ''                not null comment 'Connector类型',
    status      tinyint unsigned default '0'               not null comment '状态: 1启用，0未启用',
    pod_state   tinyint unsigned default '0'               not null comment 'k8s pod状态。0: pending;1: running;2: success;3: failed;4: unknown',
    config_ids  varchar(1024)    default ''                not null comment 'csv config id list, like:1,3,7',
    create_time timestamp        default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time timestamp        default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '修改时间'
)
    comment 'Connector信息表';

create index idx_cluster_id
    on connector (cluster_id);

drop table if exists `group`;
create table `group`
(
    id           bigint unsigned auto_increment comment 'id'
        primary key,
    cluster_id   bigint                        default -1                not null comment '集群id',
    name         varchar(192) collate utf8_bin default ''                not null comment 'Group名称',
    member_count int unsigned                  default '0'               not null comment '成员数',
    members      text                                                    null comment 'group的member列表',
    type         tinyint                                                 not null comment 'group类型 0：consumer 1：producer',
    state        varchar(64)                   default ''                not null comment '状态',
    create_time  timestamp                     default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time  timestamp                     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '修改时间',
    status       int                           default 1                 not null,
    constraint uniq_cluster_phy_id_name
        unique (cluster_id, name)
)
    comment 'Group信息表';

create index cluster_id
    on `group` (cluster_id, name);


drop table if exists group_member;
create table group_member
(
    id             bigint unsigned auto_increment comment 'id'
        primary key,
    cluster_id     bigint       default -1                not null comment '集群ID',
    topic_name     varchar(192) default ''                not null comment 'Topic名称',
    group_name     varchar(192) default ''                not null comment 'Group名称',
    eventmesh_user varchar(192) default ''                not null comment 'EventMesh用户',
    state          varchar(64)  default ''                not null comment '状态',
    create_time    timestamp    default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time    timestamp    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '修改时间',
    status         int          default 1                 not null,
    constraint uniq_cluster_topic_group
        unique (cluster_id, topic_name, group_name)
)
    comment 'GroupMember信息表';

create index cluster_id
    on group_member (cluster_id, topic_name, group_name);

drop table if exists health_check_result;
create table health_check_result
(
    id          bigint unsigned auto_increment comment '自增id'
        primary key,
    type        tinyint       default 0                 not null comment '检查维度(0:未知, 1:Cluster, 2:Runtime, 3:Topic, 4:Storage)',
    type_id     bigint unsigned                         not null comment '对应检查维度的实例id',
    cluster_id  bigint        default 0                 not null comment '集群ID',
    state       tinyint       default 0                 not null comment '检查状态(0:未通过，1:通过,2:正在检查,3:超时)',
    result_desc varchar(1024) default ''                not null comment '检查结果描述',
    create_time timestamp     default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time timestamp     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '健康检查结果';

create index idx_cluster_id
    on health_check_result (cluster_id);

create index idx_type
    on health_check_result (type);


drop table if exists meta;
create table meta
(
    id          bigint unsigned auto_increment comment 'id'
        primary key,
    name        varchar(192)     default ''                not null comment '注册中心名称',
    type        varchar(192)     default ''                not null comment '注册中心类型,nacos,etcd,zookeeper',
    version     varchar(128)     default ''                not null comment '注册中心版本',
    cluster_id  bigint           default -1                not null comment '集群ID',
    host        varchar(128)     default ''                not null comment '注册中心地址',
    port        int              default -1                not null comment '注册中心端口',
    role        varchar(16)      default '-1'              not null comment '角色, leader follower observer',
    username    varchar(192)     default ''                not null comment '注册中心用户名',
    params      varchar(192)     default ''                not null comment '注册中心启动参数',
    status      tinyint unsigned default '0'               not null comment '状态: 1启用，0未启用',
    create_time timestamp        default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time timestamp        default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '修改时间'
)
    comment '注册中心信息表';

create index idx_cluster_id
    on meta (cluster_id);

drop table if exists operation_log;
create table operation_log
(
    id             bigint unsigned auto_increment comment 'id'
        primary key,
    cluster_id     bigint       default -1                not null comment '物理集群ID',
    operation_type varchar(192) default ''                not null comment '操作类型,如:启动，停止，重启，添加，删除，修改',
    state          int          default 0                 not null comment '操作状态 0:未知，1:执行中，2:成功，3:失败',
    content        text                                   null comment '备注信息',
    create_time    timestamp    default CURRENT_TIMESTAMP not null comment '创建时间',
    end_time       timestamp    default CURRENT_TIMESTAMP null comment '结束时间',
    operation_user varchar(192)                           null,
    result         text                                   null,
    target_type    varchar(192)                           not null,
    is_delete      int          default 0                 not null
)
    comment '操作记录信息表';

create index idx_cluster_phy_id
    on operation_log (cluster_id);

create index idx_status
    on operation_log (state);

drop table if exists runtime;
create table runtime
(
    id                 bigint auto_increment comment 'id'
        primary key,
    cluster_id         bigint        default -1                not null comment '物理集群ID',
    host               varchar(128)  default ''                not null comment 'runtime主机名',
    storage_cluster_id bigint        default -1                not null comment 'storageId',
    port               int           default -1                not null comment 'runtime端口',
    jmx_port           int           default -1                not null comment 'Jmx端口',
    start_timestamp    bigint        default -1                not null comment '启动时间',
    rack               varchar(128)  default ''                not null comment 'Rack信息',
    status             int           default 1                 not null comment '状态: 1启用，0未启用',
    create_time        timestamp     default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time        timestamp     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '修改时间',
    endpoint_map       varchar(1024) default ''                not null comment '监听信息',
    constraint uniq_cluster_phy_id__host_port
        unique (cluster_id, host)
)
    comment 'Runtime信息表';

create index idx_phy_id_host_storage_id
    on runtime (cluster_id, storage_cluster_id);


drop table if exists store;
create table store
(
    id              bigint unsigned auto_increment comment 'id'
        primary key,
    cluster_id      bigint        default -1                not null comment '物理集群ID',
    store_id        int           default -1                not null comment 'storeId',
    store_type      varchar(32)   default ''                not null comment 'Store类型,如rocketmq,redis,...',
    host            varchar(128)  default ''                not null comment 'store主机名',
    runtime_id      bigint        default -1                not null comment 'runtimeId',
    topic_list      varchar(4096) default ''                not null comment 'topicName列表',
    diff_type       int           default -1                not null comment '差异类型',
    port            int           default -1                not null comment 'store端口',
    jmx_port        int           default -1                not null comment 'Jmx端口',
    start_timestamp bigint        default -1                not null comment '启动时间',
    rack            varchar(128)  default ''                not null comment 'Rack信息',
    status          int           default 1                 not null comment '状态: 1启用，0未启用',
    create_time     timestamp     default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time     timestamp     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '修改时间',
    endpoint_map    varchar(1024) default ''                not null comment '监听信息',
    constraint uniq_cluster_phy_id__storage_id
        unique (cluster_id, store_id)
)
    comment 'Store信息表';

create index idx_store_id_runtime_id
    on store (store_id, cluster_id, runtime_id);


drop table if exists topic;
create table topic
(
    id           bigint unsigned auto_increment comment 'id'
        primary key,
    cluster_id   bigint                        default -1                not null comment '集群ID',
    topic_name   varchar(192) collate utf8_bin default ''                not null comment 'Topic名称',
    runtime_id   varchar(2048)                 default ''                not null comment 'RuntimeId',
    storage_id   varchar(2048)                 default ''                not null comment 'StorageId',
    retention_ms bigint                        default -2                not null comment '保存时间，-2：未知，-1：无限制，>=0对应时间，单位ms',
    type         tinyint                       default 0                 not null comment 'Topic类型，默认0，0:普通，1:EventMesh内部',
    description  text                                                    null comment '备注信息',
    create_time  timestamp                     default CURRENT_TIMESTAMP not null comment '创建时间(尽量与Topic实际创建时间一致)',
    update_time  timestamp                     default CURRENT_TIMESTAMP not null comment '修改时间(尽量与Topic实际创建时间一致)',
    status       int                           default 1                 not null,
    constraint uniq_cluster_phy_id_topic_name
        unique (cluster_id, topic_name)
)
    comment 'Topic信息表';

create index cluster_id
    on topic (cluster_id, topic_name);

