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


drop table if exists `case`;

create table `case`
(
    id              bigint unsigned primary key auto_increment comment 'id',
    organization_id bigint unsigned not null comment '组织id',
    name            varchar(128)    not null comment '案例名',
    case_type       varchar(16)     not null comment '案例类型',
    object_type     varchar(16)     not null comment '对象类型',
    object_id       varchar(32)     not null comment '',
    status          int             not null default 1 comment '',
    create_time     timestamp       not null default current_timestamp comment '接入时间',
    update_time     timestamp       not null default current_timestamp on update current_timestamp comment '修改时间',
    is_delete       int             not null default 0 comment '数据逻辑标记',
    key object_type_id (object_type, object_id)
) comment ='';

drop table if exists `deploy_script`;

create table `deploy_script`
(
    id                    bigint unsigned primary key auto_increment comment 'id',
    organization_id       bigint unsigned not null comment '组织id',
    name                  varchar(128)    not null comment '脚本名字',
    version               varchar(128)    not null comment '脚本版本',
    cluster_type          varchar(128)    not null comment '脚本类型',
    content               varchar(8192)   not null comment '脚本内容',
    start_runtime_version varchar(16)     not null comment '支持的版本',
    end_runtime_version   varchar(16)     not null comment '支持的版本',
    description           varchar(1024)   not null comment '说明',
    status                int             not null default 1 comment '',
    create_time           timestamp       not null default current_timestamp comment '接入时间',
    update_time           timestamp       not null default current_timestamp on update current_timestamp comment '修改时间',
    is_delete             int             not null default 0 comment '数据逻辑标记',
    key organization_id_index (organization_id)
);

drop table if exists `resources_config`;

create table `resources_config`
(
    id              bigint unsigned primary key auto_increment comment 'id',
    organization_id bigint unsigned not null comment '组织id',
    name            varchar(128)    not null comment '案例名',
    object_type     varchar(16)     not null comment '对象类型',
    object_id       varchar(32)     not null comment '',
    cpu_num         int             not null comment '',
    men_num         int             not null comment '',
    disk_num        int             not null comment '',
    gpu_num         int             not null comment '',
    status          int             not null default 1 comment '',
    create_time     timestamp       not null default current_timestamp comment '接入时间',
    update_time     timestamp       not null default current_timestamp on update current_timestamp comment '修改时间',
    is_delete       int             not null default 0 comment '数据逻辑标记',
    key object_type_id (object_type, object_id)
) comment ='资源配置表';

drop table if exists `cluster`;
create table cluster
(
    id                    bigint unsigned primary key auto_increment comment '集群id',
    organization_id       bigint unsigned not null comment '组织id',
    cluster_type          varchar(64)     not null comment '集群类型',
    name                  varchar(128)    not null comment '集群名称',
    version               varchar(32)     not null comment 'eventmesh版本',
    cluster_index         int             not null default 0 comment '在 集群里面的索引',
    jmx_properties        varchar(256)    not null default '' comment 'jmx配置',
    trusteeship_type      varchar(32)     not null comment '托管类型',
    cluster_own_type      varchar(32)     not null comment '共享类型',
    runtime_index         int             not null default 0 comment 'runtime索引值',
    first_to_whom         varchar(16)     not null comment '第一次同步模式',
    first_sync_state      varchar(16)     not null default 'NOT' comment '第一次同步结果',
    replication_type      varchar(16)     not null comment '复制模式',
    sync_error_type       varchar(16)     not null default 'NOT' comment '同步数据异常标识字段',
    deploy_status_type    varchar(16)     not null comment '部署进行中状态',
    resources_config_id   varchar(16)     not null comment '默认部署配置',
    deploy_script_id      bigint unsigned not null comment '默认部署脚本id',
    deploy_script_name    varchar(16)     not null default '' comment '脚本名字',
    deploy_script_version varchar(16)     not null default '' comment '脚本版本',
    config                varchar(8192)   not null comment '集群配置',
    description           text comment '备注',
    auth_type             varchar(32)     not null default 0 comment '认证类型，-1未知，0:无认证，',
    run_state             tinyint         not null default 1 comment '运行状态, 0表示未监控, 1监控中，有注册中心，2:监控中，无注册中心',
    online_timestamp      datetime        not null default current_timestamp comment '上线时间',
    offline_timestamp     datetime        not null default current_timestamp comment '下线时间',
    status                int             not null default 1 comment '0',
    create_time           timestamp       not null default current_timestamp comment '接入时间',
    update_time           timestamp       not null default current_timestamp on update current_timestamp comment '修改时间',
    is_delete             int             not null default 0 comment '数据逻辑标记',
    unique key uniq_name (name)
) comment '物理集群信息表';


drop table if exists `cluster_relationship`;
create table `cluster_relationship`
(
    id                bigint unsigned primary key auto_increment,
    organization_id   bigint unsigned not null comment '组织id',
    cluster_type      varchar(63)     not null comment '主集群类型',
    cluster_id        bigint          not null comment '主集群id',
    relationship_type varchar(63)     not null comment '关联集群类型',
    relationship_id   bigint          not null comment '关联集群id',
    create_time       timestamp       not null default current_timestamp comment '创建时间',
    update_time       timestamp       not null default current_timestamp on update current_timestamp comment '修改时间',
    status            int             not null default 1 comment '0',
    is_delete         int             not null default 0 comment '0',
    unique key cluster_id_relationship_id_unique (`cluster_id`, `relationship_id`),
    key cluster_id_key (`cluster_id`),
    key relationship_id_key (`relationship_id`)
);

drop table if exists `config`;
create table config
(
    id                 bigint unsigned auto_increment primary key,
    organization_id    bigint unsigned not null comment '组织id',
    cluster_id         bigint          not null comment '集群id，差点删了',
    cluster_type       varchar(32)     not null comment '',
    instance_type      varchar(31)     not null comment '实例类型 0:runtime,1:storage,2:connector,3:topic',
    instance_id        bigint          not null default -1 comment '实例id，上面配置对应的(比如runtime)的id，如果是-1，是cluster的配置',
    config_type        varchar(31)     not null default '' comment '配置类型',
    config_name        varchar(192)    not null comment '配置名称',
    config_value       text            not null comment '配置值',
    config_value_type  varchar(16)     not null comment '值类型,number，string,boolean,date,enum',
    config_value_range varchar(16)     not null comment '',
    start_version      varchar(64)     not null default '' comment '配置开始使用的版本',
    end_version        varchar(64)     not null default '' comment '配置结束使用的版本',
    status             int             not null default 1 comment '0 关闭 1 开启 ',
    is_default         int             not null default 1,
    diff_type          int             not null default -1 comment '差异类型',
    description        varchar(1000)   not null default '' comment '备注',
    edit               int             not null default 1 comment '是否可以编辑 1 不可编辑（程序获取） 2 可编辑',
    create_time        timestamp       not null default current_timestamp comment '创建时间',
    update_time        timestamp       not null default current_timestamp on update current_timestamp comment '修改时间',
    is_modify          int             not null default 0 comment '是否修改元版本数据',
    already_update     int             not null default 0 comment '0:no,1:yes',
    is_delete          int             not null default 0 comment '0',
    unique key uniq_cluster_id_instance_type_instance_id_config_name (instance_id, config_name, instance_type, cluster_id)
) comment '配置信息表';

drop table if exists `topic`;
create table `topic`
(
    `id`               bigint unsigned primary key auto_increment comment 'id',
    `cluster_id`       bigint          not null default '-1' comment '集群id',
    cluster_type       varchar(32)     not null comment '',
    runtime_id         bigint unsigned not null default 0 comment 'kafka 没有runtime',
    `topic_name`       varchar(192)    not null default '' comment 'topic名称',
    topic_type         varchar(16)     not null default '' comment 'topic 类型。用户，broker，console，console',
    read_queue_num     int             not null default 8 comment '读队列数量',
    write_queue_num    int             not null default 8 comment '写队列数量',
    replication_factor int             not null default 0 comment '副本数量',
    `order`            int             not null default 0 comment '是否是定时队列',
    `status`           int             not null default 1,
    `create_progress`  int             not null default 1 comment '0:创建成功，1：创建中，2：创建失败',
    `retention_ms`     bigint          not null default '-2' comment '保存时间，-2：未知，-1：无限制，>=0对应时间，单位ms',
    `description`      varchar(1024)            default '' comment '备注信息',
    `create_time`      timestamp       not null default current_timestamp comment '创建时间(尽量与topic实际创建时间一致)',
    `update_time`      timestamp       not null default current_timestamp on update current_timestamp comment '修改时间(尽量与topic实际创建时间一致)',
    `is_delete`        int             not null default '0',
    unique key `uniq_cluster_phy_id_topic_name` (`cluster_id`, `topic_name`)
) comment ='topic信息表';

drop table if exists `group`;
create table `group`
(
    id              bigint unsigned primary key auto_increment comment 'id',
    organization_id bigint unsigned not null comment '组织id',
    cluster_id      bigint          not null comment '集群id',
    cluster_type    varchar(32)     not null comment '',
    name            varchar(192)    not null comment 'group名称',
    type            tinyint         not null comment 'group类型 0：consumer 1：producer',
    own_type        varchar(16)     not null default '' comment 'topic 类型。用户，broker，console，console',
    state           varchar(64)     not null default '' comment '状态',
    create_time     timestamp       not null default current_timestamp comment '创建时间',
    update_time     timestamp       not null default current_timestamp on update current_timestamp comment '修改时间',
    status          int             not null default 1,
    is_delete       int             not null default 0 comment '0',
    unique key uniq_cluster_phy_id_name (cluster_id, name)
) comment 'group信息表';


drop table if exists group_member;
create table group_member
(
    id              bigint unsigned primary key auto_increment comment 'id',
    organization_id bigint unsigned not null comment '组织id',
    cluster_id      bigint          not null default -1 comment '集群id',
    topic_name      varchar(192)    not null default '' comment 'topic名称',
    group_name      varchar(192)    not null default '' comment 'group名称',
    eventmesh_user  varchar(192)    not null default '' comment 'eventmesh用户',
    state           varchar(64)     not null default '' comment '状态',
    create_time     timestamp       not null default current_timestamp comment '创建时间',
    update_time     timestamp       not null default current_timestamp on update current_timestamp comment '修改时间',
    status          int             not null default 1,
    is_delete       int             not null default 0 comment '0',
    unique key uniq_cluster_topic_group (cluster_id, topic_name, group_name)
) comment 'groupmember信息表';

drop table if exists `offset`;

create table offset
(
    id                 bigint unsigned primary key auto_increment comment 'id',
    organization_id    bigint unsigned not null comment '组织id',
    cluster_id         bigint unsigned not null default -1 comment '集群id',
    runtime_id         bigint unsigned not null default -1 comment '',
    offset_record_type varchar(16)     not null default '' comment 'topic or consume',
    topic_id           bigint unsigned not null default '',
    topic_name         varchar(128)    not null default '',
    queue_index        bigint          not null default '',
    topic_offset       bigint unsigned not null default '',
    group_id           bigint unsigned not null default '',
    group_name         varchar(128)    not null default '',
    consume_offset     bigint unsigned not null default '',
    consume_rote       bigint unsigned not null default '消费速率，写到这个表？',
    delay_num          bigint unsigned not null default '延迟数量',
    create_time        timestamp       not null default current_timestamp comment '创建时间',
    update_time        timestamp       not null default current_timestamp on update current_timestamp comment '修改时间',
    status             int             not null default 1,
    is_delete          int             not null default 0 comment '0',
    key cluster_topic_group (cluster_id, topic_name, group_name)

);


drop table if exists runtime;
create table runtime
(
    id                    bigint primary key auto_increment comment 'id',
    organization_id       bigint unsigned not null comment '组织id',
    cluster_id            bigint          not null default -1 comment '集群id',
    cluster_type          varchar(63)     not null comment '集群类型',
    `name`                varchar(128)    not null comment '节点名字',
    host                  int             not null comment 'runtime 对外的 IP，kubernetes这个ip 有问题',
    port                  int             not null comment 'runtime port',
    runtime_index         int             not null default -1 comment 'runtime 在 cluster 里面的索引',
    version               varchar(32)     not null comment 'runtime版本',
    jmx_port              varchar(256)    not null default '' comment 'jmx配置',
    trusteeship_type      varchar(16)     not null comment '托管类型',
    first_to_whom         varchar(16)     not null comment '第一次同步模式',
    first_sync_state      varchar(16)     not null default 'NOT' comment '第一次同步结果',
    replication_type      varchar(16)     not null comment '节点在集群的复制类型',
    sync_error_type       varchar(16)     not null default 'NOT' comment '同步数据异常标识字段',
    deploy_status_type    varchar(16)     not null comment '部署进行中状态',
    kubernetes_cluster_id bigint unique   not null default 0 comment 'kubernetes cluster id',
    create_script_content text            not null comment ' kubernetes 创建时的内容',
    resources_config_id   varchar(16)     not null comment '默认部署配置',
    deploy_script_id      bigint unsigned not null comment '默认部署脚本id',
    deploy_script_name    varchar(16)     not null default '' comment '脚本名字',
    deploy_script_version varchar(16)     not null default '' comment '脚本版本',
    auth_type             varchar(16)     not null default '' comment '认真类型',
    description           text comment '备注',
    rack                  varchar(128)    not null default '' comment '哪个机架信息',
    status                int             not null default 1 comment '状态: 1启用，0未启用',
    online_timestamp      datetime        not null default current_timestamp comment '上线时间',
    offline_timestamp     datetime        not null default current_timestamp comment '下线时间',
    create_time           timestamp       not null default current_timestamp comment '创建时间',
    update_time           timestamp       not null default current_timestamp on update current_timestamp comment '修改时间',
    endpoint_map          varchar(1024)   not null default '' comment '监听信息',
    is_delete             int             not null default 0 comment '0',
    unique key uniq_cluster_phy_id_host_port (cluster_id, host)
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
    `id`                bigint unsigned primary key auto_increment comment '自增id',
    `cluster_type`      varchar(64)     not null comment '集群类型',
    `cluster_id`        bigint          not null default '0' comment '集群id',
    `protocol`          varchar(64)     not null comment '协议',
    `type`              tinyint(4)      not null default '0' comment '检查维度(0:未知, 1:cluster, 2:runtime, 3:topic)',
    `type_id`           bigint unsigned not null comment '对应检查维度的实例id',
    `address`           varchar(64)     not null comment '地址',
    `health_check_type` varchar(64)     not null comment '心跳类型',
    `result`            varchar(64)     not null comment '心跳结果',
    `result_desc`       varchar(1024)   not null default '' comment '检查结果描述',
    `begin_time`        timestamp       not null comment '创建时间',
    `finish_time`       timestamp       not null default current_timestamp on update current_timestamp comment '更新时间',
    unique `uni_type_id_begin_time_type` (`type_id`, `type`, `begin_time`),
    index `idx_cluster_id` (`cluster_id`)
) comment ='健康检查结果';



drop table if exists `port`;

create table `port`
(
    `id`           bigint unsigned primary key auto_increment comment '自增id',
    `cluster_id`   bigint    not null comment '集群id',
    `runtime_id`   bigint    not null comment '节点id',
    `current_port` int       not null default 0 comment '当前 port value',
    `status`       int                default 1 not null comment '状态: 1启用，0未启用',
    `create_time`  timestamp not null default current_timestamp comment '创建时间',
    `update_time`  timestamp not null default current_timestamp on update current_timestamp comment '修改时间',
    is_delete      int       not null default 0 comment '0',
    unique key unique_cluster_id (cluster_id)
);



