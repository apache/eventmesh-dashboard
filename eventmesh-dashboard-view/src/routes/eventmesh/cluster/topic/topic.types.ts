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

export enum TopicHealthStatusEnum {
  // 失败;
  Failed = 0,
  // 成功;
  Succeed = 1,
  // 检查;
  Checking = 2,
  // 超时;
  Expired = 3
}

export enum TopicCreationStatusEnum {
  // 等待创建中;
  Pending = 0,
  // 正在创建中;
  Processing = 1,
  // 创建成功;
  Created = 2,
  // 创建失败;
  Failed = 3,
  // 未知
  Unkown = 4
}

export type Topic = {
  id: number
  topicName: string
  description: string
  status: TopicHealthStatusEnum
  createProgress: TopicCreationStatusEnum
  retentionMs: number
}
