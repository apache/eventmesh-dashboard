##   

1. 存储 remote 服务
    1. topic 创建在 cluster main runtime
2. message
    1. 发送
    2. 查看消息
    3. 发布订阅
    4. console ->  抽象层 -> broker
    5. rocketmq 4.0 push（pull） 5.0 pull
3. offset
4. k8s
    1. 集群
        2. 创建
        3.
    2. 节点
        3. 创建
        4. 停用
        5. 删除
    2. console ， docker 执行脚本
    2. console 与 操作分开
    3. 6c 16G
    4. eventmesh
    5. kafka
        1. 取舍问题
    6. rocketmq
        1. 简单
        2. 不做难得
        3. 两个集群
            1. 单节点
            2. 一主已从
    7. pulsuer
        1. 我有
5. 数据采集快
    1. 存储已经支持的采集
    2. RocketMQ 实现
6. 前端
7. 认证与权限，介入
    1. rocketmq 的插件
        2. acl ->  console 同步
    2. kafka 的 插件
        3. 修改
8. 一键部署
9. 巡检
10. 告警

## 部署业务模块

>

## 难点

超级难点一:
因消息中间件架构不一样，所以处理不一样。
Kafka是
1. CAP 架构，操作一个节点就可以。
2. 单注册中心，meta 集群决定 kafka 集群的 一致性架构
3. 操作性行为
4. 难点 复制：kafka 基于 topic 复制
RocketMQ 是
1. 多子集群
1. 子集群主从架构。且部分功能独立
2. 有 raft 架构
3. 难点 复制：RocketMQ 是 主从复制
2. 多注册中心，使用 AP 架构。broker 集群架构由 broker 集群决定
pulsar（占时不支持） 是
1. 多计算集群多存储集群的计算与存储分离架构
2. 存储使用 BookKeeper，只能通过 pulsar broker 进行操作
2. 多注册中心。meta 集群决定 pulsar 集群的 一致性架构
3. 跨集群复制
4. 难点 复制： 基于 BookKeeper 复制

集群创建的时候，可以定义集群 一致性 架构
broker 集群决定一致性架构

重置维度

1. 最大
2. 最小
3. 指定 offset
4. 指定 某个时间点
   PS： RocketMQ 的 queue 与 kafka 的 partition 是一个意思。
   超级难点一:
   因消息中间件架构不一样，所以处理不一样
   kafka 的 offset 操作时 cluster行为
   RocketMQ 操作时 broker 行为

kafka 只需要 队列id 就行了

1. 按照某个维度重置 topic 下面所有 队列
2. 按照某个维度重置 topic 下某个队列
   RocketMQ 的重置场景
    1. 按照某个维度重置 topic 在集群里面所有的 队列
    2. 按照某个维度重置 topic 在集群里面某个主从节点的 队列
    3. 按照某个维度重置 topic 在集群里面所有的 下某个队列
    4. 按照某个维度重置 topic 在集群里面某个主从节点的 下某个队列
       PS：以上 每次操作都需要同时操作主节点与从节点
    5. 按照某个维度重置 topic 在某个节点上的所有 队列
    6. 按照某个维度重置 topic 在某个节点上的所有 某个队列

## 数据关联

1. 所有对 eventmesh 的操作，需要再对应的存储集群进行操作
2. 什么状态的集群进行操作【】。读取数据的时候，需要过滤掉，不正常得节点

##  

## 数据创建

1. 把导出的数据，导入
    1. 点击 eventmesh 集群导出 or 备份
        1. 如果 eventmesh 独立集群直接操作
        2. 不是，检查 依赖集群是否同时进行了备份
            1. 没有，提示某某集群
    2. 点击 runtime 集群
    3. 点击 meta 集群
    4. 点击 存储 集群
        1. 存储集群
2. 直接点击 cluster 绑定
    1. 定时 保存 cluster 信息
    2. 绑定 集群架构
        1. eventmesh cluster
            1. meta cluster 列表
                1. runtime列表
                2. 一份 runtime config
            2. runtime cluster 列表
                1. runtime
                    1. 每个 runtime config
                2. runtime 的 acl
                3. topic 列表
                4. group
            3. 存储 cluster 列表
                1. meta cluster 列表
                    1. runtime列表
                    2. 一份 runtime config
                2. runtime cluster 列表
                    1. runtime
                        1. 每个 runtime config
                    2. user
                    3. acl
                    4. topic 列表
                    5. group 列表
        2. 绑定 集群配置
        3. 绑定 runtime 配置
        4. 如果没有 resource 信息是不允许绑定的
        5. 是否可以绑定 topic信息
        6. 是否绑定 group 信息
        7. 是否绑定 acl 信息
3. case 修改就是创建
4. 在 runtime 点击编辑。TODO 不允许在在runtime编辑。
    1. 只能 添加 resource
5. 在 deploy 模块，添加
6. deploy
    1. 部署方案
    2. 资源配置
    3. 部署关系
7. 部署信息
    1. 每个runtime 的部署 对应一个 资源信息
    2. cluster 第一次部署对应一个部署信息
        1. 这次
8.  