# Apache EventMesh Dashboard

[🌐 English Version](README.md)

## 介绍

EventMesh Dashboard 处于开发中，将支持 Connection 管理、集群健康检查等功能。欢迎联系 [EventMesh 小助手](https://github.com/apache/eventmesh?tab=readme-ov-file#community)参与贡献。

EventMesh 于 v1.8.0 ~ v1.10.0 期间维护的 Dashboard 纯前端项目位于 [Next.js Dashboard 分支](https://github.com/apache/eventmesh-dashboard/tree/nextjs-dashboard)。

EventMesh Dashboard 每周开发例会文档：https://docs.qq.com/doc/DQmhVbklUdGNNWGZi

## 技术架构

![](https://github.com/apache/eventmesh/assets/34571087/f61103a8-e9a4-419f-ab42-ae99feb4f431)

### 环境

- JDK 8/11
- Maven 3.9.x
- Spring Boot 2.7.x

### 模块介绍

1. eventmesh-dashboard-console  业务模块的代码，调用service接口
2. eventmesh-dashboard-observe  监控模块的代码
3. eventmesh-dashboard-core     对EventMesh Runtime, Meta以及相关组件的代码，提供service实现
4. eventmesh-dashboard-service  公用API接口，对core的抽象
5. eventmesh-dashboard-common   公共模块的代码
6. eventmesh-dashboard-view     前端代码

## 自动部署最新版 EventMesh Dashboard

当仓库代码更新后，脚本将基于最新版本的代码构建并运行 EventMesh Dashboard。

### 快速开始

```
cd ~/service
git clone https://github.com/apache/eventmesh-dashboard.git
cd eventmesh-dashboard/deployment/
```

编辑凭据：

```
cp .env.example .env
vim .env
```

添加定时任务：

```
crontab -e
```

```
0 * * * * bash ~/service/eventmesh-dashboard/deployment/auto-deploy-eventmesh-dashboard.sh
```

## 构建

### 使用源代码构建

```
cd eventmesh-dashboard
./mvnw clean package
```

>TODO download mysql-connector-j manually

```
java -DDB_ADDRESS=$DB_ADDRESS -DDB_USERNAME=$DB_USERNAME -DDB_PASSWORD=$DB_PASSWORD -jar eventmesh-dashboard-console/target/eventmesh-dashboard-console-0.0.1-SNAPSHOT.jar
```

### 构建并运行 Docker 镜像

>To be updated

```
cd eventmesh-dashboard
./gradlew clean bootJar
docker build -t yourname/eventmesh-dashboard -f docker/Dockerfile .
```

```
docker run -d --name eventmesh-dashboard -p 8080:8080 yourname/eventmesh-dashboard
```

### 开发准备
1. 配置格式化 
   1. 后端格式文档地址：https://eventmesh.apache.org/zh/community/contribute/contribute/
   2. 前端使用 eslint
2. 配置协议头工具 license-eye。
   1. 检查命令：license-eye header check
   2. 使用命令：license-eye header fix
   3. 下载地址： https://skywalking.apache.org/downloads/