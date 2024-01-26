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

insert into `eventmesh-dashboard-test`.connection (id, cluster_phy_id, source_type, source_id, source_status, sink_type, sink_id, sink_status, runtime_id, status, topic, group_id, group_name, description, create_time, end_time, update_time)
values  (1, 1, 'connector', 1, 0, 'connector', 1, 0, 1, 0, 'test-topic', -1, '', '', '2024-01-26 14:51:16', '2024-01-26 14:51:16', '2024-01-26 14:51:16'),
    (2, 1, 'connector', 2, 1, 'connector', 2, 0, 2, 0, 'test-topic', -1, '', '', '2024-01-26 14:51:16', '2024-01-26 14:51:16', '2024-01-26 14:51:16'),
    (3, 1, 'connector', 3, 0, 'connector', 3, 0, 3, 0, 'test-topic', -1, '', '', '2024-01-26 14:51:16', '2024-01-26 14:51:16', '2024-01-26 14:51:16'),
    (4, 2, 'connector', 1, 0, 'connector', 1, 0, 1, 0, 'test-topic', -1, '', '', '2024-01-26 14:51:16', '2024-01-26 14:51:16', '2024-01-26 15:57:44'),
    (5, 2, 'client', 5, 0, 'client', 5, 1, 5, 1, 'test-topic', -1, '', '', '2024-01-26 14:51:16', '2024-01-26 14:51:16', '2024-01-26 14:51:16'),
    (6, 3, 'client', 6, 1, 'client', 6, 1, 6, 1, 'test-topic', -1, '', '', '2024-01-26 14:51:16', '2024-01-26 14:51:16', '2024-01-26 14:51:16');


