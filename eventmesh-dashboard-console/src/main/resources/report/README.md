# Kafka metrics

Kafka collection uses one AdminClient task per cluster and one JMX task per active Broker or KRaft node with a configured JMX port. It uses the existing collector schedule and report storage.

The eight report names are `kafka_cluster_metrics`, `kafka_topic_metrics`, `kafka_partition_offset_metrics`, `kafka_consumer_group_metrics`, `kafka_consumer_offset_metrics`, `kafka_broker_traffic_metrics`, `kafka_broker_topic_traffic_metrics`, and `kafka_broker_health_metrics`. Broker and Topic traffic use cumulative JMX `Count` values. Missing values use `-1`; zero remains a valid measurement.

Run the Kafka regression profile with Kafka, its Broker JMX endpoint, IoTDB, and a MySQL test instance available:

```sh
./mvnw -B -pl eventmesh-dashboard-console -am -Pkafka-collect-tests \
  -Dkafka.collect.bootstrap=127.0.0.1:19092 \
  -Dkafka.collect.host=127.0.0.1 -Dkafka.collect.port=19092 \
  -Dkafka.collect.jmx.port=19999 \
  -Dkafka.collect.iotdb.address=127.0.0.1:6667 \
  -Dkafka.collect.mysql.address=127.0.0.1:13306 \
  -DfailIfNoTests=false test
```

The live tests create and remove their own Topic, consumer group, and test databases. The MySQL test defaults to the user `root` and password `codex_test_pw`; override them with `kafka.collect.mysql.user` and `kafka.collect.mysql.password`. For a ZooKeeper Broker, set `kafka.collect.node.type=STORAGE_KAFKA_BROKER`; KRaft uses `STORAGE_KAFKA_RAFT`.
