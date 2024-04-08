# EventMesh Dashboard

## 介绍

## 业务架构

## 技术架构

### 环境

- JDK 8/11
- Maven 3.8.1
- Spring Boot 2.7.x

### 模块依赖图

### 模块介绍

1. eventmesh-dashboard-console  业务模块的代码，调用service接口
2. eventmesh-dashboard-observe  监控模块的代码
3. eventmesh-dashboard-core     对EventMesh Runtime, Meta以及相关组件的代码，提供service实现
4. eventmesh-dashboard-service  公用API接口，对core的抽象
5. eventmesh-dashboard-common   公共模块的代码
6. eventmesh-dashboard-view     前端代码

### eventmesh-dashboard-core 介绍

## Build

### Build on source code

```
cd eventmesh-dashboard
./gradlew clean bootJar
```

```
java -jar build/libs/eventmesh-dashboard-0.0.1-SNAPSHOT.jar
```

### Build and Run with Docker

```
cd eventmesh-dashboard
./gradlew clean bootJar
docker build -t yourname/eventmesh-dashboard -f docker/Dockerfile .
```

```
docker run -d --name eventmesh-dashboard -p 8080:8080 yourname/eventmesh-dashboard
```