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


package org.apache.eventmesh.dashboard.common.model.remoting;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TODO
 *  如果定义返回类，
 *  定义异步返回，的成本太大，需要把所有链路实现异步返回操作
 *  需要统一异步返回行为。比如单个返回，多个返回。
 *  异步行为是否需要设计这样
 *  同步行为是否需要设计这样，是否可以通过异常去做。 直接返回结果，如果有问题 throw 异常，这样代码量很少。
 *  因为 所有的 远程调用，都在 RemotingService 里面。 在 RemotingService 做好同时支持 GlobalResult 与 直接返回
 *  对于同步行为
 *  本来设计成 请求，响应，返回 三件套的，因为完成整体链路的 异步调用，实在太麻烦了。最终
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlobalResult<T> {

    private Integer code;

    private String message;

    private String errorCode;

    private T data;

    private String errorMessages;

    private Throwable throwable;

}
