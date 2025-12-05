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

package org.apache.eventmesh.dashboard.console.function.report.model.base;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * TODO
 *  统计基类 </p>
 *  有两个体系： CAP 体系 与 NOT 体系 </p>
 *  CAP 体系从 cluster 开始， NOT 体系从 runtime 开始
 *  但是因为：RocketMQ 普罗米修斯 的行为卡在 CAP 与 NOT 体系之间，需要特殊处理 </p>
 *  所以还有一个体系。</p>
 *  Kafka 不管用 普罗米修斯 还是 console 写 都是一套体系 </p>
 */
@Data
public class Time {

    private LocalDateTime time;

}
