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


package org.apache.eventmesh.dashboard.core.remoting;

import org.apache.eventmesh.dashboard.common.model.remoting.GlobalResult;
import org.apache.eventmesh.dashboard.core.function.SDK.AbstractClientInfo;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKTypeEnum;

/**
 * 默认是一个集群，操作是基于集群操作还是单个操作
 */
public abstract class AbstractRemotingService<T> extends AbstractClientInfo<T> {


    @Override
    protected SDKTypeEnum getSDKTypeEnum() {
        return SDKTypeEnum.ADMIN;
    }


    @SuppressWarnings("unchecked")
    protected <D> D createSuccessGlobalResult(Object data) {
        GlobalResult<D> globalResult = new GlobalResult<>();
        globalResult.setCode(200);
        globalResult.setData((D) data);
        return (D) globalResult;
    }

}
