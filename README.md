# Apache EventMesh Dashboard

[🌐 简体中文](README.zh-CN.md)

## Introduction

The EventMesh Dashboard is under development and will support functionalities such as Connection management, cluster health checks, etc. Feel free to reach out to the [EventMesh Assistant](https://github.com/apache/eventmesh?tab=readme-ov-file#community) to contribute.

The Dashboard for EventMesh, maintained during v1.8.0 ~ v1.10.0, is a pure frontend project located at the [Next.js Dashboard branch](https://github.com/apache/eventmesh-dashboard/tree/nextjs-dashboard).

Weekly development meeting documents for EventMesh Dashboard: https://docs.qq.com/doc/DQmhVbklUdGNNWGZi

## Technical Architecture

![](https://github.com/apache/eventmesh/assets/34571087/f61103a8-e9a4-419f-ab42-ae99feb4f431)

### Environment

- JDK 8/11
- Maven 3.9.x
- Spring Boot 2.7.x

### Module Introduction

1. eventmesh-dashboard-console: Code for business modules, invoking service interfaces.
2. eventmesh-dashboard-observe: Code for monitoring modules.
3. eventmesh-dashboard-core: Code for EventMesh Runtime, Meta, and related components, providing service implementations.
4. eventmesh-dashboard-service: Common API interfaces, abstracting core functionalities.
5. eventmesh-dashboard-common: Code for common modules.
6. eventmesh-dashboard-view: Frontend code.

## Auto Deploy EventMesh Dashboard

When the repository code is updated, the script will build and run the EventMesh Dashboard based on the latest version of the code.

### Usage

```
cd ~/service
git clone https://github.com/apache/eventmesh-dashboard.git
cd eventmesh-dashboard/deployment/
```

Edit credentials:

```
cp .env.example .env
vim .env
```

Add task to crontab:

```
crontab -e
```

```
0 * * * * bash ~/service/eventmesh-dashboard/deployment/auto-deploy-eventmesh-dashboard.sh
```

## Build

### Build on source code

```
cd eventmesh-dashboard
./mvnw clean package
```

>TODO download mysql-connector-j manually

```
java -DDB_ADDRESS=$DB_ADDRESS -DDB_USERNAME=$DB_USERNAME -DDB_PASSWORD=$DB_PASSWORD -jar eventmesh-dashboard-console/target/eventmesh-dashboard-console-0.0.1-SNAPSHOT.jar
```

### Build and Run with Docker

>To be updated

```
cd eventmesh-dashboard
./gradlew clean bootJar
docker build -t yourname/eventmesh-dashboard -f docker/Dockerfile .
```

```
docker run -d --name eventmesh-dashboard -p 8080:8080 yourname/eventmesh-dashboard
```


1. 注册中心的 创建，添加，删除， 扩容，缩容。
2. runtime  创建，添加，删除， 扩容，缩容。
3. 连接的 创建 ， 添加，删除，扩容，缩容。
4. rocketmq 的 创建，添加，删除。

### 基础页面
1. 用户登录页面

### 业务方面页面
1. 用户欢迎页面
   2. 基础统计信息
   3. 常用 集群，关注集群
   4. 事件
2. eventmesh 集群
   1. runtime
      1. overview
      2. 列表页面
         1. 详情
         2. topic
         3. group
         4. config
      3. runtime操作
   2. 注册中心
      3. 列表
      4. 添加注册中心
      5. 删除注册中心
      6. 
   3. topic
   4. 消费组
   5. 安全
   6. 消息
      7. 消息生产与消费
3. eventmesh 注册中心
4. eventmesh runtime
5. 

5. 集群
   1. 集群创建
      1. eventmesh  and rocketmq
         1. runtime 的 自动创建
         2. 注册中心的创建
         3. eventmesh 的 自动创建
      2. 手动创建
         1. 注册中心
      3. k8s创建
         1. 整体集群创建
         2. 单个集群创建
   2. 集群暂停
      1. 需要解除依赖，才能暂停
   3. 集群删除
      1. 需要解除依赖，才能暂停
   4. 集群的关联
6. runtime group topic
   1. 创建
      1. 手动创建
      2. 自动创建
   2. 删除
   3. 修改
   4. config
   5. 关联
7. 集群元数据复制并创建
8. 集群恢复