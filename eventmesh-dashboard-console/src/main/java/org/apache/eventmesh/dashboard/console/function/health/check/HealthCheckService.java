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

import org.apache.eventmesh.dashboard.console.function.health.HealthExecutor;
import org.apache.eventmesh.dashboard.console.function.health.callback.HealthCheckCallback;

/**
 * Health check service interface.<br>
 * To add a new check service, extend the {@link AbstractHealthCheckService}.
 * @see AbstractHealthCheckService
 */
public interface HealthCheckService {

    /**
     * Do the health check.<br>
     * To implement a new check service, add the necessary logic to call the success and fail functions of the callback.
     * @param callback The behaviour of the callback is defined as a lambda function when used. Please refer to {@link HealthExecutor} for usage.
     */
    public void doCheck(HealthCheckCallback callback);

    public void init();

    public void destroy();

}
