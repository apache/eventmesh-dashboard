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
DROP TABLE IF EXISTS `cluster`;
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


DROP TABLE IF EXISTS `config`;
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


DROP TABLE IF EXISTS `store`;
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



DROP TABLE IF EXISTS `group`;
CREATE TABLE `group`
(
    `id`           bigint unsigned                                        NOT NULL AUTO_INCREMENT COMMENT 'id',
    `cluster_id`   bigint                                                 NOT NULL DEFAULT '-1' COMMENT '集群id',
    `name`         varchar(192) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT '' COMMENT 'Group名称',
    `member_count` int unsigned                                           NOT NULL DEFAULT '0' COMMENT '成员数',
    `members`      varchar(1024) COMMENT 'group的member列表',
    `type`         tinyint                                                NOT NULL COMMENT 'group类型 0：consumer 1：producer',
    `state`        varchar(64)                                            NOT NULL DEFAULT '' COMMENT '状态',
    `create_time`  timestamp                                              NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  timestamp                                              NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `status`    int                                                    NOT NULL DEFAULT '1',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uniq_cluster_phy_id_name` (`cluster_id`, `name`),
    KEY `cluster_id` (`cluster_id`, `name`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 322
  DEFAULT CHARSET = utf8mb4,
  DEFAULT COLLATE = utf8mb4_bin COMMENT ='Group信息表';


DROP TABLE IF EXISTS `group_member`;
CREATE TABLE `group_member`
(
    `id`             bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
    `cluster_id`     bigint          NOT NULL DEFAULT '-1' COMMENT '集群ID',
    `topic_name`     varchar(192)    NOT NULL DEFAULT '' COMMENT 'Topic名称',
    `group_name`     varchar(192)    NOT NULL DEFAULT '' COMMENT 'Group名称',
    `eventmesh_user` varchar(192)    NOT NULL DEFAULT '' COMMENT 'EventMesh用户',
    `state`          varchar(64)     NOT NULL DEFAULT '' COMMENT '状态',
    `create_time`    timestamp       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    timestamp       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `status`      int             NOT NULL DEFAULT '1',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uniq_cluster_topic_group` (`cluster_id`, `topic_name`, `group_name`),
    KEY `cluster_id` (`cluster_id`, `topic_name`, `group_name`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 257
  DEFAULT CHARSET = utf8mb4,
  DEFAULT COLLATE = utf8mb4_bin COMMENT ='GroupMember信息表';


DROP TABLE IF EXISTS `operation_log`;
CREATE TABLE `operation_log`
(
    `id`             bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
    `cluster_id`     bigint          NOT NULL DEFAULT '-1' COMMENT '物理集群ID',
    `operation_type` varchar(192)    NOT NULL DEFAULT '' COMMENT '操作类型,如:启动，停止，重启，添加，删除，修改',
    `state`         int             NOT NULL DEFAULT '0' COMMENT '操作状态 0:未知，1:执行中，2:成功，3:失败',
    `content`        varchar(1024) COMMENT '备注信息',
    `create_time`    timestamp       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `end_time`       timestamp       NULL     DEFAULT CURRENT_TIMESTAMP COMMENT '结束时间',
    `operation_user` varchar(192)             DEFAULT NULL,
    `result`         varchar(1024),
    `target_type`    varchar(192)    NOT NULL,
    `is_delete`      int             NOT NULL DEFAULT '0',
    PRIMARY KEY (`id`),
    KEY `idx_cluster_phy_id` (`cluster_id`),
    KEY `idx_state` (`state`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 68
  DEFAULT CHARSET = utf8mb4,
  DEFAULT COLLATE = utf8mb4_bin COMMENT ='操作记录信息表';


DROP TABLE IF EXISTS `topic`;
CREATE TABLE `topic`
(
    `id`           bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
    `cluster_id`   bigint          NOT NULL DEFAULT '-1' COMMENT '集群ID',
    `topic_name`   varchar(192) CHARACTER SET utf8mb4
        COLLATE utf8mb4_bin        NOT NULL DEFAULT '' COMMENT 'Topic名称',
    `runtime_id`   varchar(2048)   NOT NULL DEFAULT '' COMMENT 'RuntimeId',
    `storage_id`   varchar(2048)   NOT NULL DEFAULT '' COMMENT 'StorageId',
    `retention_ms` bigint          NOT NULL DEFAULT '-2' COMMENT '保存时间，-2：未知，-1：无限制，>=0对应时间，单位ms',
    `type`         tinyint         NOT NULL DEFAULT '0' COMMENT 'Topic类型，默认0，0:普通，1:EventMesh内部',
    `description`  varchar(1024)            DEFAULT '' COMMENT '备注信息',
    `create_time`  timestamp       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间(尽量与Topic实际创建时间一致)',
    `update_time`  timestamp       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间(尽量与Topic实际创建时间一致)',
    `status`    int             NOT NULL DEFAULT '1',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uniq_cluster_phy_id_topic_name` (`cluster_id`, `topic_name`),
    KEY `cluster_id` (`cluster_id`, `topic_name`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 562
  DEFAULT CHARSET = utf8mb4,
  DEFAULT COLLATE = utf8mb4_bin COMMENT ='Topic信息表';


DROP TABLE IF EXISTS `client`;
CREATE TABLE `client`
(
    `id`          bigint(20)          NOT NULL AUTO_INCREMENT COMMENT 'id',
    `cluster_id`  bigint(20)          NOT NULL DEFAULT '-1' COMMENT '集群ID',
    `name`        varchar(192)        NOT NULL DEFAULT '' COMMENT '客户端名称',
    `platform`    varchar(192)        NOT NULL DEFAULT '' COMMENT '客户端平台',
    `language`    varchar(192)        NOT NULL DEFAULT '' COMMENT '客户端语言',
    `pid`         bigint(22)          NOT NULL DEFAULT '-1' COMMENT '客户端进程ID',
    `host`        varchar(128)        NOT NULL DEFAULT '' COMMENT '客户端地址',
    `port`        int(16)             NOT NULL DEFAULT '-1' COMMENT '客户端端口',
    `protocol`    varchar(192)        NOT NULL DEFAULT '' COMMENT '协议类型',
    `status`      tinyint(4) unsigned NOT NULL DEFAULT '0' COMMENT '状态: 1启用，0未启用',
    `config_ids`  varchar(1024)       NOT NULL DEFAULT '' COMMENT 'csv config id list, like:1,3,7',
    `description` varchar(1024)       NOT NULL DEFAULT '' COMMENT '客户端描述',
    `create_time` timestamp           NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `end_time`    timestamp           NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '结束时间',
    `update_time` timestamp           NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',

    PRIMARY KEY (`id`),
    INDEX `idx_cluster_id` (`cluster_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4,
  DEFAULT COLLATE = utf8mb4_bin COMMENT ='client is an SDK application that can produce or consume events.';



DROP TABLE IF EXISTS `connector`;
CREATE TABLE `connector`
(
    `id`          bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
    `cluster_id`  bigint(20)          NOT NULL DEFAULT '-1' COMMENT '集群ID',
    `name`        varchar(512)        NOT NULL DEFAULT '' COMMENT 'Connector名称',
    `class_name`  varchar(512)        NOT NULL DEFAULT '' COMMENT 'Connector类',
    `type`        varchar(32)         NOT NULL DEFAULT '' COMMENT 'Connector类型',
    `status`      tinyint(4) unsigned NOT NULL DEFAULT '0' COMMENT '状态: 1启用，0未启用',
    `pod_state`   tinyint(4) unsigned NOT NULL DEFAULT '0' COMMENT 'k8s pod状态。0: pending;1: running;2: success;3: failed;4: unknown',
    `config_ids`  varchar(1024)       NOT NULL DEFAULT '' COMMENT 'csv config id list, like:1,3,7',
    `create_time` timestamp           NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` timestamp           NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    INDEX `idx_cluster_id` (`cluster_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4,
  DEFAULT COLLATE = utf8mb4_bin COMMENT ='Connector信息表';

DROP TABLE IF EXISTS `connection`;
CREATE TABLE `connection`
(
    `id`          bigint(20)          NOT NULL AUTO_INCREMENT COMMENT 'id',
    `cluster_id`  bigint(20)          NOT NULL DEFAULT '-1' COMMENT '集群ID',
    `source_type` varchar(64)         NOT NULL DEFAULT '' COMMENT 'source类型,可以为client或source connector',
    `source_id`   bigint(20)          NOT NULL DEFAULT '-1' COMMENT 'client或source connector ID',
    `sink_type`   varchar(64)         NOT NULL DEFAULT '' COMMENT 'sink类型,可以为client或sink connector',
    `sink_id`     bigint(20)          NOT NULL DEFAULT '-1' COMMENT 'client或sink connector ID',
    `runtime_id`  bigint(20)          NOT NULL DEFAULT '-1' COMMENT '对应runtime id',
    `status`      tinyint(4) unsigned NOT NULL DEFAULT '0' COMMENT '状态: 1启用，0未启用',
    `topic`       varchar(192)        NOT NULL DEFAULT '' COMMENT 'topic name',
    `group_id`    bigint(20)          NOT NULL DEFAULT '-1' COMMENT 'GroupID',
    `description` varchar(1024)       NOT NULL DEFAULT '' COMMENT '客户端描述',
    `create_time` timestamp           NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `end_time`    timestamp           NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '结束时间',
    `update_time` timestamp           NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',

    PRIMARY KEY (`id`),
    INDEX `idx_cluster_id` (`cluster_id`),
    INDEX `idx_group_id` (`group_id`),
    INDEX `idx_topic` (`topic`),
    INDEX `idx_source_id` (`source_id`),
    INDEX `idx_sink_id` (`sink_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4,
  DEFAULT COLLATE = utf8mb4_bin COMMENT ='connection from event source to event sink. event source can be a source connector or a producer client.';

DROP TABLE IF EXISTS `health_check_result`;
CREATE TABLE `health_check_result`
(
    `id`          bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增id',
    `type`        tinyint(4)          NOT NULL DEFAULT '0' COMMENT '检查维度(0:未知, 1:Cluster, 2:Runtime, 3:Topic, 4:Storage)',
    `type_id`     bigint(20) unsigned NOT NULL COMMENT '对应检查维度的实例id',
    `cluster_id`  bigint(20)          NOT NULL DEFAULT '0' COMMENT '集群ID',
    `state`       tinyint(4)          NOT NULL DEFAULT '0' COMMENT '检查状态(0:未通过，1:通过,2:正在检查,3:超时)',
    `result_desc` varchar(1024)       NOT NULL DEFAULT '' COMMENT '检查结果描述',
    `create_time` timestamp           NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` timestamp           NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_cluster_id` (`cluster_id`),
    INDEX `idx_type` (`type`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4,
  DEFAULT COLLATE = utf8mb4_bin COMMENT ='健康检查结果';

DROP TABLE IF EXISTS `meta`;
CREATE TABLE `meta`
(
    `id`          bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
    `name`        varchar(192)        NOT NULL DEFAULT '' COMMENT '注册中心名称',
    `type`        varchar(192)        NOT NULL DEFAULT '' COMMENT '注册中心类型,nacos,etcd,zookeeper',
    `version`     varchar(128)        NOT NULL DEFAULT '' COMMENT '注册中心版本',
    `cluster_id`  bigint(20)          NOT NULL DEFAULT '-1' COMMENT '集群ID',
    `host`        varchar(128)        NOT NULL DEFAULT '' COMMENT '注册中心地址',
    `port`        int(16)             NOT NULL DEFAULT '-1' COMMENT '注册中心端口',
    `role`        varchar(16)         NOT NULL DEFAULT '-1' COMMENT '角色, leader follower observer',
    `username`    varchar(192)        NOT NULL DEFAULT '' COMMENT '注册中心用户名',
    `params`      varchar(192)        NOT NULL DEFAULT '' COMMENT '注册中心启动参数',
    `status`      tinyint(4) unsigned NOT NULL DEFAULT '0' COMMENT '状态: 1启用，0未启用',

    `create_time` timestamp           NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` timestamp           NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',

    PRIMARY KEY (`id`),
    INDEX `idx_cluster_id` (`cluster_id`)

) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4,
  DEFAULT COLLATE = utf8mb4_bin COMMENT ='注册中心信息表';