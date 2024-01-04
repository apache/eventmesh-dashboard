# Getting Started

## Build on source code

```
cd eventmesh-dashboard
./gradlew clean bootJar
```

```
java -jar build/libs/eventmesh-dashboard-0.0.1-SNAPSHOT.jar
```

## Build and Run with Docker

```
cd eventmesh-dashboard
./gradlew clean bootJar
docker build -t yourname/eventmesh-dashboard -f docker/Dockerfile .
```

```
docker run -d --name eventmesh-dashboard -p 8080:8080 yourname/eventmesh-dashboard
```