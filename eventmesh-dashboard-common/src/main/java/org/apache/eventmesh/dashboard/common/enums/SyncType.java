/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.eventmesh.dashboard.common.enums;

import lombok.Getter;

@Getter
public enum SyncType {

    NOT("NOT","没命中"),

    READONLY("READONLY","只读"),

    FIRSTTOWHOM("FIRSTTOWHOM","第一次同步"),

    INIT("INIT","初始化"),

    CHECK("CHECK","检查"),

    TIMINGSYNC("TIMINGSYNC","定时同步"),

    ;

    private final String name;

    private final String description;


    SyncType(String name , String description) {
        this.name = name;
        this.description = description;
    }


    @Override
    public String toString() {
        return "SyncType{" +
               "name='" + name + '\'' +
               ", description='" + description + '\'' +
               '}';
    }
}
