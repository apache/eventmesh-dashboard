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


package org.apache.eventmesh.dashboard.core.remoting.rocketmq;

import org.apache.eventmesh.dashboard.core.remoting.AbstractRemotingService;

import org.apache.rocketmq.tools.admin.DefaultMQAdminExt;

/**
 * rocketmq 其他不同的是。 以nameservier 为主。那么可以多集群。一个eventmesh 可以操作多个集群
 */
public abstract class AbstractRocketMQRemotingService extends AbstractRemotingService<DefaultMQAdminExt> {


}
