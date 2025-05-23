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

package org.apache.eventmesh.dashboard.common.port;

import java.util.List;

public class NotPortValidate extends AbstractPortValidate {


    public static NotPortValidate builder() {
        return builder(1);
    }

    public static NotPortValidate builder(Integer num) {
        NotPortValidate notPortValidate = new NotPortValidate();
        notPortValidate.build(num);
        return notPortValidate;
    }


    protected void build(Integer num) {
        for (int i = 0; i < num; i++) {
            PortRule rule = PortRule.builder().valid(false).build();
            this.setPortRules(rule);
        }
    }


    @Override
    public boolean validate(PortRule lastPortRule, Integer lastPort, PortRule current, Integer port, List<PortRule> portRules) {
        return false;
    }
}
