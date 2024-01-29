/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

DELETE FROM `eventmesh-dashboard-test`.connection WHERE TRUE;
ALTER TABLE `eventmesh-dashboard-test`.connection AUTO_INCREMENT = 1;

insert into `eventmesh-dashboard-test`.connection (id, cluster_id, source_type, source_id, sink_type, sink_id, runtime_id, status, topic, group_id, description, create_time, end_time, update_time)
values  (1, 1, 'connector', 1, 'connector', 1, 1, 0, 'test-topic', -1, '', '2024-01-27 11:55:11', '2024-01-27 11:55:11', '2024-01-27 11:55:11'),
    (2, 1, 'connector', 2, 'connector', 2, 2, 0, 'test-topic', -1, '', '2024-01-27 11:55:11', '2024-01-27 11:55:11', '2024-01-27 11:55:11'),
    (3, 1, 'connector', 3, 'connector', 3, 3, 0, 'test-topic', -1, '', '2024-01-27 11:55:11', '2024-01-27 11:55:11', '2024-01-27 11:55:11'),
    (4, 2, 'connector', 1, 'connector', 1, 1, 0, 'test-topic', -1, '', '2024-01-27 11:55:11', '2024-01-27 11:55:11', '2024-01-27 11:55:11'),
    (5, 2, 'client', 5, 'client', 5, 5, 0, 'test-topic', -1, '', '2024-01-27 11:55:11', '2024-01-27 11:55:11', '2024-01-27 11:55:11'),
    (6, 3, 'client', 6, 'client', 6, 6, 0, 'test-topic', -1, '', '2024-01-27 11:55:11', '2024-01-27 11:55:11', '2024-01-27 11:55:11');


