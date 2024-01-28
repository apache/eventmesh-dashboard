create table operation_log
(
    id             bigint unsigned auto_increment comment 'id'
        primary key,
    cluster_id     bigint       default -1                not null comment '物理集群ID',
    operation_type varchar(192) default ''                not null comment '操作类型,如:启动，停止，重启，添加，删除，修改',
    status         int          default 0                 not null comment '操作状态 0:未知，1:执行中，2:成功，3:失败',
    description    text                                   null comment '备注信息',
    create_time    timestamp    default CURRENT_TIMESTAMP not null comment '创建时间',
    end_time       timestamp    default CURRENT_TIMESTAMP null comment '结束时间'
)
    comment '操作记录信息表';

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
    constraint uniq_cluster_phy_id_topic_name
        unique (cluster_id, topic_name)
)
    comment 'Topic信息表';

create index cluster_id
    on topic (cluster_id, topic_name);



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
    constraint uniq_cluster_phy_id_topic_name
        unique (cluster_id, topic_name)
)
    comment 'Topic信息表';

create index cluster_id
    on topic (cluster_id, topic_name);





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
    constraint uniq_cluster_phy_id_name
        unique (cluster_id, name)
)
    comment 'Group信息表';

create index cluster_id
    on `group` (cluster_id, name);

