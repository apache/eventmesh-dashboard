# EventMesh Dashboard

[🌐 简体中文](README.zh-CN.md)

## Introduction

The EventMesh Dashboard is under development and will support functionalities such as Connection management, cluster health checks, etc. Feel free to reach out to the [EventMesh Assistant](https://github.com/apache/eventmesh?tab=readme-ov-file#community) to contribute.

The Dashboard for EventMesh, maintained during v1.8.0 ~ v1.10.0, is a pure frontend project located at the [Next.js Dashboard branch](https://github.com/apache/eventmesh-dashboard/tree/nextjs-dashboard).

Weekly development meeting documents for EventMesh Dashboard: https://docs.qq.com/doc/DQmhVbklUdGNNWGZi

## Technical Architecture

### Environment

- JDK 8/11
- Maven 3.8.1
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
mvn clean package
```

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