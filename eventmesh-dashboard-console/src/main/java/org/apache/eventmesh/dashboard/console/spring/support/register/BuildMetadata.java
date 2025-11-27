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

package org.apache.eventmesh.dashboard.console.spring.support.register;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.port.PortValidate;
import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.entity.function.ConfigEntity;

import java.util.List;

/**
 *
 */
public interface BuildMetadata {


    boolean isMeta();


    PortValidate portValidate();


    List<ClusterType> runtimeTypes();

    /**
     *  在 create ，绑定时，调用，修改对应的配置。这样速度快点，可以减少 RuntimeDeployService 的复杂度
     */
    default ConfigEntity createMetaConfig(RuntimeEntity target, List<RuntimeEntity> runtimeMetadataList){
        return null;
    }

    /**
     *  这里是 在 create 时候调用?  如果识别了 依赖配置存在，就不进行处理
     */
    void buildMetaAddress(ScriptBuildData data, RuntimeEntity target, List<RuntimeEntity> runtimeMetadataList);


    void buildConfig(ScriptBuildData data, RuntimeEntity target);

}
