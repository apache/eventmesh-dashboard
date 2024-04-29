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

DELETE FROM `eventmesh_dashboard_test`.runtime WHERE TRUE;
ALTER TABLE `eventmesh_dashboard_test`.runtime AUTO_INCREMENT = 1;

INSERT INTO eventmesh_dashboard_test.runtime (id, cluster_id, host, storage_cluster_id, port, jmx_port, start_timestamp, rack, status, create_time, update_time, endpoint_map)
values  (1, 1, '127.0.0.1', -1, 12345, -1, -1, '', 1, '2024-03-15 14:13:20', '2024-03-18 09:53:10', ''),
        (2, 2, '127.0.0.1', -1, 12344, -1, -1, '', 1, '2024-03-15 14:13:20', '2024-03-18 09:53:10', ''),
        (3, 3, '127.0.0.1', -1, 12333, -1, -1, '', 1, '2024-03-15 14:13:20', '2024-03-18 09:53:10', '');