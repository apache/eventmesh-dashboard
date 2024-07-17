/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import { TopicCreationStatusEnum, TopicHealthStatusEnum } from './topic.types'

export const TopicHealthStatusText = {
  [TopicHealthStatusEnum.Failed]: '失败',
  [TopicHealthStatusEnum.Succeed]: '成功',
  [TopicHealthStatusEnum.Checking]: '正在检查',
  [TopicHealthStatusEnum.Expired]: '已超时'
}

export const TopicCreationStatusText = {
  [TopicCreationStatusEnum.Pending]: '等待创建',
  [TopicCreationStatusEnum.Processing]: '正在创建',
  [TopicCreationStatusEnum.Created]: '创建成功',
  [TopicCreationStatusEnum.Failed]: '创建失败',
  [TopicCreationStatusEnum.Unkown]: '未知'
}
