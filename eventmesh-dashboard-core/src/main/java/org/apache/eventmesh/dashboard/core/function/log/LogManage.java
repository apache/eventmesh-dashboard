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

package org.apache.eventmesh.dashboard.core.function.log;

/**
 * 问题： 运行中同步行为情况 需要对 sync 行为进行监控，否者不知道运行时的情况怎么样 sync 与 心跳模块打印打得 太频繁了，造成了大量日志。把日志收集起来写入时序数据库 or MySQL 需要对写入的数据库进行控制否者数据量实在太大了,需要对数据进行手动归纳，形成数据压缩
 * 比如对5秒钟之内的行为进行归来，相同行为只出一条数据。 写入量可能非常大，5秒钟写一次，可能有 runtimeNum * metadataNum
 * <p>
 * <p>
 * 需要的写入点： 1. MetadataSyncWrapper 2. SyncMetadataCreateFactory 3. RemotingService
 * <p>
 * 如何提供查询，是提供查询页面，还是通过 dbeare 异常如果进行报警
 */
public class LogManage {


}
