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
DELETE FROM `eventmesh_dashboard_test`.health_check_result WHERE TRUE;
ALTER TABLE `eventmesh_dashboard_test`.health_check_result AUTO_INCREMENT = 1;

insert into `eventmesh_dashboard_test`.health_check_result (id, type, type_id, cluster_id, state, result_desc, create_time, update_time)
values (1, 1, 1, 1, 0, '', '2024-02-02 18:56:50', '2024-02-02 18:56:50'),
       (2, 2, 2, 1, 1, '', '2024-02-02 18:56:50', '2024-02-02 18:56:50'),
       (3, 3, 3, 1, 1, '', '2024-02-02 18:56:50', '2024-02-02 18:56:50'),
       (4, 4, 4, 1, 1, '', '2024-02-02 18:56:50', '2024-02-02 18:56:50'),
       (5, 1, 2, 1, 1, '', '2024-02-04 18:56:50', '2024-02-02 19:33:13'),
       (6, 4, 2, 2, 0, '', '2024-02-04 18:56:50', '2024-02-02 19:33:13');