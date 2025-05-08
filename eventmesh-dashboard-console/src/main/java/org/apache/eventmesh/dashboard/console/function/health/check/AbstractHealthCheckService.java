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


package org.apache.eventmesh.dashboard.console.function.health.check;

import org.apache.eventmesh.dashboard.console.function.health.callback.HealthCheckCallback;
import org.apache.eventmesh.dashboard.core.function.SDK.AbstractClientInfo;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKTypeEnum;

import lombok.Getter;

/**
 * extends
 */
@Getter
public abstract class AbstractHealthCheckService<T> extends AbstractClientInfo<T> implements HealthCheckService {


    private volatile boolean endCheck = true;


    public SDKTypeEnum getSDKTypeEnum() {
        return SDKTypeEnum.PING;
    }

    public void check(HealthCheckCallback callback) throws Exception {
        this.endCheck = false;
        try {
            this.doCheck(callback);
        } catch (Exception e) {
            this.endCheck = true;
            callback.onFail(e);
        }

    }

    public abstract void doCheck(HealthCheckCallback callback) throws Exception;

    public void setEndCheck() {
        this.endCheck = true;
    }

    protected boolean isEndCheck() {
        return this.endCheck;
    }

    @Override
    public void init() throws Exception {
    }

    @Override
    public void destroy() throws Exception{

    }
}
