# Kafka metrics

Kafka collection uses one AdminClient task per cluster and one JMX task per Broker or KRaft node with a configured JMX port. The tasks share the existing collection metadata and report storage pipeline.

The eight report names are `kafka_cluster_metrics`, `kafka_topic_metrics`, `kafka_partition_offset_metrics`, `kafka_consumer_group_metrics`, `kafka_consumer_offset_metrics`, `kafka_broker_traffic_metrics`, `kafka_broker_topic_traffic_metrics`, and `kafka_broker_health_metrics`. Broker and Topic traffic stores cumulative JMX `Count` values. The other models store gauges. Missing values use `-1`; zero remains a valid measurement.

`POST /report/reportBySingle` returns an array of rows in the application's result envelope. `POST /report/reportByHome` accepts `reportNameList` and returns data keyed by each report name. Reports with the same `reportType` remain separate. The default envelope puts the query result in `data` and uses `code` for the application status.

For example, query consumer offsets with:

```json
{
  "reportName": "kafka_consumer_offset_metrics",
  "reportType": "gauge",
  "organizationId": 1,
  "clustersId": 2,
  "topicName": "orders",
  "groupName": "buyers",
  "partitionIndex": "0",
  "startTime": "2026-10-27T10:00:00",
  "endTime": "2026-10-27T10:05:00"
}
```

Time ranges include `startTime` and exclude `endTime`. Broker reports also accept `runtimeId` and `runtimeType`. Kafka Topic and group reports use name tags; a filter that the selected model does not support returns an error instead of returning unrelated resources.

Set `selectFun` to `sum`, `avg`, `min`, `max`, or `count` for aggregation. Set `interval` to a positive duration such as `10s`, `5m`, or `1h`. Aggregation preserves every tag, including Topic, group, and partition. Request functions and intervals are validated before connecting to IoTDB. JDBC binds values, including quoted consumer group names.

Run the unit and schema regressions from the repository root:

```sh
./mvnw -B -pl eventmesh-dashboard-console -am -Pkafka-collect-tests -DfailIfNoTests=false test
```

The live profile requires Kafka, its Broker JMX endpoint, and IoTDB with the table SQL dialect. It creates and removes its own Topic, group, and database. Missing services fail the test.

```sh
./mvnw -B -pl eventmesh-dashboard-console -am -Pkafka-collect-live-tests \
  -Dkafka.collect.bootstrap=127.0.0.1:9092 \
  -Dkafka.collect.host=127.0.0.1 -Dkafka.collect.port=9092 \
  -Dkafka.collect.jmx.port=9999 \
  -Dkafka.collect.iotdb.address=127.0.0.1:6667 \
  -DfailIfNoTests=false test
```

For a ZooKeeper Broker, set `kafka.collect.node.type=STORAGE_KAFKA_BROKER`. KRaft uses the default `STORAGE_KAFKA_RAFT`. The live test verifies concurrent registration and collection, all eight tables, consumption lag, quoted group names, detail and aggregate queries, resource filters, and the Spring MVC JSON endpoints.

New installations use a unique Runtime address of `(cluster_id, host, port)` and a nonunique `kubernetes_cluster_id` index. Existing installations with the former constraints need the same schema migration before registering multiple Brokers on one host. Historical integer `collect.enable` values and empty JMX ports are supported by the JDBC metadata mapper.
