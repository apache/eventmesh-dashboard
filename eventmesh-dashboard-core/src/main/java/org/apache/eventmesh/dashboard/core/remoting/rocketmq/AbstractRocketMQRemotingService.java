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

import org.apache.eventmesh.dashboard.common.model.remoting.GlobalResult;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKManager;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKTypeEnum;
import org.apache.eventmesh.dashboard.core.function.SDK.config.CreateRocketmqConfig;
import org.apache.eventmesh.dashboard.core.remoting.AbstractRemotingService;

import org.apache.rocketmq.tools.admin.DefaultMQAdminExt;
import org.apache.rocketmq.tools.command.CommandUtil;

import java.util.AbstractMap;
import java.util.Objects;
import java.util.Set;

/**
 * rocketmq 其他不同的是。 以nameservier 为主。那么可以多集群。一个eventmesh 可以操作多个集群
 */
public abstract class AbstractRocketMQRemotingService extends AbstractRemotingService {


    protected DefaultMQAdminExt defaultMQAdminExt;

    protected CreateRocketmqConfig createRocketmqConfig;


    @Override
    public void createConfig() {
        createRocketmqConfig = new CreateRocketmqConfig();
        createRocketmqConfig.setNameServerUrl(this.getMetaString());
    }

    @Override
    protected void doInit() {
        AbstractMap.SimpleEntry<String, DefaultMQAdminExt> clientSimple =
            SDKManager.getInstance().createClient(SDKTypeEnum.STORAGE_ROCKETMQ_ADMIN, createRocketmqConfig);
        this.defaultMQAdminExt = clientSimple.getValue();
    }

    protected <T> T cluster(GlobalResult t, Function function) {
        try {

            /*for(ColonyDO clusterDO : this.cache.getClusterDOList()){
                    for(RuntimeMetadata runtimeMetadata : clusterDO.getRuntimeEntityList()){

                    }
            }*/

            Set<String> masterSet =
                CommandUtil.fetchMasterAddrByClusterName(defaultMQAdminExt, createRocketmqConfig.getClusterName());
            for (String masterName : masterSet) {
                Object newResult = function.apply(masterName, t);
                if (Objects.nonNull(newResult)) {
                    return (T) newResult;
                }
            }
            t.setCode(200);
        } catch (Exception exception) {
            t.setCode(400);
            t.setErrorMessages(exception.getMessage());
            t.setThrowable(exception);
        } finally {
            return (T) t;
        }
    }

    ;

    protected <T> T clusterName(GlobalResult t, Function function) {
        try {

            Object newResult = function.apply(createRocketmqConfig.getClusterName(), t);
            if (Objects.nonNull(newResult)) {
                return (T) newResult;
            }
            t.setCode(200);
        } catch (Exception exception) {
            t.setCode(400);
            t.setErrorMessages(exception.getMessage());
            t.setThrowable(exception);
        } finally {
            return (T) t;
        }
    }

    /**
     * @param <T>
     */
    protected interface Function<T> {

        T apply(String masterName, T t) throws Exception;
    }


}
