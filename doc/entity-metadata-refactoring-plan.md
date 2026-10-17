# Entity、Console 与 Metadata 字段重构方案

最近核查：**2026-10-17**。代码基线：`feature/entity-field-standardization`，提交 `616d9d0`（已合并 `dev-maven-release`），以及本地尚未提交的六个 Entity、三个 Metadata 字段去重、ACL/Connection HTTP 修复和三类 Metadata 同步链路。

**当前结论：Topic/Config 名称统一、Cluster/Runtime 身份与继承整理已落地，六个 Entity 及 AclMetadata、ConnectionMetadata、ClusterRelationshipMetadata 已完成重复字段清理，查询侧完成了一批修复。三类 Metadata 已补齐转换、注册及同步：ACL 对接 Kafka，Connection 对接 Console 内存，集群关系对接内存拓扑；剩余名称、DTO 类型角色及通用生命周期重构尚未完成。**

| 工作范围 | 当前状态 |
| --- | --- |
| Topic/Config 的 Entity、DTO、Metadata 名称及调用链 | 已完成；旧 Entity 访问方法作为 SQL 兼容桥接保留 |
| Cluster/Runtime 自身编号、所属编号与继承结构 | 已完成；相关查询、转换和身份消费边界已有验证 |
| AclEntity、ConnectionEntity、NetConnectionEntity 重复身份字段 | 已完成；使用父类存储，AclEntity 独有的 clusterId 保留 |
| InstanceUserEntity、ConnectorEntity、LogEntity 重复身份字段 | 已完成；使用父类存储，LogEntity 保留原九参数构造器，字段契约及相关 Mapper 回归通过 |
| 查询过滤、查询结果组装、数据库读取失败保留游标 | 已完成当前查询范围；不代表变更应用及运行时动作已完成 |
| AclMetadata、ConnectionMetadata、ClusterRelationshipMetadata 重复字段 | 已清理，使用父类存储；关系端点字段及原继承结构保留 |
| 三类 Metadata 转换、注册、同步 | 已实现 Kafka ACL、Connection 内存配置、集群关系拓扑三条路径；不修改表结构，启用配置及验收边界见 3.5 和第六章 |
| 剩余 Metadata 名称和 DTO 类型角色 | 待完成或部分完成，逐项状态见第二、三章 |
| 通用状态解释、写入语义、注册/注销、SDK 生命周期 | 相关增强已于 2026-10-09 撤回；本次仅补齐三类 Metadata 范围内的状态消费、关系解绑和应用失败重试 |

第六章保留分阶段历史记录，当前状态以本次核查及后续范围调整为准。历史测试通过不代表被撤回的实现仍在当前代码中，也不代表已授权恢复这些改动。

## 1. 目标（STAR）

- **S｜背景**：`id/xxxId`、`name/xxxName`、`clusterType` 的混用与重复定义，使 Entity、Console、Metadata 对同一资源的身份和类型缺乏一致解释。Cluster、Runtime 的核心流程又依赖复杂的 status 判断，一处取值或判定错误，就会沿查询、注册、拓扑维护和同步链路传递，造成目标误定位、停用事件丢失、关系无法解绑或错误同步，直接破坏资源生命周期管理的正确性。
- **T｜目标**：自身编号统一为 `id`，自身名称统一为 `name`，保留关联字段；清理重复定义，分清业务状态与逻辑删除。
- **A｜行动**：先整理 Entity 和 Console，再联动调整 Metadata、字段映射及注册、同步、注销方法。
- **R｜预期结果**：建立贯穿 Entity、Console、Metadata 的统一身份与状态规则，消除核心流程中“操作哪个对象、应执行哪个动作”的歧义。让 Cluster、Runtime 的状态变更完整驱动注册、同步、注销和解绑，确保数据库记录、内存拓扑与操作目标保持一致，为后续功能扩展提供可靠的资源生命周期基础。

## 2. 涉及的类及字段

下表列出已完成和计划中的字段改动，以及对应的处理要求。Entity 与 Metadata 是平行模型层，两边出现同名字段不构成继承重复。仅同一对象的父子继承链中存在同语义的多份存储，才属于字段遮蔽问题。

### 2.1 Entity

| 类 | 状态 | 当前实现与剩余要求 |
| --- | --- | --- |
| TopicEntity | 已完成 | `topicName` → `name`；旧 getter/setter 转接同一字段，适配原 SQL，不是第二份名称存储 |
| ConfigEntity | 已完成 | `configName` → `name`；保留旧访问方法适配原 SQL；`instanceId/instanceType` 保持关联目标语义 |
| RuntimeEntity | 已完成 | 删除重复 `name`，使用父类字段；自身 `id` 与所属 `clusterId` 同时保留 |
| ClusterEntity | 已完成 | 自身仅使用 `id`；通过调整 BaseSyncEntity 继承移除 `clusterId`；不保留自身编号的第二份存储 |
| BaseIdEntity | 待完成 | `status`、`isDelete` 的统一职责和逐表写入规则尚未落地；查询侧修复见第三章 |
| BaseSyncEntity | 已完成 | 直接继承 BaseOrganizationEntity，保留名称、类型及同步属性；RuntimeEntity 独立保留父级 `clusterId` |
| AclEntity | 已完成 | 删除重复 `id`，使用 BaseIdEntity 字段；保留本类独有的 `clusterId` |
| ConnectionEntity | 已完成 | 删除重复 `clusterId/runtimeId`，使用父类字段；`getClusterId()` 委托父类，保留原 OpenAPI 描述 |
| NetConnectionEntity | 已完成 | 删除重复 `clusterId/runtimeId`，使用父类字段；现有 MapStruct 转换的身份字段往返已验证 |
| InstanceUserEntity | 已完成 | 删除重复 `clusterId`，继承 BaseClusterIdEntity 的字段及访问方法 |
| ConnectorEntity | 已完成 | 删除重复 `clusterId`，继承 BaseClusterIdEntity 的字段及访问方法 |
| LogEntity | 已完成 | 删除重复 `id/clusterId`，使用父类字段；保留无参和原九参数构造器，后者将身份写入父类 |

### 2.2 DTO

| 类 | 状态 | 当前实现与剩余要求 |
| --- | --- | --- |
| CreateTopicDTO | 已完成 | `topicName` → `name` |
| GetTopicListDTO | 已完成 | `topicName` → `name` |
| ChangeConfigDTO | 已完成 | `configName` → `name` |
| GetConfigsListDTO | 已完成 | `configName` → `name` |
| QueryByInstanceIdDTO | 已完成 | `configName` → `name` |
| ClusterRequestDTO | 已完成 | 以 `id` 表示选中集群自身；ClusterIdDTO 继续表达资源所属集群 |
| CreateClusterByFullAddressDTO | 待完成 | 仍使用 `clusterName`；对应 Controller 方法为空实现，不能只凭字段改名认定创建流程完成 |
| QueryClusterByOrganizationIdAndTypeDTO | 待完成 | 仍使用 `clusterName`；名称映射和查询条件尚未贯通，不能只修改 DTO |
| CreateClusterDTO、CreateClusterByFormDataDTO | 待完成 | 子类与 ClusterIdDTO 仍均声明 `clusterType`；需先确认创建目标与已选对象的类型角色，再去重或区分 |
| QueryRelationClusterByClusterIdAndTypeDTO | 部分完成 | 已改为继承 ClusterRequestDTO 的无字段子类，自身 `id` 契约与重复存储清理已完成；已选集群类型与关联目标类型的职责仍待明确及补测 |

### 2.3 Metadata

| 类 | 状态 | 当前实现与剩余要求 |
| --- | --- | --- |
| TopicMetadata | 已完成 | `topicName` → `name`，`nodeUnique()` 使用 `name` |
| ConfigMetadata | 已完成名称统一 | `configName` → `name`；合并上游后 `nodeUnique()` 支持目标作用域，规则见 3.2；远程目标信息并未因此持久化 |
| BaseOrganizationBase | 部分完成 | 已承接两条继承分支共用的状态判断方法；方法上移不代表语义修复。`getUnique()` 缓存与 id 生命周期仍待处理：首次无 id 会异常，缓存后改 id 不会失效 |
| BaseClusterIdBase | 已完成方法上移 | `isDelete/isInsert/isUpdate` 现继承自 BaseOrganizationBase；统一删除来源和分类优先级仍待完成 |
| BaseSyncBase | 已完成身份拆分 | 直接继承 BaseOrganizationBase，保留单份 `clusterType/trusteeshipType` 及同步属性，不再继承集群归属字段 |
| ClusterMetadata | 已完成 | 自身仅使用 `id`，无 `clusterId`；`nodeUnique()` 使用自身 id；重复 `clusterType/trusteeshipType` 已清理 |
| RuntimeMetadata | 已完成 | 保留自身 `id`、独立父级 `clusterId` 与 `storageClusterId`；`clusterUnique()` 使用父集群编号，`nodeUnique()` 保留 host-port 语义 |
| AclMetadata | 已完成去重及 Kafka 同步 | 使用父类 clusterId；新增 Entity 转换与 Kafka ACL 同步，RocketMQ 保留独立 RPC，不进入持久化同步 |
| ConnectionMetadata | 已完成去重及内存同步 | 使用父类 runtimeId；新增 Entity 转换并保留已有 end_time，经注册同步到 Console 内存配置 |
| ClusterRelationshipMetadata | 已完成去重及拓扑同步 | 删除重复 `clusterType`，继承 BaseClusterIdBase 的字段；`relationshipId/relationshipType` 表示另一端，独立保留。继承的 `runtimeId` 并非重复字段，本轮保留公开契约，不改关系作用域；新增 Entity 转换、注册及关系绑定/解绑 |
| InstanceUserMetadata | 待完成 | 仍使用 `userName`；待统一为 `name` 并调整调用者 |
| QueueMetadata | 待完成 | 仍使用 `queueName` 且无访问方法；待统一为 `name` 并补 getter/setter；关联 `topicName` 保留 |

## 3. 方法调整与当前状态

### 3.1 ID 与集群查询

| 类 | 方法 | 影响内容 |
| --- | --- | --- |
| QueryClusterInSyncDO | `create()` | 已完成：将新建且设置了 id 的 ClusterEntity 放入返回对象 |
| ClusterMetadataDomain | `setMainCluster()` | 已完成身份调整：使用 ClusterEntity.id，并正确包装为 ClusterBaseDO |
| ClusterControllerMapper | `toClusterEntity(ClusterRequestDTO)`、`queryRelationClusterByClusterIdAndType()` | 已完成 id 映射：选中集群的请求同名映射到 Entity.id；资源所属 clusterId 保留；关联类型角色仍待明确 |
| ClusterControllerMapper | `queryClusterByOrganizationIdAndType()` | 待完成：DTO 仍用 clusterName，名称未映射到 Entity.name，SQL 也没有名称过滤条件 |
| ClusterController | `createClusterByFullAddress()` | 待完成：DTO 仍用 clusterName，方法体为空 |
| ClusterAndRuntimeDomainImpl.GetSyncObjectHandler | `structureRelationship()` | 已完成：Cluster.id 建键、补入主集群、按关系两端组装列表；Runtime 按父级 clusterId 归组 |
| DefaultDataHandler | `unRegisterCluster()` | 已完成身份调整：健康注销使用 ClusterEntity.id；不代表 SDK 关闭等生命周期保障已完成 |
| SyncMetadataCreateFactory | 请求构造中的 Cluster/Runtime 分支 | 已完成：两个分支均读取目标自身 id，分别填入查询请求的 runtimeId/clusterId |
| ClusterMetadata、RuntimeMetadata、BaseOrganizationBase | `nodeUnique()`、`clusterUnique()`、`getUnique()` | 目标内、所属集群和管理器身份保持不同职责；Cluster/Runtime 身份已调整，getUnique 缓存失效和无 id 边界仍待处理 |

### 3.2 名称读写与转换

| 类 | 方法 | 影响内容 |
| --- | --- | --- |
| TopicControllerMapper | `createTopic()`、`queryTopicListByClusterId()` | 已完成：DTO.name → Entity.name |
| ConfigControllerMapper | `queryByInstanceId()` | 已完成：DTO.name → Entity.name |
| TopicServiceImpl | `setSearchCriteria()` | 已完成：使用 getName() 构造查询条件 |
| TopicServiceImpl | `getTopicDetailGroups()`、`addTopic()` | 已完成：读取 Topic.name 填充关联对象的 topicName |
| ConfigServiceImpl | `logUpdateRuntimeConfigs()`、`logUpdateStoreConfigs()`、`logUpdateConnectorConfigs()`、`logUpdateTopicConfigs()` | 已完成名称调整：使用 ChangeConfigDTO.name 构造配置对象 |
| ConfigServiceImpl | `updateConfigsByInstanceId()` | 已完成名称调整：使用 name 作为配置键 |
| TopicConvertMetaData、ConfigConvertMetaData | `toEntity()`、`toMetaData()` | 已完成名称映射：Entity 与 Metadata 同名复制 name |
| TopicMetadata | `nodeUnique()` | 已完成：使用 name 识别 Topic |
| ConfigMetadata | `nodeUnique()` | 已完成名称兼容：instanceType/instanceName 均存在时返回 `类型:目标名称长度:目标名称:配置名`，否则返回 name |
| RocketMQTopicRemotingService | `createTopic()`、`deleteTopic()`、`getAllTopics()` | 已完成模型访问调整：使用 getName/setName，第三方 SDK 名称不变 |
| RocketMQConfigRemotingService | `getConfig()`、`getAllConfigs()`、`addProperty()` | 已完成模型访问调整：配置名称读取、结果回填及配置键写入使用 name |
| KafkaTopicRemotingService、KafkaConfigRemotingService | Topic/Config 模型读写 | 2026-10-13 合并时已适配 name，并同步调整新增 Kafka 测试中的模型访问方法 |
| TopicEntity | `getTopicName()`、`setTopicName()` | 已完成兼容桥接：转接到 name，保留原 SQL 的参数与结果映射 |
| ConfigEntity | `getConfigName()`、`setConfigName()` | 已完成兼容桥接：转接到 name，保留原 SQL 的参数与结果映射 |
| InstanceUserMetadata | `getUserName()`、`setUserName()` | 待完成：改为 getName/setName 并调整调用者 |
| QueueMetadata | 名称 getter/setter（需补充） | 待完成：queueName 改为 name 后提供访问方法 |

ConfigMetadata.instanceName 是远程目标名称，现有 Entity、表结构和转换并未保存该字段。目标作用域身份支持不等于数据库往返能够完整保留这份身份信息，也不表示整条 Config 持久化同步链路已验收。

### 3.3 重复字段清理

| 类 | 方法 | 影响内容 |
| --- | --- | --- |
| RuntimeEntity | `getName()`、`setName()` | 已完成：使用父类字段及访问方法 |
| AclEntity | `getId()`、`setId()` | 已完成：继承 BaseIdEntity 的访问方法与单份 id 存储 |
| LogEntity | `getId()`、`setId()` | 已完成：继承 BaseIdEntity 的访问方法与单份 id 存储 |
| ConnectionEntity、NetConnectionEntity | `getClusterId()`、`setClusterId()`、`getRuntimeId()`、`setRuntimeId()` | 已完成：使用父类存储，关联含义不变；Connection 的 clusterId getter 仅委托父类并承载原 Schema 注解 |
| InstanceUserEntity、ConnectorEntity、LogEntity | `getClusterId()`、`setClusterId()` | 已完成：使用父类 clusterId，关联含义不变 |
| AclMetadata | `getClusterId()`、`setClusterId()` | 已完成：使用父类 clusterId，关联含义不变 |
| ConnectionMetadata | `getRuntimeId()`、`setRuntimeId()` | 已完成：使用父类 runtimeId |
| ClusterRelationshipMetadata | `getClusterType()`、`setClusterType()` | 已完成：使用父类 clusterType；另一端的 relationshipType 仍独立存储 |
| CreateClusterDTO、CreateClusterByFormDataDTO | `getClusterType()`、`setClusterType()` | 待完成：先确认字段角色，再去重或明确区分 |
| QueryRelationClusterByClusterIdAndTypeDTO | `getClusterType()`、`setClusterType()` | 重复存储已清理；已选集群与关联目标的类型角色仍待明确及补测 |
| ClusterMetadata | `getClusterType()`、`setClusterType()`、`getTrusteeshipType()`、`setTrusteeshipType()` | 已完成：使用 BaseSyncBase 的单份字段 |

去重还会改变 Lombok 生成的 `equals/hashCode/toString`：删除字段并不只是删掉一对访问方法。应核对父类参与方式、Jackson 属性发现、MyBatis 反射及重新生成的 MapStruct 实现，不直接编辑 target 下的生成代码。

本轮三个 Metadata 保留 `@EqualsAndHashCode(callSuper = true)`；其父类未实现值相等，当前仍受 Object 对象身份约束，不能套用 Entity 的值比较规则。删除的字段不再由子类生成的 hashCode/toString 单独处理，父类的比较策略未在本轮重写。

### 3.4 删除与状态

| 类 | 方法 | 影响内容 |
| --- | --- | --- |
| BaseIdEntity、BaseOrganizationBase | `getStatus()`、`setStatus()`、`getIsDelete()`、`setIsDelete()` | 待完成：统一状态与删除标记职责 |
| BaseOrganizationBase | `isDelete()`、`isInsert()`、`isUpdate()` | 语义修复已撤回：isDelete() 仍判断 status==0，未以 isDelete 字段为准；分类优先级未统一 |
| GroupMapper | `deleteGroup()`、普通查询与 `syncGet()` | 查询侧已调整；删除写入仍为 status=1，与有效状态相同，生产删除语义未修复 |
| ConfigMapper | `deleteConfig()`、`selectAll()`、`syncGet()` | 查询侧已兼容 isDelete 标记及旧 status=0 删除方式；生产删除仍写 status=0 |
| GroupMemberMapper | `deleteGroupMember()`、普通查询与 `syncGet()` | 查询侧已区分逻辑删除，status=0 仍可见；生产删除仍仅写 status=0，写读语义未统一 |
| TopicMapper | `queryTopicsToFrontByClusterId()`、`syncGet()` | 已完成查询范围：普通查询隐藏删除记录，同步查询保留需传播的记录 |
| ClusterConvertMetaData、RuntimeConvertMetaData、TopicConvertMetaData、ConfigConvertMetaData、GroupConvertMetaData、GroupMemberConvertMetaData | `toEntity()`、`toMetaData()` | 保留 status/isDelete 和时间戳仍是转换要求；Cluster/Runtime 已有往返校验，不据此认定全资源状态执行已验收 |
| RuntimeMapper、ClusterMapper、ClusterRelationshipMapper | 普通及增量查询、停用/解除写入 | 增量查询使用 update_time >= 并保留终止状态；本次关系消费已处理 status=3/0 及 isDelete=1 解绑，Runtime/Cluster 的通用生命周期仍未完成 |
| BodyDataDifference | `doDifference()` | 删除优先处理的改动已撤回；当前仍先判断新增，时间相等可能覆盖删除事实 |
| AbstractDBDataMetadataHandler | `getData()` | 已完成读取失败边界：查询成功后才更新秒级游标；拓扑/RPC 应用失败后的确认与重试边界仍待处理 |
| RuntimeController | `queryRuntimeListByClusterId()` | 独立健康展示改动已撤回；当前仍用健康结果覆盖返回对象的 status |

本节严格区分查询修复与状态执行。既有七个 Mapper 的 SELECT 调整、查询组装和读取游标修复仍保留。本次新增的状态消费与同步仅覆盖下节三类 Metadata，不代表其他资源的生产删除、注销与 SDK 生命周期已完成。

### 3.5 三类 Metadata 转换、注册与同步

| 类型 | 转换与注册 | 同步终点和行为 |
| --- | --- | --- |
| ACL | AclConvertMetaData、AclDataMetadataHandler、DatabaseAndMetadataType.ACL | 数据库快照 → KafkaAclSnapshotHandler → 现有 Kafka ACL Remoting/SDK → Broker。使用完整 ACL 绑定字段区分规则，精确创建/删除，保留无关远端规则 |
| Connection | ConnectionConvertMetaData、ConnectionDataMetadataHandler、DatabaseAndMetadataType.CONNECTION | 数据库快照 → ConnectionSnapshotHandler 内存存储；按数据库 id 建键、按 clusterId/runtimeId 查询；停用、删除或 endTime 非空时移除 |
| 集群关系 | ClusterRelationshipConvertMetaData、ClusterRelationshipDataMetadataHandler、DatabaseAndMetadataType.CLUSTER_RELATIONSHIP | 数据库快照 → ClusterRelationshipSnapshotHandler → ColonyDO；校验端点类型、多父节点、自环与循环；status=3/0 或 isDelete=1 时解绑 |

- 复用 MetadataSyncManage 调度，新加 SnapshotMetadataHandler 扩展。三类数据均先完整读取和转换，再应用快照；失败后下一轮重读，不依赖增量游标。Connection 原子替换内存快照；关系先校验后修改；Kafka 保留已确认操作，失败后幂等重试。不同类型间不提供跨类型事务。
- 修正同步管理器按已配置数据库 Handler 选取实例的逻辑，支持普通实例与 Spring 子类代理；缺少来源或目标时明确失败。FunctionManage 在拓扑处理成功后推进游标，并关闭自己持有的同步管理器线程。
- Kafka ACL 沿用现有表：pattern ↔ principal，resource_type 10/11 ↔ TOPIC/GROUP，permission_type 2/3 ↔ DENY/ALLOW；resource_type=21 的旧 RocketMQ 记录跳过。无法保存的 RocketMQ 策略字段显式拒绝转换，独立 RPC 保持现状。
- 新路径是数据库到目标的单向同步，不接入通用 FirstToWhom 反向回写或逐资源结果审计。ACL 不使用旧的仅 principal 身份作差异键。进程重启后，数据库已经覆盖掉的旧 ACL 绑定无法凭现有表重建历史；删除记录中仍保留的完整绑定可在首轮同步删除。
- 需在现有 function.enabled=true、enabledSync=true 的配置下，将 ACL、CONNECTION、CLUSTER_RELATIONSHIP 加入 includeSyncType。当前 application-dev.yml 仍只选择 TOPIC，本次未自动启用新任务。Connection 不部署 Source/Sink Connector。
- 转换保留共有的身份、状态、审计字段及 Connection 的 endTime。Connection 的 Source/Sink 展示名称、地址未新增持久化；关系的 relationshipTime、unRelationshipTime、bindType 在现有 Metadata 中无对应字段，不承诺这些历史信息的 Entity 往返保存。
- 未修改 DDL、迁移脚本或前端。新增 Mapper SELECT 使用现有列；Connection/Connector 的仓库建表脚本缺失或异常仍是环境限制，具体测试边界见第六章。

## 4. 字段使用规则

本章描述目标规则，尚未全部实现；各项落地状态以第二、三章为准。需要统一明确对象“是谁、叫什么、是什么类型、处于什么状态”，让业务代码与同步代码使用同一套规则。

### 4.1 id 与 xxxId：分清自己和归属

自身主键统一用 `id`，关联编号保留 `clusterId/runtimeId/relationshipId/instanceId`。例如：

```java
runtime.getId();        // 100：节点自己的编号
runtime.getClusterId(); // 10：所属集群的编号
```

Cluster 自身用 id 标识；其他对象引用该集群时，用 clusterId 保存这个编号。自身编号与关联编号承担不同职责。

### 4.2 name 与 xxxName：统一自身名称的访问方式

有自身名称的 Entity 与 Metadata 统一使用 `name`，公共功能通过标准访问方法读取：

```java
topic.getName();
config.getName();
```

公共功能无需分别适配 getTopicName/getConfigName。“直接使用 name”指统一属性和访问方法，仍保留 getter/setter，以及 DTO、Entity、Metadata 之间的对象转换。

关联名称保留前缀，例如 GroupMember.topicName/groupName。Topic/Config 的旧名称访问方法目前用于适配原 SQL，完成 Mapper 参数与结果映射调整后再移除。

### 4.3 clusterType 与重复字段：同一含义只存一份

父类已有 clusterId、name 或 clusterType，子类又声明同义字段，就会产生两份独立存储。清理子类重复声明，统一使用父类字段，并同步检查 Lombok 访问方法、对象转换和数据库映射。

Entity 与 Metadata 是不同用途的对象，各自拥有同名字段是正常的。去重针对同一个对象的父子继承链。

类型按角色区分：所属集群类型、节点组件类型、关系另一端类型各自保留；父子字段角色相同才合并。已有类型枚举值保持不变。

### 4.4 status 与 isDelete：明确核心流程的状态驱动规则

`status` 整理的难点在于 **Cluster、Runtime 的状态种类多，而且绝大部分主逻辑和核心流程都依据这些状态进行流转**。状态值决定资源是否加载、注册、参与同步、停用或注销，是控制执行路径的关键依据。

因此，本次整理需要明确每个状态的含义、允许的转换、写入位置和消费分支，使 Console 的状态写入、数据库增量查询、core 的注册/同步/注销使用同一套规则。

| 字段 | 职责与处理要求 |
| --- | --- |
| status | 表达 Cluster、Runtime 等资源的业务状态；逐项定义状态值、转换条件及对应动作 |
| isDelete | 表达逻辑删除；明确删除前置条件，以及删除后需要执行的注销、解绑与同步动作 |
| 部署、健康、同步相关状态字段 | 分别记录对应过程或结果；与业务状态的联动通过明确规则完成 |

普通展示查询过滤已删除数据；变更查询必须保留需要传播的停用、删除和解绑记录，直到对应处理端完成动作。重点是让每次状态变化都能驱动正确的核心流程，具体源码矛盾和风险见第五章。

## 5. 风险

### 5.1 已定位的源码矛盾及当前状态

| 风险项 | 源码中的写入与处理矛盾 | 可能后果 |
| --- | --- | --- |
| Runtime 状态传播与执行未闭环 | 原增量查询仅取 status=1 的问题已修复；status=0 会进入移除/注销分支，但 isDelete=1、status=1 仍走注册分支，SDK 移除也未保证关闭客户端 | 能读取停用/删除记录，不等于生命周期动作已完整执行 |
| 集群关系解除识别 | 已修复：status=3/0 和 isDelete=1 均进入解绑；初始/增量拓扑加载与新快照目标共用状态规则 | 已覆盖真实解除接口写库后，内存关系及 superiorId 清理 |
| 删除记录被归类为新增 | isDelete() 仍按 status==0 判断；同步分类先判断新增，新增又依赖创建时间与更新时间相等 | 逻辑删除标记可能被忽略，或已删除记录先命中新增分支 |
| 生产删除写入与查询规则不一致 | Group 删除仍写 status=1；GroupMember 删除仍写 status=0，但其普通查询保留未标记逻辑删除的 status=0 记录 | 调用生产删除方法后记录仍可能可见；查询测试通过不证明删除功能正确 |
| 健康结果覆盖业务状态 | RuntimeController 仍把健康结果写入返回对象的 status | 接口展示混淆健康结果与持久化业务状态；不表示此处执行了数据库更新 |
| 读取成功与应用成功边界 | 已修复 FunctionManage 拓扑处理失败前移游标；三类新 Metadata 使用完整快照重试 | 其他元数据的通用远程同步确认、反向回写及 SDK 生命周期仍需独立验证 |
| 缓存身份与 id 变化不一致 | getUnique() 首次计算后缓存，setId() 不清缓存；首次计算直接使用 id.toString() | 换 id 后可能继续使用旧身份；无 id 时可能异常 |

### 5.2 本次重构需要消除的风险

- **身份误定位**：把 Cluster 自身 id 与关联 clusterId 混用，可能查询、注册或操作错误对象。
- **停用/删除处理不完整**：查询侧已区分展示过滤和变更读取；仍需处理逻辑删除消费、注销关闭及应用失败后的重试，避免“读到但未正确执行”。
- **状态相互覆盖**：业务状态、逻辑删除、健康结果及部署过程共用或覆盖同一字段，可能改变后续状态判断。
- **错误同步**：新增、更新、删除判定不互斥，或写入端与消费端状态定义不同，可能执行与原操作相反的动作。

本次重构的目的，是消除以上错误发生的条件。重点检查 Runtime、Cluster 从状态写入、增量加载到内存注册/注销、关系绑定/解绑及同步分类的完整处理链。

### 5.3 验证场景与边界

| 场景 | 当前验证范围 | 尚待验证或修复 |
| --- | --- | --- |
| Runtime 停用或逻辑删除 | 增量查询代码已不按有效状态过滤 | 停用、逻辑删除、地址清空等路径均正确注销和释放资源 |
| 集群关系解除 | 已覆盖真实 HTTP → MySQL → 同步管理器 → 内存拓扑的 status=3 解绑，以及冲突/循环拒绝 | 完整应用长期运行和集群生命周期联动仍未验收 |
| 已删除且创建时间等于更新时间 | 已确认当前分类顺序仍有风险 | 仅进入删除分支；不触发新增 |
| 数据库与应用失败 | 已覆盖查询/转换失败不应用快照、拓扑失败不推进游标、Kafka 失败重试 | 其他元数据的通用双向同步确认及完整应用长时间调度 |
| Topic/Config/Group/GroupMember 删除 | 查询测试使用固定状态数据验证 SELECT | 通过真实生产删除方法写入后，查询和同步行为一致 |

## 6. 已完成进度与验证

以下日期记录对应当时的代码和环境。当前有效范围还需结合 2026-10-09 的撤回记录；历史 MySQL 数据、容器状态和测试结果不代表 2026-10-13 已重新检查或执行。

### 2026-10-02：ClusterEntity 自身编号修正

- BaseSyncEntity 改为继承 BaseOrganizationEntity，保留同步属性及 clusterType；RuntimeEntity 独立保留所属 clusterId。ClusterEntity 不再继承 clusterId。
- 选中集群的请求现已统一为 DTO.id，同名映射到 Entity.id（见下方请求契约更新）；修正 QueryClusterInSyncDO 的返回对象、主集群拓扑包装、健康注销、部署子节点归属、同步结果及集群采集编号读取。
- 关系组装按 Cluster.id 建键、Runtime.clusterId 分组，补入主集群，并按关系两端组装父子列表。
- ClusterMetadata.nodeUnique 和集群同步查询请求使用 id；当时尚未完成 Metadata 继承拆分，Cluster/Runtime 部分已在下述 2026-10-08 工作中完成。
- 新增 ClusterIdentityQueryTest，使用隔离 H2 数据和真实 MyBatis Mapper、Service、Domain，覆盖 DTO → 查询 → Metadata → 主集群拓扑、关联查询、Runtime 分组及不存在编号查询。
- Maven profile `cluster-identity-query-tests` 显式启用测试源码编译及 JUnit 4 引擎，并使测试失败导致构建失败。新增 4 个查询测试与原有 6 个 NameMappingTest 均通过，零失败、零错误、零跳过；编译和 Checkstyle 通过。
- 验证范围为本地 H2 查询与对象组装；未验证实际 MySQL、HTTP 启动、真实中间件注册和定时同步，未执行业务库写入。前端不在本次范围。

### 2026-10-02：Docker MySQL 实际查询验收

- 启动已有 `eventmesh-dashboard-mysql`（MySQL 8.0.36，127.0.0.1:3306），连接已有 eventmesh_dashboard 数据库，容器保持运行。
- 新增 ClusterIdentityMysqlQueryTest，通过只读事务使用真实 Mapper、Service 和 Domain；不创建表、不插入、不更新业务数据。
- 13 个现有有效集群通过 DTO → Service → MyBatis → Entity → Metadata 往返及自身 id 校验。
- 根集群 16 的 7 条关系、23 个节点通过关系两端、父子列表、Runtime 按类型和所属集群分组、主集群拓扑验证；结果与独立 SELECT 查询比对一致。
- 不存在集群编号查询返回空。3 个测试全部通过，零失败、零错误、零跳过；编译、Checkstyle 和 git diff --check 通过。
- 运行使用 `cluster-identity-query-tests` profile 与 `-Dtest=ClusterIdentityMysqlQueryTest`，连接和根编号由 CLUSTER_QUERY_MYSQL_URL、CLUSTER_QUERY_MYSQL_USER、CLUSTER_QUERY_MYSQL_PASSWORD、CLUSTER_QUERY_ROOT_ID 环境变量提供。
- 本次完成 MySQL 查询链路验证；HTTP 应用启动、实际中间件注册和定时同步仍未验证。

### 2026-10-02：集群自身请求统一 id

- 新增 ClusterRequestDTO，以必填 id 表示集群自身编号；QueryRelationClusterByClusterIdAndTypeDTO 使用该请求基类，集群详情请求也使用 ClusterRequestDTO。
- ClusterControllerMapper 的关联查询和 toClusterEntity 使用同名 id 自动映射，移除两处 clusterId → id 注解。
- ClusterIdDTO 继续用于资源所属集群的引用，例如 Topic 查询和 Runtime 创建/查询；内部通过该引用查找 ClusterEntity 时明确赋给 Entity.id。
- 请求示例：关联查询、集群详情使用 `{"id":16}`；查询所属集群的 Runtime 仍使用 `{"clusterId":20}`。本次未统一其他查询接口（例如集群树）的请求字段。
- 5 个 H2 查询测试、3 个真实 MySQL 只读查询测试、6 个名称映射测试全部通过，零失败/错误/跳过；编译和 Checkstyle 通过。MySQL 仍覆盖 13 个集群以及根集群 16 的 7 条关系、23 个节点。

### 2026-10-08：Cluster/Runtime Metadata 身份与继承整理

- 提交 `9c51ec2`：BaseSyncBase 改为直接继承 BaseOrganizationBase；ClusterMetadata 移除继承的 clusterId 及重复 clusterType/trusteeshipType。RuntimeMetadata 独立保留父级 clusterId，storageClusterId 和节点身份不变。
- 状态判断方法上移至 BaseOrganizationBase，供拆分后的继承分支复用；保留原判断语义，不是状态规则修复。
- 联动调整 remoting 目标作用域、Topic 回调中的所属集群编号、采集缓存身份及健康结果/注销身份。
- 增加 ClusterMetadataIdentityTest 的 9 个测试，覆盖字段反射、Jackson、MapStruct 往返、身份方法、同步查询、remoting 作用域、Topic 归属、采集和健康边界。
- 历史验收共 24 项通过：4 项 MySQL、9 项 Metadata、5 项 H2 查询、6 项名称映射，零失败/错误/跳过。当时真实 MySQL 覆盖 13 个有效集群、41 条 Runtime，以及根集群 16 的关系和节点组装。
- 这些结果证明查询、转换与身份边界，未证明完整 HTTP 应用、定时同步及真实 SDK/RPC 生命周期。

### 2026-10-09：范围收敛为查询，撤回状态执行与生命周期改动

- `8f91562` 曾提交状态及增量链路改动；随后 `8af2daf` 按要求撤回状态执行和生命周期增强。不能把前一阶段的实现或测试成绩计入当前完成状态。
- 相对 `9c51ec2`，当前保留的该阶段生产变更为：七个 Mapper 的 SELECT 调整、ClusterAndRuntimeDomainImpl 查询组装、AbstractDBDataMetadataHandler 读取游标处理。Mapper 的 INSERT/UPDATE/DELETE 保留原有实现。
- 普通查询增加逻辑删除过滤，Topic/Config 兼容旧 status=0 删除方式；增量查询保留终止状态，使用 update_time >=。查询组装处理不存在的根、空关系集合及缺失关系端点。
- DB 元数据查询只有成功返回后才前移秒级游标；这不等于远程应用成功确认。状态判断、运行时解绑、SDK 关闭及重试保障仍未统一。
- 相关状态/生命周期/SDK/远程写入测试已移除；保留的 BaseIdDatabaseTest 通过测试夹具直接准备数据，只验证 SELECT，不能证明生产删除方法正确。
- 当时收敛后的回归记录为 38 项执行、34 项通过、4 项可选 MySQL 测试跳过；独立 MySQL 查询验收 12 项通过。更早的完整状态 CRAP/变异测试结果不认证当前已缩减的范围。

### 2026-10-13：合并上游并复核当前进度

- 合并提交 `616d9d0` 纳入 dev-maven-release 的 `5f4c737`，保留现有身份和名称规则；适配新增 Kafka 实现及测试中的 TopicMetadata/ConfigMetadata 名称访问方法。
- 保留上游 ConfigMetadata 的目标作用域身份：有 instanceType/instanceName 时使用组合键，否则回退为 name。instanceName 未持久化的限制保持不变。
- 整合并保留双方 Maven 测试 profile，修正重复 profiles 容器；该合并未恢复 10 月 9 日撤回的状态执行与生命周期增强。
- 当前同一代码树已验证的本计划相关测试如下。20 项身份/名称测试来自本次合并验证，14 项查询测试来自随后的进度审查；合计 34 项通过，零失败、错误和跳过。

| 测试类 | 数量 | 证明范围 |
| --- | --- | --- |
| ClusterIdentityQueryTest | 5 | H2/MyBatis 查询、请求编号与关系组装 |
| ClusterMetadataIdentityTest | 9 | 继承、转换和下游身份边界 |
| NameMappingTest | 6 | 名称映射、JSON 与原 SQL 兼容桥接 |
| BaseIdDatabaseTest | 4 | H2 中 Topic/Config/Group/GroupMember 的 SELECT 可见性 |
| QueryStateContractTest | 9 | 缺失根、空关系、隐藏端点与查询结果组装 |
| BaseIdRuntimeContractTest | 1 | DB 读取失败保留游标及重试 |

- 验证使用与当前 HEAD 完全一致的隔离源码副本、JDK 17.0.20 和 Maven 3.9.16；编译及 Checkstyle 通过。本次文档编辑复用上述结果，没有再次运行测试。
- 合并另有 79 项 Kafka 测试通过、28 项 RocketMQ 采集测试通过、2 项可选采集集成测试跳过。这些结果属于合并验证，不扩大本计划的状态重构验收范围。
- 本次未重跑真实 MySQL、Broker/IoTDB、完整 HTTP 应用或定时同步。前端未验收；关联查询的类型角色也没有专门覆盖。
- 详细执行命令、日志位置和协作记录保存在本地根 AGENTS.md。当前计划文档仍为未跟踪文件，本次仅更新文档，未提交或推送。

本次核查的关键源码入口：

| 内容 | 源码 |
| --- | --- |
| Metadata 继承与配置身份 | [BaseSyncBase](../eventmesh-dashboard-common/src/main/java/org/apache/eventmesh/dashboard/common/model/base/BaseSyncBase.java)、[ConfigMetadata](../eventmesh-dashboard-common/src/main/java/org/apache/eventmesh/dashboard/common/model/metadata/ConfigMetadata.java) |
| Runtime 增量查询 | [RuntimeMapper](../eventmesh-dashboard-console/src/main/java/org/apache/eventmesh/dashboard/console/mapper/cluster/RuntimeMapper.java) |
| 数据库读取游标 | [AbstractDBDataMetadataHandler](../eventmesh-dashboard-console/src/main/java/org/apache/eventmesh/dashboard/console/service/metadata/AbstractDBDataMetadataHandler.java) |
| 关系及 Runtime 状态执行 | [ClusterMetadataDomain](../eventmesh-dashboard-console/src/main/java/org/apache/eventmesh/dashboard/console/domain/metadata/ClusterMetadataDomain.java) |
| 删除及新增分类 | [BaseOrganizationBase](../eventmesh-dashboard-common/src/main/java/org/apache/eventmesh/dashboard/common/model/base/BaseOrganizationBase.java)、[BodyDataDifference](../eventmesh-dashboard-core/src/main/java/org/apache/eventmesh/dashboard/core/metadata/difference/BodyDataDifference.java) |
| 健康展示与 SDK 移除 | [RuntimeController](../eventmesh-dashboard-console/src/main/java/org/apache/eventmesh/dashboard/console/controller/cluster/RuntimeController.java)、[SDKManage](../eventmesh-dashboard-core/src/main/java/org/apache/eventmesh/dashboard/core/function/SDK/SDKManage.java) |

### 2026-10-13：Acl、Connection、NetConnection Entity 重复字段清理

- AclEntity 删除重复 `id`，使用 BaseIdEntity 的字段与访问方法；保留本类独有的 `clusterId`。ConnectionEntity、NetConnectionEntity 删除重复 `clusterId/runtimeId`，分别复用 BaseClusterIdEntity、BaseRuntimeIdEntity 的存储。
- 保留三类原有继承关系及 `@EqualsAndHashCode(callSuper = true)`。ConnectionEntity 的 `getClusterId()` 委托父类，并承载原字段上的 Schema 注解，保留 `runtime cluster id` 描述。未调整生产 SQL、表结构、Metadata、状态逻辑或 SDK 生命周期。
- 新增 EntityIdentityInheritanceTest（5 项）和 EntityIdentityMappingTest（4 项），覆盖字段唯一性、空值及非空值访问、Jackson、MyBatis 属性发现、相等性及身份哈希样例、Schema 注解，以及 NetConnection 现有 MapStruct 的身份往返。哈希样例验证指定身份值参与计算，不表示不同对象永远没有哈希碰撞。
- 数据库测试在 H2 中使用现有 Acl/Connection Mapper 查询，并执行 Acl 插入及主键回填。原 Acl SQL 的单数 `VALUE` 不被 H2 接受，首轮因此失败；测试专用拦截器仅将该语法适配为 `VALUES` 后复测通过，生产 SQL 未改动。Connection 插入只验证现有 SQL 的参数绑定；NetConnectionMapper 仍无可执行 SQL 映射，未验证其数据库链路。未运行真实 MySQL、HTTP 或定时同步。
- 最终冻结版本的 **24 项测试全部通过**（上述新增 9 项、NameMappingTest 6 项、ClusterMetadataIdentityTest 9 项），零失败、错误和跳过；Maven 编译、Checkstyle 和差异检查通过。
- 使用官方 JaCoCo 0.8.13 的方法复杂度及行覆盖计算 CRAP，保留三类全部 62 个声明方法。Connection 的委托 getter CRAP 为 1；Acl/Connection/NetConnection 的生成 equals 分别为 29/29/26，hashCode 为 9/9/8。生成 equals 的代码集中在一行，行覆盖不等于分支完整覆盖；父类方法不重复计入子类，未设定虚构的通过阈值。
- PIT 1.17.4 使用相同三类与默认算子：首次 224 个变异为 91 杀死、77 幸存、56 未覆盖；补充五组身份哈希样例后，最终为 **97 杀死、71 幸存、56 未覆盖**，无超时或运行错误。新增断言杀死 6 个哈希变异；Connection 委托 getter 的返回值变异也被测试杀死。
- 剩余结果保留原标签：46 个生成 hashCode 的算术/条件变异、3 个 canEqual 变异和 22 个非目标业务字段 getter 变异幸存；53 个生成 equals 分支及 3 个 toString 变异未覆盖。它们未验证的部分属于本次身份去重以外的哈希混合、扩展子类和业务字段行为，不作为已杀死或等价变异计数。代码及字节码复核确认三类 equals/hashCode 均调用父类实现；本次不宣称全类变异覆盖完成。
- 执行命令、失败留档、快照与问题处置见本地根 AGENTS.md 和 `eventmesh-dashboard-console/target/dev-crew/three-entity-identity-20261013/manifest.json`。改动仍在工作区，未提交或推送。

### 2026-10-13：Acl、Connection 数据库与 HTTP 链路验收（修复前）

- 按后续要求使用真实 MySQL 8.0.36 和随机本机端口的 Tomcat/DispatcherServlet 验证 Acl、Connection。使用真实 Controller、Service 和 MyBatis Mapper，没有用 Mock 补齐缺失的业务实现；测试只装配相关组件，未启动完整 Dashboard 的后台任务或 Broker 链路。
- 新增 AclMysqlChainTest、ConnectionMysqlChainTest 及共享隔离夹具。最终 **34 项执行：30 通过、4 失败，零错误和跳过**。其中新增 10 项为 6 通过、4 失败，原 24 项字段与转换回归全部通过。本轮不能标记为全链路验收通过。

| 验证范围 | 实际结果 |
| --- | --- |
| Acl 原始 Mapper SQL → MySQL | 2 项通过：生成 id 回填、字段读回及不同集群记录区分；临时表使用仓库原始 Acl DDL，未转换生产 SQL |
| Acl Controller → Service → Mapper → MySQL，直接调用已注入的真实组件 | 1 项通过：插入、查询、更新 resourceType、删除写 status=0，均核对数据库；此项跳过 HTTP 编解码 |
| Acl HTTP 新增、查询 | 2 项失败：请求反序列化时返回 500，未进入 Controller 方法体 |
| Connection 原始 Mapper SQL → MySQL | 3 项通过：单条插入及主键、批量插入、clusterId/runtimeId 读写与集群/状态过滤；使用测试专用表 |
| Connection HTTP 创建、查询 | 2 项失败：创建在反序列化阶段返回 500；查询进入 Controller 后因 Service 为空返回 500 |

本轮定位的阻塞如下，均保留失败断言和原始日志：

1. **Jackson 依赖版本冲突**：当前 `jackson-datatype-jsr310` 为 2.17.1，`jackson-databind/core/annotations` 为 2.13.5。Acl 新增/查询及 Connection 创建的真实 HTTP 请求触发 `NoSuchFieldError: READ_DATE_TIMESTAMPS_AS_NANOSECONDS`，因此无法证明这些 HTTP 操作成功。
2. **Connection Service 链路缺失**：ConnectionDataService 未找到实现，Controller 的对应字段也没有注入。查询请求实际触发空指针；未通过手动注入假 Service 绕过。
3. **Connection 建表缺失**：仓库生产建表脚本和当前本地业务库都没有 connection 表。本轮独立测试表只包含 Mapper 所需字段及自增主键，不验证生产建表或业务唯一键、重复键更新语义。
4. **Acl 查询返回契约缺口**：源码中的 selectAcl 返回 void，丢弃 Service 查询结果。本轮 HTTP 更早被 Jackson 拦截，因此“成功查询后返回空响应”仅为源码结论，未冒充运行结果。

- 所有测试数据写入唯一的 `em_entity_chain_*` 临时库，执行后已全部删除；原业务库 Acl 行数前后均为 0。MySQL 容器恢复到测试前停止状态。未修改生产 Java、依赖配置、SQL 或 Docker 文件，本轮只新增测试和记录。
- 原始业务基线存在失败，按 dev-crew 规则未进入本轮 CRAP/PIT。JaCoCo 已生成执行数据，但 Maven 在测试失败后未执行 Console XML 报告目标；不以旧字段级质量报告证明此次链路通过。
- 详细命令、失败原因与报告索引在 `eventmesh-dashboard-console/target/dev-crew/acl-connection-chain-20261013/manifest.json`，最终日志为该目录下的 `logs/unit-crap/baseline-4.log`。


### 2026-10-13：修复 ACL/Connection HTTP 链路，保留现有表结构

- 已移除 Console 对 `jackson-datatype-jsr310:2.17.1` 的单独指定，实际测试运行时的 Jackson 组件均由 Spring Boot BOM 统一为 2.13.5；原反序列化 `NoSuchFieldError` 不再出现。`AclController.selectAcl` 现在返回查询实体，真实 HTTP 返回字段已逐项核对。
- 新增 [ConnectionDataServiceImpl](../eventmesh-dashboard-console/src/main/java/org/apache/eventmesh/dashboard/console/service/cluster/impl/ConnectionDataServiceImpl.java)，通过真实 Mapper 实现创建、列表和详情查询；Controller 使用构造器注入。创建失败不再返回成功，参数校验失败返回 400，数据库异常返回 500。
- 按用户确认的规则，`CreateConnectionDTO` 增加必填 `runtimeId`：节点必须有效且属于请求 `clusterId`。Source/Sink 只复用同集群中名称、类名、地址、端口与角色匹配唯一的现有 Connector；缺失、多条匹配、停用和跨集群候选均拒绝。只写入 Connection，保留 `groupId` 与连接描述；不新建或修改 Connector。插入方法返回回填后的实体 ID，失败传播并参与 Spring 事务。
- 复用模式接受空或 null 的配置列表，携带配置变更时明确返回 400。配置模板查询仍未实现：现有模型缺少 Connector 类型及模板关联信息，接口明确返回 501。此项和正式 Connection 表结构继续保留为后续事项，不能将本轮描述为全部 Connection 功能或完整应用启动验收。
- 最终 **60 项测试全部通过，零失败、错误和跳过**：ConnectionServiceTest 15、ControllerFailureTest 7、AclMysqlChainTest 5、ConnectionMysqlChainTest 9，以及原有身份/名称/MapStruct 回归 24。真实 MySQL 用例验证 HTTP 成功响应、实际落库与读取、节点/Connector 校验、集群和 Topic 过滤、错误响应、Connector 不被修改，以及 Service 插入参与事务回滚。编译与 Checkstyle 通过。
- 官方 JaCoCo 0.8.13 显示新增 Service 行覆盖 98/98、分支覆盖 58/58。按方法复杂度与行覆盖计算的 CRAP 包含 29 个目标方法（含 lambda）；最高为列表组装方法的 8。空 Connector ID 的分支补测后由 7/8 提升至 8/8；ConnectionController 未变更的 `showCreateConnectionMessage` 仍未覆盖，不宣称全类覆盖。
- PIT 1.17.4 使用相同三个 Controller/Service 类及 DEFAULTS 算子，测试目标限于两个确定性单测类，真实 HTTP/MySQL 另由上述基线验证。首次 102 个变异为 92 杀死、6 幸存、4 未覆盖；补充空值、同值无效集群编号及逐项 Connector 匹配断言后，最终为 **98 杀死、0 幸存、4 未覆盖**，零超时或工具错误。4 个未覆盖变异分别来自未修改的 Acl 插入/更新/删除方法和 Connection 空创建表单方法，保留原始标签；不计为杀死或等价。
- 未修改生产 SQL、建表、迁移或 Docker 文件。Connection 仍使用隔离测试表；Connector 原建表片段含无效的独立 `x`，测试同样使用明确标注的专用表。Acl 与 Runtime 使用原始 DDL。测试不能证明生产 Connection/Connector 表结构、唯一键/upsert 语义、后台调度或 Broker 链路可用。
- 证据位于 `eventmesh-dashboard-console/target/dev-crew/acl-connection-fix-20261013/manifest.json`；最终基线日志为 `logs/unit-crap/baseline-3.log`，变异日志为 `logs/behavior-mutation/pit-final.log`。原始失败记录与首次质量结果保留。最终源码快照及冻结边界一次纯换行修正的核对依据见 manifest；业务行为未随该格式修正变化。


### 2026-10-17：InstanceUser、Connector、Log Entity 重复身份字段清理

- InstanceUserEntity、ConnectorEntity 删除重复 `clusterId`；LogEntity 删除重复 `id/clusterId`，使用 BaseIdEntity/BaseClusterIdEntity 的单份字段。保留原继承关系、业务字段及 `@EqualsAndHashCode(callSuper = true)`。
- LogEntity 保留无参和原九参数构造器。用显式九参数构造器替换 Lombok 的 `@AllArgsConstructor`，其中 `id/clusterId` 通过父类 setter 写入，避免删字段时连带改变原构造器签名。
- 新增 RemainingEntityIdentityTest（5 项）、RemainingEntityMappingTest（2 项），验证字段唯一性、父类实际存储、null/非 null、JavaBean/Jackson/MyBatis 属性发现、身份比较与固定哈希样例、Log 构造器，以及 Connector/操作日志的 Mapper 映射。哈希样例仅验证所选身份值参与计算，不承诺不同对象永不碰撞。
- 最终 **53 项测试全部通过，零失败、错误和跳过**：新增 7 项、原身份/名称/MapStruct 回归 24 项、ConnectionServiceTest 15 项、ControllerFailureTest 7 项。Maven 编译、Checkstyle 与差异检查通过。
- Mapper 测试使用真实 MyBatis 和隔离 H2 内存表：验证 Connector 按 clusterId 查询及插入参数绑定；验证操作日志按集群查询、自增 id 回填、按继承 id 更新与读回。OprLogMapper 原始 MySQL `VALUE` 语法仅在测试拦截器内适配为 H2 支持的 `VALUES`，生产 SQL 未改。Connector 的 MySQL upsert 未实际执行；InstanceUser 尚无可供验证的现有 Mapper/转换器，不虚构数据库或 MapStruct 链路。
- 首轮 53 项中 2 失败、49 错误，多个未修改类因已有编译输出中的无效构造器字节码触发 VerifyError；Maven 增量编译跳过了这些类。保留失败日志与字节码样本后，仅清理相关四个模块的 classes/编译缓存并重新编译，同一源码快照即通过 53 项。随后补充 Log 不同 id 的不相等断言，最终快照再次通过同范围测试。未清空 target 或删除旧任务报告。
- 官方 JaCoCo 0.8.13 结合方法级复杂度和行覆盖计算 CRAP，共保留三类全部 53 个声明方法。InstanceUser/Connector/Log 的最大 CRAP 分别为 17/26/26，均来自 Lombok 生成的 equals；Log 九参数构造器复杂度及 CRAP 均为 1，11/11 行覆盖。生成方法共享注解行，不能用行覆盖声称所有分支已验证；未设置虚构的百分比门槛。
- PIT 1.17.4 对三类完整实体使用 DEFAULTS 算子，最终 **182 个变异：47 杀死、89 幸存、46 未覆盖**，零超时或运行错误。Log 构造器中删除 `setId/setClusterId` 的两个变异均被测试杀死；父类身份比较失败分支相关变异也已检验。补充 Log id 断言后计数未改变，不宣称额外杀死变异。
- 幸存项包括 43 个 hashCode 混合算术/条件、33 个生成 equals 条件、10 个未改业务字段 getter、3 个 canEqual；未覆盖项为 43 个生成 equals 分支和 3 个 toString。它们保留工具原始标签，按本次身份存储/构造器范围记录，未算作杀死或等价，不宣称全类变异覆盖完成。
- 未修改生产 SQL、表结构、Metadata、状态生命周期或前端；未运行真实 MySQL、HTTP、后台调度或 Broker 验收。证据位于 `eventmesh-dashboard-console/target/dev-crew/remaining-entity-identity-20261017/manifest.json`，最终日志为 `logs/unit-crap/baseline-3.log` 和 `logs/behavior-mutation/pit-final.log`。改动保留在工作区，未提交或推送。

### 2026-10-17：Acl、Connection、ClusterRelationship Metadata 重复字段清理

- AclMetadata 删除重复 `clusterId`；ConnectionMetadata 删除重复 `runtimeId`；ClusterRelationshipMetadata 删除重复 `clusterType`。使用既有父类的单份存储及访问方法，三个类的继承结构、业务字段、`nodeUnique()` 方法体和 Lombok `callSuper` 设置保留。
- 关系元数据的 `relationshipId/relationshipType` 表示另一端，独立保留；继承的 `runtimeId` 没有发生重复声明，本轮保留其公开契约。当前没有足够的生产调用链依据支持同时移除这一作用域，故不将去重扩展为关系模型重设计。
- 新增 MetadataFieldIdentityTest（7 项）、MetadataFieldBehaviorTest（4 项），验证父类实际存储、层级字段唯一性、null/非 null、JavaBean/MyBatis 属性发现、JSON 读写、Object 身份约束下的 equals/hashCode、业务字段输出及 RocketMQ ACL 子类继承。覆盖 ACL legacy 与 ACL2 的身份规则、null/空串编码边界、Connection 身份和关系两端独立性。
- 最终 **64 项测试全部通过，零失败、错误和跳过**：新增 11 项，加上原有身份/名称/MapStruct、H2 Mapper、Connection Service/Controller 回归 53 项。Maven 编译、Checkstyle 和差异检查通过。
- 首轮遇到已有编译缓存中的无效构造器字节码（63 项中 6 失败、27 错误）；保存失败记录后仅清理四个相关模块的 classes/编译缓存，同一冻结源码重新编译即通过 63 项。随后根据 CRAP 报告补充哈希、字符串输出和 equals 边界测试，最终重新冻结并通过 64 项。没有用失败轮覆盖数据计算最终质量指标，也未删除旧任务产物。
- 官方 JaCoCo 0.8.13 在 Console 测试结束后生成 Common 报告，CRAP 保留三个类全部 69 个声明方法。补测使 Acl/Connection/ClusterRelationship 的 hashCode CRAP 分别由 132/210/12 降至 11/14/3，分支覆盖分别达到 20/20、26/26、4/4。三个 `nodeUnique()` 及 Acl 的编码辅助方法均覆盖全部行和分支。生成 equals 的 CRAP 仍为 35/44/11；共享注解行的行覆盖不代表所有分支可达。
- PIT 1.17.4 官方 CLI 显式使用当前 Common 编译目录作为变异代码路径，使用 Console 的两个 MetadataField 测试类和 DEFAULTS 算子。最终 **245 个变异：68 杀死、71 幸存、106 未覆盖**，零超时或运行错误；三个 `nodeUnique()` 及 Acl 编码辅助方法的 9 个变异全部被杀死。
- 遗留变异保持原始分类：幸存项包括 53 个生成 hashCode 混合/返回变异、6 个 canEqual、3 个 equals 条件和 9 个未改业务 getter。106 个未覆盖项均在生成 equals 中；现有父类使用 Object 身份比较，同对象提前返回、不同对象在父类比较处返回，因此后续字段比较不可达。本轮验证去重存储与现有身份契约，不重写该相等策略，也不承诺生成哈希的具体数值或全类变异覆盖。
- 本轮只改变上述三个 Metadata 的字段声明，没有新增持久化链路或修改生产 SQL、表结构、前端、状态生命周期。新增测试使用真实 POJO、JSON 和反射属性发现，不冒充 HTTP/数据库/Broker 全链路验收。证据位于 `eventmesh-dashboard-console/target/dev-crew/metadata-field-dedup-20261017/manifest.json`，日志为 `logs/unit-crap/baseline-3.log`、`logs/behavior-mutation/pitest-cli-run-1.log`；改动未提交或推送。


### 2026-10-17：补齐三类 Metadata 转换、注册及同步链路

- 按用户确认范围实现：Kafka ACL 数据库持久化同步、Connection 数据库到 Console 内存、集群关系数据库到 ColonyDO 拓扑。新增三组 Converter、数据库 Handler、注册项和同步目标，复用 MetadataSyncManage 执行。RocketMQ ACL 独立 RPC 保留，未添加 Connector 远端部署协议。
- 现有表结构保持不变；新增三个 Mapper 的快照 SELECT。状态、审计、身份字段在共有模型间转换；Connection 保留 endTime；关系解除 status=3 不再被当作建立关系。先转换/校验后应用，异常保留给下一轮重试；FunctionManage 的拓扑游标在应用成功后推进。
- 新增 MetadataPipelineContractTest 18 项、MetadataPipelineRegressionTest 15 项、MetadataPipelineMysqlTest 3 项。结合原有身份、查询、服务和 HTTP 回归，最终 **133 项全部通过，零失败、错误、跳过**；Maven 编译和 Checkstyle 通过。额外覆盖已配置 TOPIC 仍能注册原有远程同步 Wrapper，避免新筛选逻辑影响旧类型。
- 真实链路验证使用实际 HTTP 端口、Controller/Service、MyBatis、MySQL、转换/注册和 MetadataSyncManage.init/run/close。ACL 通过默认 SDK/Remoting 到真实 Kafka，验证创建、幂等、删除及无关 ACL 保留；Connection 验证创建、节点及 Connector 关联到内存配置和终止移除；关系通过真实建立/解除接口验证绑定、解绑及 superiorId 清理。
- 边界：ACL、关系使用仓库原 DDL；Connection/Connector 使用测试表，因为仓库缺少完整 Connection DDL，Connector DDL 也有既有异常。通用结果审计 Handler 为 Mock；集成测试显式调用管理器单轮执行，没有启动完整应用或验证长期后台定时运行。未覆盖远端 ACL 修改反向入库，也无法在重启后恢复已被数据库覆盖的旧 ACL 历史绑定。
- 官方 JaCoCo Maven 插件 0.8.13 采集并生成当前版本 XML，以方法 COMPLEXITY 和 LINE 计算 56 个业务方法的 CRAP。三个新同步目标的 synchronize 均为 100% 行覆盖，CRAP 分别为 Kafka 9、关系 8、Connection 6。SyncMetadataCreateFactory.loadData 最高为 11.764，其统计包含未改动的旧增量路径；不据此宣称整个类全覆盖，也未设虚构的百分比门槛。
- 变异检查发现的共有字段往返、非默认状态、删除记录首轮处理、重复 ID、查询作用域、关系冲突/循环、注册筛选和失败结果等断言缺口已补测。过程中测试构造、泛型、无序 Map、不可变 Map、异常包装层级的问题均记录并修复；只使用最终成功轮次的覆盖报告，不以失败用例冒充杀死变异。
- 最终冻结源码/测试/配置共 886 个文件，SHA256：`4a9e014522e87dbb311519acdc500c41cd478464463ee16c4e1953f6e1e37c4e`。证据位于 `eventmesh-dashboard-console/target/dev-crew/metadata-pipeline-20261017/manifest.json`；测试日志 `logs/unit-crap/jacoco-unit-run-10.log`，覆盖数据 `baseline10/coverage`，CRAP `crap/baseline10-crap.json`。启用方式及数据边界见 3.5；改动保留在工作区，未提交或推送。

- 最终 PIT 1.17.4 使用 DEFAULTS、两个线程，对同一冻结输入的 Console/Core 原定范围生成 **468 个变异：260 杀死、40 幸存、163 未覆盖、5 超时**，进程退出码 0。三个 Converter、三个数据库来源、Connection 目标没有遗留幸存项；关系移除循环保护会导致无限遍历，按工具原样记录超时。Kafka 唯一目标幸存项是前置非 Kafka 过滤，后续集群循环仍重复检查，复核为有效输入域内行为等价，原始标签不改。
- 其他 39 个幸存项、162 个未覆盖项主要属于原有拓扑组装、通用远程请求/配置、生成访问方法和反向写入/审计路径，按范围外记录。Kafka 默认服务解析器在变异单测中有 1 个未覆盖项，但真实 KRaft 集成已执行；不将两类证据混为变异杀死。5 个超时分别来自 4 个管理器线程/执行/计数器变异和 1 个关系循环保护变异，未计入杀死。工具文本汇总把超时计入 killed，本文采用 XML 原始分类。
- 原始报告：`mutation/final-baseline10/mutations.xml`，日志：`logs/behavior-mutation/pitest-final-baseline10.log`。跨模块可变代码目录匹配的失败和仅 Console 的诊断报告单独保留，不作为最终结果；修正工具路径后确认最终包含 Core 139 个、Console 329 个变异。早期 Connection 测试曾错误依赖 Map 顺序，其旧杀死数量不作为质量改善依据。
- 已确认隔离 MySQL 测试库和测试 Kafka ACL 清理完成，仅将本轮启动的 MySQL/Kafka 两个测试容器恢复为停止状态；其他容器与 Docker 引擎未关闭。没有暂存、提交或推送代码。
