## 文档任务

- 整体设计文档
- metadata 数据 设计文档
- 部署设计文档
- Cluster 设计文档
- 同步设计文档
- 心跳设计文档
- SDK 设计文档
- 远程服务设计文档
- 日志说明与使用设计文档
- 采集设计文档
- 采集数据库设计文档
- 模拟数据 同步测试文档
- RocketMQ 同步测试文档
- 部署测试文档
- 采集测试文档
- 增加一个 存储 整个开发流程
- 增加一个 meta 整个开发流程

## 问题

- 自动化部署之后，如何确定部署是否成功，部署测试如何做？
- group 与 group memer 是 cluster 级别还是 runtime 级别
    - 如果是 cluster 级别，rocketmq 需要支持 cluster 级别操作，如果不支持着需要全量删除
- group 与 group memer 只读，不允许创建
- config 问题
    - 不同的 存储 与 runtime 对不同的 metadata 有的支持 config ，有的不支持 config
        - 需要一个 config 管理模块
            - 对 runtime（包含 存储）的 metadata 进行标记
            - 支持 runtime（包含 存储） metadata 双维度
    - 不同的 存储 与 runtime 对 config 的修改不同
        - kakfa 支持一个一个 config 的修改
        - rocketmq 只能对 config 的 主体 进行修改。比如 修改 queue ，那么需要 对 Topic 全量修改
        - 通过 config 模块 对 config 的行为标记做出不同的处理
            - 修改 rocketmq 的 metadata 的 config ， 需要对 metadata 主体进行标准，然后触发 metadata 对象的 同步操作

## 任务 or issue

- SyncStatus 状态补充
- LogManage 记录模块
- 操作记录模块
    - 优点是：可以进行记录
- 部署与操作流模块
- 是否设计运维操作模块
- rometing service 入参 与 返回 讨论
- 手动添加 集群 与 节点 信息时的 url 设计
- kubernetes 部署 RocketMQ 的 IP 问题。client 调用 broker 是 kubernetes node ip，broker 调用 broker 是 kubernetes pod ip
- TODO 内容整理之后去除
- 文档与代码整理
- 讨论：手动添加集群与节点
- 分析：db 的负载。 维度：业务操作，统计操作，sync and 部署等操作
- 用户进来，是进入默认组织 首页

1. 通过左上角切换
2. 组织视图与 cluster 共存，这样是否会出现三级路由
3. 人员添加，可以从组织添加，也可以从 cluster 里面添加。如果从 cluster 里面添加，是否就属于合格组织一员？

6. 删除 cluster 或则 runtime 之后对应的信息是否全部作废。
7. 暂停状态数据如何标记，激活 对应的 metadata 之前相关的数据是否启动
8. 删除 cluster 之前之后校验 关联。如何关联存在是否允许删除
9. kafka 的查询维度全是 cluster
10. RocketMQ 大集群查询，是否支持 metadata 数据查询，如何对数据进行展示？ 是否现实差异化，是否提示差异字段
    1. 那些 topic 有差异？
    2. 是否配置 不允许有差异？
    3. 如果不允许有差异， console 进行处理的 metadata 是否要进行具体标识
11. RocketMQ 支持大集群，小集群，runtime 维度的展示
12. 指标数据表结构，目前不支持表修改。是否需要支持变修改
13. 是否需要支持 不同的 cluster 使用不同的 指标数据修改

## SyncStatus 状态补充

- 同步表，需要添加 SyncStatus 字段
- Entity 需要一个 SyncStatus 类
- 状态转换确认 。 RemotingService（是否独立出来） 里面 成功与失败状态
- 所有查询 需要过滤 成功状态
- 操作数据时， 需要修改 SyncStatus
- 需要一个 SyncStatus 管理类

无法进行确认，支持同类型修改

项目发起人，项目架构师，项目设计师，项目经理，
交互设计师，半个ui设计
前端，后台，运维，产品，测试，

## 用户域

### 用户首页

1. 查看 组织

### 组织首页

1. 左边是一级菜单：
    1. 工作台
    2. eventmesh集群
    3.
2. 集群信息
    1. 集群列表

### 日志分类

1. 心跳加载日志
2. 数据同步日志
3. 运维操作日志
4. 数据采集日志
5. metadata 数据日志