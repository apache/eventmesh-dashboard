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

import org.apache.eventmesh.dashboard.console.entity.cases.ResourcesConfigEntity;
import org.apache.eventmesh.dashboard.console.entity.function.ConfigEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import lombok.Data;

@Data
public class ScriptBuildData {

    private Map<String, Object> data = new HashMap<String, Object>();


    private List<ConfigEntity> configEntityList = new ArrayList<ConfigEntity>();

    public void put(String key, Object value) {
        data.put(key, value);
    }

    public void setResourcesConfigEntity(ResourcesConfigEntity resourcesConfigEntity) {
        JSONObject jsonObject = (JSONObject) JSON.toJSON(resourcesConfigEntity);
        this.data.putAll(jsonObject);
    }

    public void setConfigEntityList(List<ConfigEntity> configEntityList) {
        this.configEntityList.addAll(configEntityList);
    }

    public void setConfigEntity(ConfigEntity configEntity) {
        this.configEntityList.add(configEntity);
    }

}
