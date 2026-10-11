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


package org.apache.eventmesh.dashboard.core.remoting.kafka;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Records the JUnit outcome after assertions and cleanup, including setup/cleanup failures. */
public class KafkaTestLog implements BeforeEachCallback, TestWatcher {

    private static final Logger log = LoggerFactory.getLogger(KafkaTestLog.class);

    private String scenario(ExtensionContext context) {
        DisplayName suite = context.getRequiredTestClass().getAnnotation(DisplayName.class);
        return (suite == null ? context.getRequiredTestClass().getSimpleName() : suite.value()) + " / " + context.getDisplayName();
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        log.info("【测试开始】场景={}，方法={}", this.scenario(context), context.getRequiredTestMethod().getName());
    }

    @Override
    public void testSuccessful(ExtensionContext context) {
        log.info("【测试通过】场景={}，断言及清理已完成", this.scenario(context));
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        log.error("【测试失败】场景={}，异常类型={}，详细原因见 JUnit 测试报告", this.scenario(context), cause.getClass().getSimpleName());
    }

    @Override
    public void testAborted(ExtensionContext context, Throwable cause) {
        log.warn("【测试中止】场景={}，原因类型={}", this.scenario(context), cause.getClass().getSimpleName());
    }

    @Override
    public void testDisabled(ExtensionContext context, Optional<String> reason) {
        log.info("【测试跳过】场景={}，原因={}", this.scenario(context), reason.orElse("未提供"));
    }
}
