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

package org.apache.eventmesh.dashboard.console.controller.message;


import org.apache.eventmesh.dashboard.console.modle.message.offset.QueryOffsetByMessageMetadataDTO;
import org.apache.eventmesh.dashboard.console.modle.message.offset.ResetOffsetDTO;
import org.apache.eventmesh.dashboard.service.remoting.OffsetRemotingService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lamp.decoration.core.result.ResultObject;

/**
 * 这里是否添加，消息挤压的功能. dashboard， 从存储里面拉。
 */
@RestController
@RequestMapping("offset")
public class OffsetOperateController {

    @Autowired
    private OffsetRemotingService offsetRemotingService;


    public ResultObject<String> queryOffsetByMessageMetadataDTO(
        @RequestBody @Validated QueryOffsetByMessageMetadataDTO queryOffsetByMessageMetadataDTO) {

        return null;
    }


    public ResultObject<String> resetOffset(@RequestBody @Validated ResetOffsetDTO resetOffsetDTO) {

        return null;
    }


}
