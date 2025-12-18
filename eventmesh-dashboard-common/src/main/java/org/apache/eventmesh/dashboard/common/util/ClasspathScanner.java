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


package org.apache.eventmesh.dashboard.common.util;


import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.ClassUtils;

import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class ClasspathScanner {

    private Class<?> base;

    private String baseString;

    private String subPath;


    private boolean allSubDirectory = true;

    private Set<Class<?>> interfaceSet;

    private Set<Class<?>> annotationSet;

    private String designation;


    private Resource[] getResource() throws IOException {
        PathMatchingResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
        return resourcePatternResolver.getResources(this.createLocationPattern());
    }

    public List<Class<?>> getClazz() throws Exception {
        Resource[] resources = this.getResource();
        List<Class<?>> resourcesList = new ArrayList<>();
        for (Resource resource : resources) {
            if (this.excludeTest(resource)) {
                continue;
            }
            Class<?> clazz = this.createClass(resource);
            if (Modifier.isAbstract(clazz.getModifiers())) {
                continue;
            }
            if (Objects.isNull(this.interfaceSet)) {
                resourcesList.add(clazz);
                continue;
            }

            if (this.includeInterface(clazz)) {
                resourcesList.add(clazz);
                continue;
            }

            Class<?>[] innerClass = clazz.getDeclaredClasses();
            if (ArrayUtils.isEmpty(innerClass)) {
                continue;
            }
            for (Class<?> c : innerClass) {
                if (this.includeInterface(c)) {
                    resourcesList.add(c);
                }
            }
        }
        return resourcesList;
    }

    private boolean excludeTest(Resource resource) throws IOException {
        return resource.getFile().getPath().contains("/target/test-classes/");
    }

    private Class<?> createClass(Resource resource) throws IOException, ClassNotFoundException {
        String path = resource.getURL().getPath();
        String classPath = path.substring(path.indexOf(this.baseString)).replace("/", ".").replace(".class", "");
        return Class.forName(classPath);
    }

    private boolean includeInterface(Class<?> clazz) throws Exception {
        if (CollectionUtils.isEmpty(this.interfaceSet)) {
            return true;
        }
        Set<Class<?>> classInterfaceSet = ClassUtils.getAllInterfacesForClassAsSet(clazz);
        for (Class<?> c : this.interfaceSet) {
            if (classInterfaceSet.contains(c)) {
                return true;
            }
        }
        return false;
    }

    private String createLocationPattern() {
        StringBuilder stringBuffer = new StringBuilder();
        stringBuffer.append("classpath*:./");
        this.baseString = base.getPackage().getName().replace('.', '/');
        stringBuffer.append(this.baseString);
        stringBuffer.append("/");
        stringBuffer.append(subPath);
        if (this.allSubDirectory) {
            stringBuffer.append("/**");
        }
        /**
         *  SDK*.class
         */
        if (Objects.isNull(this.designation)) {
            stringBuffer.append("/*");
        } else {
            stringBuffer.append("/");
            stringBuffer.append(this.designation);
        }
        stringBuffer.append(".class");
        return stringBuffer.toString();
    }
}

