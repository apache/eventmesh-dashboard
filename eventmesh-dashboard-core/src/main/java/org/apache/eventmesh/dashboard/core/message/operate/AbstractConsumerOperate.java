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


package org.apache.eventmesh.dashboard.core.message.operate;

import org.apache.eventmesh.dashboard.core.function.SDK.config.CreateSDKConfig;
import org.apache.eventmesh.dashboard.core.message.model.ConsumerDTO;

import java.util.ArrayList;
import java.util.List;


public abstract class AbstractConsumerOperate<CL, C extends CreateSDKConfig>
    extends AbstractMessageOperate<CL, C, ConsumerDTO> implements ConsumerOperate {

    private List<Object> data = new ArrayList<>();

    private int tate;


    public List<Object> pull() {
        synchronized (this.data) {
            List<Object> data = this.data;
            this.data = new ArrayList<>();
            return data;
        }
    }

}
