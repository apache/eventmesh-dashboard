# Entity、Console 与 Metadata 字段重构方案


## 1. 目标（STAR）

- **S｜背景**：`id/xxxId`、`name/xxxName`、`clusterType` 的混用与重复定义，使 Entity、Console、Metadata 对同一资源的身份和类型缺乏一致解释。Cluster、Runtime 的核心流程又依赖复杂的 status 判断，一处取值或判定错误，就会沿查询、注册、拓扑维护和同步链路传递，造成目标误定位、停用事件丢失、关系无法解绑或错误同步，直接破坏资源生命周期管理的正确性。
- **T｜目标**：自身编号统一为 `id`，自身名称统一为 `name`，保留关联字段；清理重复定义，分清业务状态与逻辑删除。
- **A｜行动**：先整理 Entity 和 Console，再联动调整 Metadata、字段映射及注册、同步、注销方法。
- **R｜预期结果**：建立贯穿 Entity、Console、Metadata 的统一身份与状态规则，消除核心流程中“操作哪个对象、应执行哪个动作”的歧义。让 Cluster、Runtime 的状态变更完整驱动注册、同步、注销和解绑，确保数据库记录、内存拓扑与操作目标保持一致，为后续功能扩展提供可靠的资源生命周期基础。

## 2. 涉及的类及字段

下表列出已完成和计划中的字段改动，以及对应的处理要求。Entity 与 Metadata 是平行模型层，两边出现同名字段不构成继承重复。仅同一对象的父子继承链中存在同语义的多份存储，才属于字段遮蔽问题。

### 2.1 Entity

| 类 | 字段与处理要求 |
| --- | --- |
| TopicEntity | `topicName` → `name`；保留旧访问方法适配原 SQL（已完成） |
| ConfigEntity | `configName` → `name`；保留旧访问方法适配原 SQL（已完成）；`instanceId/instanceType` 保持关联目标语义 |
| RuntimeEntity | 删除重复 `name`，使用父类字段（已完成）；自身 `id` 与所属 `clusterId` 必须同时保留 |
| ClusterEntity | 自身使用 `id`；审查继承的 `clusterId`，不能为适配旧调用者把自身 id 复制到两份字段作为最终方案 |
| BaseIdEntity | `status`、`isDelete`：明确业务状态与删除标记职责；逐表核对对应列及默认值 |
| BaseSyncEntity | 拆清同步公共属性与集群归属，避免 Cluster 被迫继承自指 `clusterId`；保留 Runtime 的父级引用 |
| AclEntity | `id`：删除重复声明，使用父类字段；其 `clusterId` 不是与 BaseIdEntity 的重复字段 |
| ConnectionEntity | `clusterId`、`runtimeId`：删除重复声明，使用父类字段 |
| NetConnectionEntity | `clusterId`、`runtimeId`：删除重复声明，使用父类字段 |
| InstanceUserEntity | `clusterId`：删除重复声明，使用父类字段 |
| ConnectorEntity | `clusterId`：删除重复声明，使用父类字段 |
| LogEntity | `id`、`clusterId`：删除重复声明，使用父类字段 |

### 2.2 DTO

| 类 | 字段与处理要求 |
| --- | --- |
| CreateTopicDTO | `topicName` → `name`（已完成） |
| GetTopicListDTO | `topicName` → `name`（已完成） |
| ChangeConfigDTO | `configName` → `name`（已完成） |
| GetConfigsListDTO | `configName` → `name`（已完成） |
| QueryByInstanceIdDTO | `configName` → `name`（已完成） |
| CreateClusterByFullAddressDTO | 自身待创建集群的 `clusterName` → `name`，核对接口调用者 |
| QueryClusterByOrganizationIdAndTypeDTO | 查询对象自身 `clusterName` → `name`，同步核对查询映射 |
| CreateClusterDTO、CreateClusterByFormDataDTO | 子类与 ClusterIdDTO 均声明 `clusterType`；确认创建目标与已选对象是否为同一角色，再去重或明确区分 |
| QueryRelationClusterByClusterIdAndTypeDTO | 子类与 ClusterIdDTO 均声明 `clusterType`；必须区分已选集群类型与关联查询目标类型，不能只删声明 |

### 2.3 Metadata

| 类 | 字段与处理要求 |
| --- | --- |
| TopicMetadata | `topicName` → `name`，同步调整 `nodeUnique()`（已完成） |
| ConfigMetadata | `configName` → `name`，同步调整 `nodeUnique()`（已完成） |
| BaseOrganizationBase | `status`、`isDelete`：与 Entity 状态/删除语义对齐；检查 `getUnique()` 的缓存与 id 生命周期 |
| BaseClusterIdBase | `isDelete/isInsert/isUpdate`：在状态规则确定后统一判断来源与分类优先级 |
| BaseSyncBase | 与 Entity 同批调整身份契约，但不机械复制继承结构；保留现有同步、注册及多态要求 |
| ClusterMetadata | 自身 `id`、继承的 `clusterId`、`nodeUnique()` 一起审查；`clusterType/trusteeshipType` 清理同语义重复声明 |
| RuntimeMetadata | 保留自身 `id`、父级 `clusterId` 与 `storageClusterId` 的区别；核对 `clusterUnique()` 与节点定位 |
| AclMetadata | `clusterId`：删除重复声明，使用父类字段 |
| ConnectionMetadata | `runtimeId`：删除重复声明，使用父类字段 |
| ClusterRelationshipMetadata | 清理同语义 `clusterType` 遮蔽；保留 `relationshipId/relationshipType`；审查继承 runtimeId 是否符合实际关系作用域 |
| InstanceUserMetadata | 自身 `userName` → `name`，保持现有身份来源 |
| QueueMetadata | 自身 `queueName` → `name`，补齐访问方法；关联 `topicName` 保留 |

## 3. 会影响的方法

### 3.1 ID 与集群查询

| 类 | 方法 | 影响内容 |
| --- | --- | --- |
| QueryClusterInSyncDO | `create()` | 将新建且设置了 id 的 ClusterEntity 放入返回对象，不再把原空引用赋回自身 |
| ClusterMetadataDomain | `setMainCluster()` | 主集群编号改取 ClusterEntity.id，并核对 ColonyDO、转换及后续注册 |
| ClusterControllerMapper | `toClusterEntity(ClusterIdDTO)`、`queryRelationClusterByClusterIdAndType()` | 选中集群的请求 clusterId 明确映射到 Entity.id；不能推广到创建映射或 Runtime 父级引用 |
| ClusterControllerMapper | `queryClusterByOrganizationIdAndType()` | clusterName 改为 name 后同步名称映射 |
| ClusterController | `createClusterByFullAddress()` | 接收 DTO 的名称字段变为 name |
| ClusterAndRuntimeDomainImpl.GetSyncObjectHandler | `structureRelationship()` | 两端 Cluster 使用自身 id；同时核对分组建键和查询键，Runtime 按父级 clusterId 归组而非自身 id |
| DefaultDataHandler | `unRegisterCluster()` | 健康注销当前读取 ClusterEntity.clusterId，需与注册、SDK 移除的身份一致 |
| SyncMetadataCreateFactory | 请求构造中的 Cluster/Runtime 分支 | Runtime 分支读取自身 id；Cluster 分支仍读取 clusterId，需同批对齐 |
| ClusterMetadata、RuntimeMetadata、BaseOrganizationBase | `nodeUnique()`、`clusterUnique()`、`getUnique()` | 分别定义目标内身份、所属集群身份及管理器缓存身份，禁止一律替换成 name 或 id |

### 3.2 名称读写与转换

| 类 | 方法 | 影响内容 |
| --- | --- | --- |
| TopicControllerMapper | `createTopic()`、`queryTopicListByClusterId()` | DTO.name → Entity.name |
| ConfigControllerMapper | `queryByInstanceId()` | DTO.name → Entity.name |
| TopicServiceImpl | `setSearchCriteria()` | 使用 getName() 构造查询条件 |
| TopicServiceImpl | `getTopicDetailGroups()`、`addTopic()` | 读取 Topic.name 填充关联对象的 topicName |
| ConfigServiceImpl | `logUpdateRuntimeConfigs()`、`logUpdateStoreConfigs()`、`logUpdateConnectorConfigs()`、`logUpdateTopicConfigs()` | 使用 ChangeConfigDTO.name 构造配置对象 |
| ConfigServiceImpl | `updateConfigsByInstanceId()` | 使用 name 作为配置键 |
| TopicConvertMetaData、ConfigConvertMetaData | `toEntity()`、`toMetaData()` | Entity 与 Metadata 同名复制 name |
| TopicMetadata、ConfigMetadata | `nodeUnique()` | 使用 name 识别资源 |
| RocketMQTopicRemotingService | `createTopic()`、`deleteTopic()`、`getAllTopics()` | 使用模型 getName/setName，第三方 SDK 名称不变 |
| RocketMQConfigRemotingService | `getConfig()`、`getAllConfigs()`、`addProperty()` | 配置名称读取、结果回填及配置键写入 |
| TopicEntity | `getTopicName()`、`setTopicName()` | 转接到 name，保留原 SQL 的参数与结果映射 |
| ConfigEntity | `getConfigName()`、`setConfigName()` | 转接到 name，保留原 SQL 的参数与结果映射 |
| InstanceUserMetadata | `getUserName()`、`setUserName()` | 改为 getName/setName，同步调整调用者 |
| QueueMetadata | 名称 getter/setter（需补充） | queueName 改为 name 后提供访问方法 |

### 3.3 重复字段清理

| 类 | 方法 | 影响内容 |
| --- | --- | --- |
| RuntimeEntity | `getName()`、`setName()` | 使用父类字段及访问方法 |
| AclEntity、LogEntity | `getId()`、`setId()` | 使用父类 id |
| ConnectionEntity、NetConnectionEntity、InstanceUserEntity、ConnectorEntity、LogEntity、AclMetadata | `getClusterId()`、`setClusterId()` | 使用父类 clusterId，关联含义不变 |
| ConnectionEntity、NetConnectionEntity、ConnectionMetadata | `getRuntimeId()`、`setRuntimeId()` | 使用父类 runtimeId |
| CreateClusterDTO、CreateClusterByFormDataDTO、QueryRelationClusterByClusterIdAndTypeDTO、ClusterMetadata、ClusterRelationshipMetadata | `getClusterType()`、`setClusterType()` | 确认父子字段角色相同后使用单一存储；不同角色必须明确区分 |
| ClusterMetadata | `getTrusteeshipType()`、`setTrusteeshipType()` | 使用父类 trusteeshipType |

去重还会改变 Lombok 生成的 `equals/hashCode/toString`：删除字段并不只是删掉一对访问方法。应核对父类参与方式、Jackson 属性发现、MyBatis 反射及重新生成的 MapStruct 实现，不直接编辑 target 下的生成代码。

### 3.4 删除与状态

| 类 | 方法 | 影响内容 |
| --- | --- | --- |
| BaseIdEntity、BaseOrganizationBase | `getStatus()`、`setStatus()`、`getIsDelete()`、`setIsDelete()` | 对齐状态与删除标记职责，部分方法由 Lombok 生成 |
| BaseClusterIdBase | `isDelete()`、`isInsert()`、`isUpdate()` | 统一删除判断来源，避免操作分类重叠 |
| GroupMapper | `deleteGroup()` | 当前写入 status=1，与有效查询相同，需修正删除语义 |
| ConfigMapper | `deleteConfig()`、`selectAll()`、`syncGet()` | 对齐删除写入、普通查询及同步查询的状态规则 |
| GroupMemberMapper | `deleteGroupMember()` | 对齐删除标记的写入方式 |
| TopicMapper | `queryTopicsToFrontByClusterId()`、`syncGet()` | 普通查询过滤与同步读取删除记录的规则需分别处理 |
| ClusterConvertMetaData、RuntimeConvertMetaData、TopicConvertMetaData、ConfigConvertMetaData、GroupConvertMetaData、GroupMemberConvertMetaData | `toEntity()`、`toMetaData()` | 保留 status/isDelete 和同步所需时间戳 |
| RuntimeMapper、ClusterMapper、ClusterRelationshipMapper | 停用/解除写入及增量查询 | 与 ClusterMetadataDomain 的移除、注销、解绑判定组成完整状态链 |
| BodyDataDifference | `doDifference()` | 删除、新增、更新分类互斥，不能让时间相等覆盖删除事实 |
| AbstractDBDataMetadataHandler | `getData()` | 查询异常不前移有效游标，明确加载与应用变更的确认边界 |
| RuntimeController | `queryRuntimeListByClusterId()` | 健康结果独立展示，不覆盖 Entity.status 的业务含义 |

状态相关方法按照统一的状态规则调整。

## 4. 字段使用规则

统一明确对象“是谁、叫什么、是什么类型、处于什么状态”，让业务代码与同步代码使用同一套规则。

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

### 5.1 已定位的源码矛盾

| 风险项 | 源码中的写入与处理矛盾 | 可能后果 |
| --- | --- | --- |
| Runtime 停用事件丢失 | 停用写入 `status=0`，增量查询却只读取 `status=1` | 停用记录无法通过该增量查询抵达处理端，内存注册及客户端注销可能未执行 |
| 集群关系解除识别不一致 | 关系解除写入 `status=3`，内存解绑分支只识别 `status=0` | 解除事件无法进入解绑分支，可能继续被按建立关系处理 |
| 删除记录被归类为新增 | 同步分类先判断新增；新增判断依赖创建时间与更新时间相等 | 已删除且两个时间相等的记录可能先命中新增分支，触发错误同步 |

### 5.2 本次重构需要消除的风险

- **身份误定位**：把 Cluster 自身 id 与关联 clusterId 混用，可能查询、注册或操作错误对象。
- **停用/删除事件丢失**：普通展示查询与变更查询共用有效记录过滤条件，可能在传播前过滤掉停用、删除或解绑记录。
- **状态相互覆盖**：业务状态、逻辑删除、健康结果及部署过程共用或覆盖同一字段，可能改变后续状态判断。
- **错误同步**：新增、更新、删除判定不互斥，或写入端与消费端状态定义不同，可能执行与原操作相反的动作。

本次重构的目的，是消除以上错误发生的条件。重点检查 Runtime、Cluster 从状态写入、增量加载到内存注册/注销、关系绑定/解绑及同步分类的完整处理链。

### 5.3 验证场景

- Runtime 停用后，增量查询能读取该变更，并完成注销。
- 集群关系解除后，内存拓扑完成解绑。
- 已删除且创建时间与更新时间相等的记录，只进入删除分支。
