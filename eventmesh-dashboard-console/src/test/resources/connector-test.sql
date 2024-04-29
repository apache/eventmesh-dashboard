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
DELETE
FROM `eventmesh_dashboard_test`.connection
WHERE TRUE;
ALTER TABLE `eventmesh_dashboard_test`.connection
    AUTO_INCREMENT = 1;

insert into eventmesh_dashboard_test.connector (id, cluster_id, name, class_name, type, host, port, status, pod_state, config_ids, create_time,
                                                update_time)
values (1, 1, '', 'the', '', '127.0.0.1', 8888, 1, 1, '', '2024-02-02 16:43:45', '2024-03-26 14:23:43'),
       (2, 1, '', 'quick', '', '192.168.3.1', 8888, 1, 1, '', '2024-02-02 16:43:45', '2024-03-26 14:24:53'),
       (3, 1, '', 'brown', '', '192.168.3.2', 8881, 1, 2, '', '2024-02-02 16:43:45', '2024-03-26 14:24:53'),
       (4, 2, '', 'fox', '', '192.168.3.3', 8882, 1, 2, '', '2024-02-02 16:43:45', '2024-03-26 14:25:02'),
       (5, 3, '', 'jumps', '', '192.168.3.4', 8883, 1, 3, '', '2024-02-02 16:43:45', '2024-03-26 14:23:43');