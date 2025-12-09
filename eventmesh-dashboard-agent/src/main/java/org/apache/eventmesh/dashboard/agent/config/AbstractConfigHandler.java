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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;

import lombok.Setter;

@Setter
public abstract class AbstractConfigHandler implements ConfigHandler {

    protected String runtimeHome;


    protected void readConfigFile(String configFile, String fileType, Map<String, String> configData) throws IOException {
        File file = new File(configFile);
        if (file.exists()) {
            Path source = Paths.get(configFile);
            Path target = Paths.get(configFile + ".copy." + System.currentTimeMillis());
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            if (!file.delete()) {
                throw new IOException(String.format("delete file %s failed", file.getAbsolutePath()));
            }
        }
        if (!file.createNewFile()) {
            throw new RuntimeException("file is exist");
        }
        try (FileOutputStream fos = new FileOutputStream(file)) {
            StringBuffer stringBuffer = new StringBuffer();
            configData.forEach((k, v) -> {
                stringBuffer.append(k).append('=').append(v).append(System.lineSeparator());
            });
            fos.write(stringBuffer.toString().getBytes());
            fos.flush();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
