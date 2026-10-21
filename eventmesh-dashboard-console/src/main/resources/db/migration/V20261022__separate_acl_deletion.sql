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

-- One-time MySQL migration for the pre-is_delete ACL schema.
-- Run before deploying the new Mapper, with ACL writes paused.
-- MySQL ALTER TABLE commits implicitly; this is not an atomic application rollout.
-- Legacy ACL status=0 meant deleted; after migration status=0 means disabled.
ALTER TABLE acl ADD COLUMN is_delete INT NOT NULL DEFAULT 0 COMMENT 'Logical deletion: 0 retained, 1 deleted';
UPDATE acl SET is_delete=1, status=1, update_time=CURRENT_TIMESTAMP WHERE status=0;
ALTER TABLE acl MODIFY COLUMN status INT NOT NULL DEFAULT 1 COMMENT 'Business status: 0 disabled, 1 enabled';
