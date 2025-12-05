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

package org.apache.eventmesh.dashboard.service.remoting;

import org.apache.eventmesh.dashboard.common.model.remoting.GlobalResult;
import org.apache.eventmesh.dashboard.common.model.remoting.RemotingServiceException;

import java.util.Objects;
import java.util.concurrent.Callable;


/**
 * 需要所有接口提供 结果直接返回 与 GlobalResult 返回两种方案。 第一种方案： 在 结果直接返回 调用 resultObjectToGlobalResult 方法，</p> 在 GlobalResult 调用 globalResultToResultObject 方法 </p>
 * 子接口可以重写一类接口或全部接口 问题： globalResultToResultObject 打印日志不方便</p>
 * <p>
 * 放弃</p> 第二种方案： GlobalResult 返回 EMPTY_RESULT，表示 没有实现。</p> 结果直接返回 如何确定 没有实现 </p>
 * <p>
 * see org.apache.eventmesh.dashboard.core.remoting.Remoting2Manage </p>
 * <p>
 * TODO 第二种方案需要一天多的时间
 */
public interface RemotingBaseService {

    GlobalResult<Object> EMPTY_RESULT = new GlobalResult<>();

    @Deprecated
    default Object globalResultToResultObject(GlobalResult<Object> globalResult) {
        if (Objects.equals(globalResult.getCode(), 200)) {
            return globalResult.getData();
        }
        throw new RuntimeException(globalResult.getCode().toString());
    }


    @Deprecated
    default GlobalResult<Object> resultObjectToGlobalResult(Callable<Object> callable) {
        GlobalResult<Object> globalResult = new GlobalResult<>();
        try {
            Object object = callable.call();
            globalResult.setCode(200);
            globalResult.setData(object);
        } catch (Exception e) {
            if (e instanceof RemotingServiceException remotingServiceException) {
                globalResult.setCode(remotingServiceException.getCode());
                globalResult.setMessage(remotingServiceException.getMessage());
            } else {
                globalResult.setCode(500);
                globalResult.setMessage(e.getMessage());
            }
            globalResult.setThrowable(e);
        }
        return globalResult;
    }

}
