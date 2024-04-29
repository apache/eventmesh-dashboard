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

DELETE FROM `eventmesh_dashboard_test`.meta WHERE TRUE;
ALTER TABLE `eventmesh_dashboard_test`.meta AUTO_INCREMENT = 1;

insert into `eventmesh_dashboard_test`.meta (id, name, type, version, cluster_id, host, port, role, username, params, status, create_time, update_time)
values  (1, '1', 'nacos', '1.0', 1, '192.168.3.14', -1, '-1', '', '', 0, '2024-02-03 10:30:02', '2024-02-03 10:30:02'),
    (2, '2', 'zookeeper', '1.0', 1, '192.168.3.15', -1, '-1', '', '', 0, '2024-02-03 10:30:02', '2024-02-03 10:30:02');