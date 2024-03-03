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
FROM `eventmesh_dashboard_test`.client
WHERE TRUE;
ALTER TABLE `eventmesh_dashboard_test`.client
    AUTO_INCREMENT = 1;

insert into `eventmesh_dashboard_test`.client (id, cluster_id, name, platform, language, pid, host, port, protocol, status, config_ids, description,
                                               create_time, end_time, update_time)
values (1, 3, '', '', 'java', -1, '', -1, '', 1, '', '', '2024-02-02 15:15:15', '2024-02-02 15:15:15', '2024-02-02 15:15:15'),
       (2, 3, '', '', 'java', -1, '', -1, '', 1, '', '', '2024-02-02 15:15:15', '2024-02-02 15:15:15', '2024-02-02 15:15:15'),
       (3, 3, '', '', 'go', -1, '', -1, '', 1, '', '', '2024-02-02 15:15:15', '2024-02-02 15:15:15', '2024-02-02 15:15:15'),
       (4, 4, '', '', 'go', -1, '', -1, '', 1, '', '', '2024-02-02 15:15:15', '2024-02-02 15:15:15', '2024-02-02 15:15:15');