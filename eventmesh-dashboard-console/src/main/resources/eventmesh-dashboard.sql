/*
 * licensed to the apache software foundation (asf) under one or more
 * contributor license agreements.  see the notice file distributed with
 * this work for additional information regarding copyright ownership.
 * the asf licenses this file to you under the apache license, version 2.0
 * (the "license"); you may not use this file except in compliance with
 * the license.  you may obtain a copy of the license at
 *
 *     http://www.apache.org/licenses/license-2.0
 *
 * unless required by applicable law or agreed to in writing, software
 * distributed under the license is distributed on an "as is" basis,
 * without warranties or conditions of any kind, either express or implied.
 * see the license for the specific language governing permissions and
 * limitations under the license.
 */
drop table if exists `cluster`;
create table cluster
(
    id               bigint unsigned primary key auto_increment comment '集群id',
    name             varchar(128) not null comment '集群名称',
    cluster_type     varchar(16)  not null comment '集群类型',
    trusteeship_type varchar(16)  not null comment '托管类型',
    version          varchar(32)  not null comment 'eventmesh版本',
    jmx_properties   text comment 'jmx配置',
    description      text comment '备注',
    auth_type        int          not null default 0 comment '认证类型，-1未知，0:无认证，',
    run_state        tinyint      not null default 1 comment '运行状态, 0表示未监控, 1监控中，有注册中心，2:监控中，无注册中心',
    status           int          not null default 1 comment '0',
    create_time      timestamp    not null default current_timestamp comment '接入时间',
    update_time      timestamp    not null default current_timestamp on update current_timestamp comment '修改时间',
    is_delete        int          not null default 0 comment '数据逻辑标记',
    unique key uniq_name (name)
) comment '物理集群信息表';


drop table if exists `cluster_relationship`;
create table `cluster_relationship`
(
    id                bigint unsigned primary key auto_increment,
    cluster_type      varchar(16) not null comment '集群类型',
    cluster_id        bigint      not null comment '集群id',
    relationship_type varchar(16) not null comment '集群类型',
    relationship_id   bigint      not null comment '集群id',
    create_time       timestamp   not null default current_timestamp comment '创建时间',
    update_time       timestamp   not null default current_timestamp on update current_timestamp comment '修改时间',
    status            int         not null default 1 comment '0',
    is_delete         int         not null default 0 comment '0',
    unique key cluster_id_relationship_id_unique (`cluster_id`, `relationship_id`),
    key cluster_id_key (`cluster_id`),
    key relationship_id_key (`relationship_id`)
);

drop table if exists `config`;
create table config
(
    id             bigint unsigned auto_increment primary key,
    cluster_id     bigint        not null comment '集群id',
    instance_type  tinyint       not null comment '实例类型 0:runtime,1:storage,2:connector,3:topic',
    instance_id    bigint        not null default -1 comment '实例id，上面配置对应的(比如runtime)的id，如果是-1，是cluster的配置',
    config_type    varchar(31)   not null default '' comment '配置类型',
    config_name    varchar(192)  not null comment '配置名称',
    config_value   text          not null comment '配置值',
    start_version  varchar(64)   not null default '' comment '配置开始使用的版本',
    end_version    varchar(64)   not null default '' comment '配置结束使用的版本',
    status         int           not null default 1 comment '0 关闭 1 开启 ',
    is_default     int           not null default 1,
    diff_type      int           not null default -1 comment '差异类型',
    description    varchar(1000) not null default '' comment '备注',
    edit           int           not null default 1 comment '是否可以编辑 1 不可编辑（程序获取） 2 可编辑',
    create_time    timestamp     not null default current_timestamp comment '创建时间',
    update_time    timestamp     not null default current_timestamp on update current_timestamp comment '修改时间',
    is_modify      int           not null default 0,
    already_update int           not null default 0 comment '0:no,1:yes',
    is_delete      int           not null default 0 comment '0',
    unique key uniq_cluster_id_instance_type_instance_id_config_name (instance_id, config_name, instance_type, cluster_id)
) comment '配置信息表';


drop table if exists `group`;
create table `group`
(
    id           bigint unsigned primary key auto_increment comment 'id',
    cluster_id   bigint       not null comment '集群id',
    name         varchar(192) not null comment 'group名称',
    member_count int unsigned not null default '0' comment '成员数',
    members      text         null comment 'group的member列表',
    type         tinyint      not null comment 'group类型 0：consumer 1：producer',
    state        varchar(64)  not null default '' comment '状态',
    create_time  timestamp    not null default current_timestamp comment '创建时间',
    update_time  timestamp    not null default current_timestamp on update current_timestamp comment '修改时间',
    status       int          not null default 1,
    is_delete    int          not null default 0 comment '0',
    unique key uniq_cluster_phy_id_name (cluster_id, name)
) comment 'group信息表';


drop table if exists group_member;
create table group_member
(
    id             bigint unsigned primary key auto_increment comment 'id',
    cluster_id     bigint       default -1                                            not null comment '集群id',
    topic_name     varchar(192) default ''                                            not null comment 'topic名称',
    group_name     varchar(192) default ''                                            not null comment 'group名称',
    eventmesh_user varchar(192) default ''                                            not null comment 'eventmesh用户',
    state          varchar(64)  default ''                                            not null comment '状态',
    create_time    timestamp    default current_timestamp                             not null comment '创建时间',
    update_time    timestamp    default current_timestamp on update current_timestamp not null comment '修改时间',
    status         int          default 1                                             not null,
    is_delete      int                                                                not null default 0 comment '0',
    unique key uniq_cluster_topic_group (cluster_id, topic_name, group_name)
) comment 'groupmember信息表';


drop table if exists runtime;
create table runtime
(
    id              bigint primary key auto_increment comment 'id',
    cluster_id      bigint        not null comment '物理集群id',
    `name`          varchar(128)  not null comment 'runtime名称',
    host            varchar(128)  not null comment 'runtime主机名',
    port            varchar(128)  not null comment 'runtime端口',
    jmx_port        int           not null default -1 comment 'jmx端口',
    start_timestamp timestamp     not null default current_timestamp comment '启动时间',
    rack            varchar(128)  not null default '' comment 'rack信息',
    status          int           not null default 1 comment '状态: 1启用，0未启用',
    create_time     timestamp     not null default current_timestamp comment '创建时间',
    update_time     timestamp     not null default current_timestamp on update current_timestamp comment '修改时间',
    endpoint_map    varchar(1024) not null default '' comment '监听信息',
    is_delete       int           not null default 0 comment '0',
    unique key uniq_cluster_phy_id__host_port (cluster_id, host)
) comment 'runtime信息表';

drop table if exists `client`;
create table `client`
(
    `id`          bigint primary key auto_increment comment 'id',
    `cluster_id`  bigint              not null default '-1' comment '集群id',
    `name`        varchar(192)        not null default '' comment '客户端名称',
    `platform`    varchar(192)        not null default '' comment '客户端平台',
    `language`    varchar(192)        not null default '' comment '客户端语言',
    `pid`         bigint(22)          not null default '-1' comment '客户端进程id',
    `host`        varchar(128)        not null default '' comment '客户端地址',
    `port`        int(16)             not null default -1 comment '客户端端口',
    `protocol`    varchar(192)        not null default '' comment '协议类型',
    `status`      tinyint(4) unsigned not null default 1 comment '状态: 1启用，0未启用',
    `config_ids`  varchar(1024)       not null default '' comment 'csv config id list, like:1,3,7',
    `description` varchar(1024)       not null default '' comment '客户端描述',
    `create_time` timestamp           not null default current_timestamp comment '创建时间',
    `end_time`    timestamp           not null default current_timestamp comment '结束时间',
    `update_time` timestamp           not null default current_timestamp on update current_timestamp comment '修改时间',
    index `idx_cluster_id` (`cluster_id`)
) comment ='client';

drop table if exists `net_connection`;
create table `net_connection`
(
    `id`              bigint unsigned primary key auto_increment comment 'id',
    `cluster_id`      bigint unsigned     not null default 0 comment '集群id',
    `client_id`       bigint unsigned     not null comment '',
    `client_host`     varchar(192)        not null comment 'client地址',
    `client_port`     int                 not null comment 'client 端口',
    `runtime_id`      bigint unsigned     not null default 0 comment '对应runtime id',
    `runtime_host`    varchar(192)        not null comment 'runtime地址',
    `runtime_port`    int                 not null comment 'runtime 端口',
    `status`          tinyint(4) unsigned not null default 1 comment '状态: 1 连接中， 2 断连',
    `description`     varchar(1024)       not null default '' comment '客户端描述',
    `connection_time` timestamp           not null comment '连接时间',
    `disconnect_time` timestamp           not null comment '断连时间',
    `create_time`     timestamp           not null default current_timestamp comment '创建时间',
    `update_time`     timestamp           not null default current_timestamp on update current_timestamp comment '修改时间',
    index `idx_cluster_id` (`cluster_id`),
    index `idx_client_id` (`client_id`),
    index `idx_runtime_id` (`runtime_id`)
) comment ='net_connection ';

drop table if exists `instance_user`;
create table `instance_user`
(
    `id`            bigint unsigned primary key auto_increment comment 'id',
    `instance_type` int(255)        not null default 0 comment '区分不同软件',
    `password`      varchar(100)    not null default '' comment '密码',
    `cluster_id`    bigint unsigned not null comment '物理集群id',
    `name`          varchar(192)    not null default '' comment '名称',
    `token`         varchar(8192)   not null default '' comment '密钥',
    `status`        int                      default 1 not null comment '状态: 1启用，0未启用',
    `create_time`   timestamp       not null default current_timestamp comment '创建时间',
    `update_time`   timestamp       not null default current_timestamp on update current_timestamp comment '修改时间',
    is_delete       int             not null default 0 comment '0',
    unique unique_cluster_name (cluster_id, name)
) engine = innodb
  default charset = utf8 comment ='instance_user信息表';



drop table if exists `acl`;
create table `acl`
(
    `id`              bigint unsigned primary key auto_increment comment '自增id',
    `cluster_id`      bigint       not null default '0' comment '集群id',
    `pattern`         varchar(192) not null default '' comment 'service user pattern',
    `operation`       int(11)      not null default '0' comment '操作,',
    `permission_type` int(11)      not null default '0' comment '权限类型(0:未知，1:任意，2:拒绝，3:允许)',
    `host`            varchar(192) not null default '' comment '',
    `resource_type`   int(11)      not null default '0' comment '资源类型(0:未知，1:任意，10:kafka topic,11:kafka group;21:rocketmq topic)',
    `resource_name`   varchar(192) not null default '' comment '资源名称',
    `pattern_type`    tinyint(4)   not null comment '匹配类型(0:未知，1:任意，2:match，3:literal，4:prefixed)',
    `status`          int          not null default 1 comment '状态(0:删除，1:存在)',
    `create_time`     timestamp    not null default current_timestamp comment '创建时间',
    `update_time`     timestamp    not null default current_timestamp on update current_timestamp comment '更新时间',
    index `idx_cluster_phy_id_principal_res_name` (`cluster_id`, `pattern`, `resource_name`)
) comment ='acl信息表';



drop table if exists `group`;
create table `group`
(
    `id`           bigint unsigned primary key auto_increment comment 'id',
    `cluster_id`   bigint        not null default '-1' comment '集群id',
    `name`         varchar(192)  not null default '' comment 'group名称',
    `member_count` int unsigned  not null default '0' comment '成员数',
    `members`      varchar(1024) not null default '' comment 'group的member列表',
    `type`         tinyint       not null comment 'group类型 0：consumer 1：producer',
    `state`        varchar(64)   not null default '' comment '状态',
    `create_time`  timestamp     not null default current_timestamp comment '创建时间',
    `update_time`  timestamp     not null default current_timestamp on update current_timestamp comment '修改时间',
    `status`       int           not null default 1,
    unique key `uniq_cluster_phy_id_name` (`cluster_id`, `name`),
    key `cluster_id` (`cluster_id`, `name`)
) comment ='group信息表';


drop table if exists `group_member`;
create table `group_member`
(
    `id`             bigint unsigned primary key auto_increment comment 'id',
    `cluster_id`     bigint          not null default '-1' comment '集群id',
    `client_id`      bigint unsigned not null comment '客服端id',
    `topic_name`     varchar(192)    not null default '' comment 'topic名称',
    `group_name`     varchar(192)    not null default '' comment 'group名称',
    `eventmesh_user` varchar(192)    not null default '' comment 'eventmesh用户',
    `state`          varchar(64)     not null default '' comment '状态',
    `create_time`    timestamp       not null default current_timestamp comment '创建时间',
    `update_time`    timestamp       not null default current_timestamp on update current_timestamp comment '修改时间',
    `status`         int             not null default 1,
    unique key `uniq_cluster_topic_group` (`cluster_id`, `topic_name`, `group_name`),
    key `cluster_id` (`cluster_id`, `topic_name`, `group_name`)
) comment ='groupmember信息表';


drop table if exists `operation_log`;
create table `operation_log`
(
    `id`             bigint unsigned primary key auto_increment,
    `cluster_id`     bigint        not null default '-1' comment '物理集群id',
    `operation_type` varchar(192)  not null default '' comment '操作类型,如:启动，停止，重启，添加，删除，修改',
    `state`          int           not null default '0' comment '操作状态 0:未知，1:执行中，2:成功，3:失败',
    `content`        varchar(1024) not null comment '备注信息',
    `operation_user` varchar(192)  not null comment '操作用',
    `result`         varchar(1024) not null comment '方法返回的 内容',
    `target_type`    varchar(192)  not null comment '目标类型。group，topic',
    `create_time`    timestamp     not null default current_timestamp comment '创建时间',
    `end_time`       timestamp     not null default current_timestamp on update current_timestamp comment '结束时间',
    `is_delete`      int           not null default '0',
    key `idx_cluster_phy_id` (`cluster_id`)
) comment ='操作记录信息表';


drop table if exists `topic`;
create table `topic`
(
    `id`              bigint unsigned primary key auto_increment comment 'id',
    `cluster_id`      bigint       not null default '-1' comment '集群id',
    `topic_name`      varchar(192) not null default '' comment 'topic名称',
    `status`          int          not null default 1,
    `create_progress` int          not null default 1 comment '0:创建成功，1：创建中，2：创建失败',
    `retention_ms`    bigint       not null default '-2' comment '保存时间，-2：未知，-1：无限制，>=0对应时间，单位ms',
    `type`            tinyint      not null default '0' comment 'topic类型，默认0，0:普通，1:eventmesh内部',
    `description`     varchar(1024)         default '' comment '备注信息',
    `create_time`     timestamp    not null default current_timestamp comment '创建时间(尽量与topic实际创建时间一致)',
    `update_time`     timestamp    not null default current_timestamp on update current_timestamp comment '修改时间(尽量与topic实际创建时间一致)',
    `is_delete`       int          not null default '0',
    unique key `uniq_cluster_phy_id_topic_name` (`cluster_id`, `topic_name`)
) comment ='topic信息表';



drop table if exists `connector`;
create table `connector`
(
    `id`          bigint unsigned primary key auto_increment comment 'id',
    `cluster_id`  bigint              not null default '-1' comment '集群id',
    `name`        varchar(512)        not null default '' comment 'connector名称',
    `class_name`  varchar(512)        not null default '' comment 'connector类',
    `type`        varchar(32)         not null default '' comment 'connector类型',
    `host`        varchar(128)        not null default '' comment 'connector地址',
    `port`        int(16)             not null default '-1' comment 'connector端口',
    `status`      tinyint(4) unsigned not null default 1 comment '状态: 1启用，0未启用',
    `pod_state`   tinyint(4) unsigned not null default '0' comment 'k8s pod状态。0: pending;1: running;2: success;3: failed;4: unknown',
    `config_ids`  varchar(1024)       not null default '' comment 'csv config id list, like:1,3,7',
    `create_time` timestamp           not null default current_timestamp comment '创建时间',
    `update_time` timestamp           not null default current_timestamp on update current_timestamp comment '修改时间',
    index `idx_cluster_id` (`cluster_id`)
) comment ='connector信息表';



drop table if exists `health_check_result`;
create table `health_check_result`
(
    `id`          bigint unsigned primary key auto_increment comment '自增id',
    `type`        tinyint(4)      not null default '0' comment '检查维度(0:未知, 1:cluster, 2:runtime, 3:topic)',
    `type_id`     bigint unsigned not null comment '对应检查维度的实例id',
    `cluster_id`  bigint          not null default '0' comment '集群id',
    `state`       tinyint(4)      not null default '0' comment '检查状态(0:未通过，1:通过,2:正在检查,3:超时)',
    `result_desc` varchar(1024)   not null default '' comment '检查结果描述',
    `create_time` timestamp       not null default current_timestamp comment '创建时间',
    `update_time` timestamp       not null default current_timestamp on update current_timestamp comment '更新时间',
    index `idx_cluster_id` (`cluster_id`),
    index `idx_type` (`type`)
) comment ='健康检查结果';







