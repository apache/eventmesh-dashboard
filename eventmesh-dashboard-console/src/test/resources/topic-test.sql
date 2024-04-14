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
FROM `eventmesh_dashboard_test`.topic
WHERE TRUE;
ALTER TABLE `eventmesh_dashboard_test`.topic
    AUTO_INCREMENT = 1;

INSERT INTO eventmesh_dashboard_test.topic (id, cluster_id, topic_name, storage_id, retention_ms, type, description, create_time, update_time, status)
values (562, 1, 'hello', '1', -2, 1, '', '2024-03-18 13:25:56', '2024-03-18 13:25:56', 1),
       (563, 1, 'world', '1', -2, 0, '', '2024-03-18 13:25:56', '2024-03-18 13:25:56', 1),
       (564, 1, 'abc', '1', -2, 0, '', '2024-03-18 13:25:56', '2024-03-18 13:25:56', 1),
       (565, 2, 'xyz', '1', -2, 0, '', '2024-03-18 13:25:56', '2024-03-18 13:25:56', 1),
       (566, 3, '333', '1', -2, 0, '', '2024-03-18 13:25:56', '2024-03-18 13:25:56', 1);