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

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import lombok.Getter;
import lombok.Setter;

public abstract class AbstractTopicHealthCheckService<T> extends AbstractHealthCheckService<T> {

    @Setter
    @Getter
    private Integer offset = 0;

    @Setter
    @Getter
    private Integer queue = 0;

    private final AtomicLong atomicLong = new AtomicLong();

    protected byte[] messageContext() {
        return ("{ 'uid': " + atomicLong.incrementAndGet() + "}").getBytes();
    }

    protected boolean isCurrentValue(String context) {
        if (Objects.isNull(context)) {
            return false;
        }
        JSONObject json = JSON.parseObject(context);
        if (Objects.isNull(json.get("uid"))) {
            return false;
        }
        return Objects.equals(Integer.valueOf(json.get("uid").toString()), atomicLong.get());
    }

    protected boolean isCurrentValue(byte[] context) {
        if (Objects.isNull(context)) {
            return false;
        }
        return this.isCurrentValue(new String(context));
    }

}
