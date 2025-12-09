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

package org.apache.eventmesh.dashboard.agent.config;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.junit.Test;

public class TestConfigHandler {

    AbstractConfigHandler abstractConfigHandler = new AbstractConfigHandler() {

        @Override
        public void handler() {

        }
    };


    @Test
    public void test() throws URISyntaxException, IOException {
        Map<String, String> map = new HashMap<>();

        for (int i = 0; i < 20; i++) {
            map.put("key" + i, "value" + i);
        }
        URI uri = Objects.requireNonNull(TestConfigHandler.class.getClassLoader().getResource("")).toURI();
        Path resourcePath = Paths.get(uri);
        abstractConfigHandler.setRuntimeHome(resourcePath.toFile().getAbsolutePath());
        String fileName = resourcePath.toFile().getAbsolutePath() + "/config.properties";
        abstractConfigHandler.readConfigFile(fileName, "", map);
    }

}
