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

package org.apache.eventmesh.dashboard.console.function.client.create;

import org.apache.eventmesh.dashboard.console.function.client.AbstractClientOperation;
import org.apache.eventmesh.dashboard.console.function.client.config.CreateClientConfig;

import java.util.AbstractMap.SimpleEntry;

public class RuntimeClientCreateOperation extends AbstractClientOperation {
    @Override
    public SimpleEntry createClient(CreateClientConfig clientConfig) {
        return null;
    }

    @Override
    public void close(Object client) {

    }
}
