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
    is_delete    int                           default 0                 not null,
    constraint uniq_cluster_phy_id_name
        unique (cluster_id, name)
)
    comment 'Group信息表' engine = InnoDB;

create index cluster_id
    on `group` (cluster_id, name);



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
    is_delete      int          default 0                 not null,
    constraint uniq_cluster_topic_group
        unique (cluster_id, topic_name, group_name)
)
    comment 'GroupMember信息表' engine = InnoDB;

create index cluster_id
    on group_member (cluster_id, topic_name, group_name);



create table operation_log
(
    id               bigint unsigned auto_increment comment 'id'
        primary key,
    cluster_id       bigint       default -1                not null comment '物理集群ID',
    operation_type   varchar(192) default ''                not null comment '操作类型,如:启动，停止，重启，添加，删除，修改',
    status           int          default 0                 not null comment '操作状态 0:未知，1:执行中，2:成功，3:失败',
    description      text                                   null comment '备注信息',
    create_time      timestamp    default CURRENT_TIMESTAMP not null comment '创建时间',
    end_time         timestamp    default CURRENT_TIMESTAMP null comment '结束时间',
    operation_user   varchar(192)                           null,
    result_content   text                                   null,
    operation_target varchar(192)                           not null,
    is_delete        int                                    not null
)
    comment '操作记录信息表' engine = InnoDB;

create index idx_cluster_phy_id
    on operation_log (cluster_id);

create index idx_status
    on operation_log (status);



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
    is_delete    int                           default 0                 not null,
    constraint uniq_cluster_phy_id_topic_name
        unique (cluster_id, topic_name)
)
    comment 'Topic信息表' engine = InnoDB;

create index cluster_id
    on topic (cluster_id, topic_name);



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
    `config_ids`  text                NOT NULL DEFAULT '' COMMENT 'csv config id list, like:1,3,7',
    `description` text                NOT NULL DEFAULT '' COMMENT '客户端描述',
    `create_time` timestamp           NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `end_time`    timestamp           NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '结束时间',
    `update_time` timestamp           NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',

    PRIMARY KEY (`id`),
    INDEX `idx_cluster_id` (`cluster_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8 COMMENT ='客户端信息表';



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
    `config_ids`  text                NOT NULL DEFAULT '' COMMENT 'csv config id list, like:1,3,7',
    `create_time` timestamp           NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` timestamp           NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    INDEX `idx_cluster_id` (`cluster_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8 COMMENT ='Connector信息表';

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
    `description` text                NOT NULL DEFAULT '' COMMENT '客户端描述',
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
  DEFAULT CHARSET = utf8 COMMENT ='client和connector连接关系，这里的client包括runtime';

DROP TABLE IF EXISTS `health_check_result`;
CREATE TABLE `health_check_result`
(
    `id`          bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增id',
    `dimension`   int(11)             NOT NULL DEFAULT '0' COMMENT '检查维度(0:未知，1:Cluster，2:Runtime，3:Topic，4:Group)',
    `config_name` varchar(192)        NOT NULL DEFAULT '' COMMENT '配置名',
    `cluster_id`  bigint(20)          NOT NULL DEFAULT '0' COMMENT '集群ID',
    `res_name`    varchar(192)        NOT NULL DEFAULT '' COMMENT '资源名称',
    `passed`      tinyint(4)          NOT NULL DEFAULT '0' COMMENT '检查通过(0:未通过，1:通过)',
    `create_time` timestamp           NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` timestamp           NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_cluster_id` (`cluster_id`),
    UNIQUE KEY `uniq_dimension_config_cluster_res` (`dimension`, `config_name`, `cluster_id`, `res_name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8 COMMENT ='健康检查结果';

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
  DEFAULT CHARSET = utf8 COMMENT ='注册中心信息表';