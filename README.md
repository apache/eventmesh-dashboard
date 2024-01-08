# EventMesh Dashboard

## 介绍

## 业务架构

## 技术架构

### 模块依赖图

### 模块介绍

1. eventmesh-dashboard-console  业务模块的代码
2. eventmesh-dashboard-observe  监控模块的代码
3. eventmesh-dashboard-core     对eventmesh以及相关组件的代码
4. eventmesh-dashboard-service  公用接口
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