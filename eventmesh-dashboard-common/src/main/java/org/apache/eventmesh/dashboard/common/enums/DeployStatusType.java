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


package org.apache.eventmesh.dashboard.common.enums;

import lombok.Getter;

/**
 * TODO 流程：
 *      runtime： 先 创建数据 -> 绑定关系 -> 在启动
 *      meta ： 先 创建数据直接启动
 *      eventmesh 空间是
 *          创建 eventmesh cluster ->
 *          创建 eventmesh meta  ->
 *          eventmesh cluster 与 meta 绑定关系 ->
 *          <!-- storage 流程 -->
 *          创建 storage cluster ->
 *          create storage meta 且直接启动 ->
 *          storage cluster 与 meta 绑定关系 ->
 *          create storage runtime ->
 *          storage cluster 与 runtime 绑定关系(绑定关系的时候，是否主动启动 runtime) ->
 *          启动 runtime ->
 *          eventmesh cluster 与 storage cluster 绑定关系 ->
 *          启动 eventmesh runtime
 *     启动操作在：
 *          meta BUILD_SUCCESS（数据构建） 时 创建 服务 ， 是否可选
 *          runtime 在 绑定时 ， 是否可选
 *          点击 启动
 *     有 agent 的存在，就不需要启动循序了
 */
public enum DeployStatusType {

    SETTLE,


    BUILD_SUCCESS("数据构建", """
        在 deploy 的 create handler 里面处理，如果使用 agent 模式，就没有意义,
        build_success 状态 下一个是  CREATE_WAIT
        """),

    RESOURCE_APPLY,

    RESOURCE_APPLY_FAILED,

    CREATE,

    CREATE_COPY,

    /**
     * 是不是需要这个状态的检查
     */
    CREATE_DATA_ING,

    CREATE_WAIT,

    CREATE_FULL_WAIT,

    CREATE_WAIT_TIMEOUT,

    CREATE_CAP_UPDATE_WAIT,

    CREATE_CAP_UPDATE_WAIT_TIMEOUT,

    CREATE_ING,

    CREATE_FULL_ING,

    CREATE_FAIL,

    CREATE_FULL_SUCCESS,

    CREATE_FULL_FAIL,

    CREATE_CAP_UPDATE_FAIL,

    CREATE_CAP_UPDATE_ING,

    CREATE_SUCCESS,

    CREATE_CAP_UPDATE_SUCCESS,

    UPDATE,

    UPDATE_WAIT,

    UPDATE_ING,

    UPDATE_SUCCESS,

    UPDATE_FAIL,

    UPDATE_FULL_WAIT,

    UPDATE_FULL_ING,

    UPDATE_FULL_SUCCESS,

    UPDATE_FULL_FAIL,

    PAUSE,

    PAUSE_WAIT,

    PAUSE_ING,

    PAUSE_FULL_ING,

    PAUSE_FULL_WAIT,

    PAUSE_SUCCESS,

    PAUSE_FAIL,

    PAUSE_FULL_SUCCESS,

    PAUSE_FULL_FAIL,

    RESET,

    RESET_WAIT,

    RESET_ING,

    RESET_FAIL,

    RESET_SUCCESS,


    UNINSTALL,

    UNINSTALL_FAIL,

    UNINSTALL_FAILED,

    UNINSTALL_ING,

    UNINSTALL_SUCCESS,
    ;

    @Getter
    private String name;

    @Getter
    private String explanation;

    DeployStatusType() {
    }

    DeployStatusType(String name, String explanation) {
        this.name = name;
        this.explanation = explanation;
    }
}
