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


package org.apache.eventmesh.dashboard.core.remoting.rocketmq;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.model.metadata.RocketMQAclAccountMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;
import org.apache.eventmesh.dashboard.common.model.remoting.acl.CreateAclRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.acl.DeleteAclRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.acl.GetAcls2Request;
import org.apache.eventmesh.dashboard.core.function.SDK.ConfigManage;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKManage;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKTypeEnum;
import org.apache.eventmesh.dashboard.core.function.SDK.config.CreateRemotingConfig;
import org.apache.eventmesh.dashboard.core.function.SDK.config.CreateRemotingConfig.AclVersion;
import org.apache.eventmesh.dashboard.core.function.SDK.config.NetAddress;
import org.apache.eventmesh.dashboard.core.function.SDK.operation.rocketmq.RocketMQRemotingSDKOperation.DefaultRemotingClient;
import org.apache.eventmesh.dashboard.core.remoting.Remoting2Manage;
import org.apache.eventmesh.dashboard.service.remoting.AclRemotingService;

import org.apache.rocketmq.remoting.protocol.RemotingCommand;
import org.apache.rocketmq.remoting.protocol.ResponseCode;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import lombok.extern.slf4j.Slf4j;

/** Requires an isolated ACL 1.0 Broker allowing the test connection. Does not test signed-client authorization. */
@Slf4j
class RocketMQAclV1IntegrationTest {

    private RuntimeMetadata runtime;
    private DefaultRemotingClient client;
    private AclRemotingService service;
    private RocketMQAclAccountMetadata account;
    private boolean created;

    @BeforeEach
    void setUp() throws Exception {
        this.runtime = new RuntimeMetadata();
        this.runtime.setId(98001L);
        this.runtime.setClusterId(98000L);
        this.runtime.setClusterType(ClusterType.STORAGE_ROCKETMQ_BROKER_MAIN_SLAVE);
        CreateRemotingConfig config = (CreateRemotingConfig) ConfigManage.getInstance()
            .getSimpleCreateSdkConfig(this.runtime.getClusterType(), SDKTypeEnum.ADMIN);
        config.setAclVersion(AclVersion.V1);
        NetAddress address = new NetAddress();
        address.setAddress(System.getProperty("rocketmq.acl.v1.broker.host", "127.0.0.1"));
        address.setPort(Integer.getInteger("rocketmq.acl.v1.broker.port", 22911));
        config.setNetAddress(address);
        SDKManage.getInstance().createClient(SDKTypeEnum.ADMIN, this.runtime, config, this.runtime.getClusterType());
        this.client = SDKManage.getInstance().getClient(SDKTypeEnum.ADMIN, this.runtime.getUnique());
        this.service = Remoting2Manage.getInstance().createRemotingService(AclRemotingService.class, this.runtime);
        this.account = new RocketMQAclAccountMetadata();
        this.account.setAccessKey("dashboard_acl_v1_" + UUID.randomUUID().toString().replace("-", ""));
        this.account.setSecretKey(UUID.randomUUID().toString());
        this.account.setAdmin(false);
        this.account.setWhiteRemoteAddress("");
        this.account.setDefaultTopicPerm("DENY");
        this.account.setDefaultGroupPerm("DENY");
        this.account.setTopicPerms(List.of("orders=PUB|SUB"));
        this.account.setGroupPerms(List.of("workers=SUB"));
        log.info("【真实 Broker 测试】使用 V1 节点配置：地址={}:{}，临时账号={}",
            address.getAddress(), address.getPort(), this.account.getAccessKey());
    }

    @AfterEach
    void tearDown() throws Exception {
        try {
            if (this.created) {
                Assertions.assertEquals(200, this.service.deleteAcl(this.deleteRequest()).getCode());
                log.info("临时 ACL 1.0 账号已清理：{}", this.account.getAccessKey());
            }
        } finally {
            try {
                if (this.client != null) {
                    this.client.shutdown();
                }
            } finally {
                if (this.runtime != null) {
                    SDKManage.getInstance().deleteClient(null, this.runtime.getUnique());
                }
            }
        }
    }

    @Test
    void createAccountThroughFramework() throws Exception {
        log.info("【真实 Broker 测试】通过原有框架创建 V1 账号，验证 Broker 配置版本变化");
        String before = this.version();
        this.upsert();
        this.awaitVersionChange(before);
    }

    @Test
    void updateAccountWithCompleteConfiguration() throws Exception {
        log.info("【真实 Broker 测试】更新 V1 账号，显式清空 Topic 权限并保留消费组规则");
        this.upsert();
        String before = this.version();
        this.account.setTopicPerms(List.of());
        this.upsert();
        this.awaitVersionChange(before);
    }

    @Test
    void deleteEntireAccountExplicitly() throws Exception {
        log.info("【真实 Broker 测试】显式删除整个 V1 账号，重复删除应返回失败");
        this.upsert();
        Assertions.assertEquals(200, this.service.deleteAcl(this.deleteRequest()).getCode());
        this.created = false;
        Assertions.assertNotEquals(200, this.service.deleteAcl(this.deleteRequest()).getCode());
    }

    @Test
    void accountListIsExplicitlyUnsupported() throws Exception {
        log.info("【真实 Broker 测试】核实 V1 Broker 不支持账号列表，适配器明确报告能力限制");
        RemotingCommand response = this.client.invokeSync(RemotingCommand.createRequestCommand(54, null), 3000);
        Assertions.assertEquals(ResponseCode.REQUEST_CODE_NOT_SUPPORTED, response.getCode());
        Assertions.assertThrows(UnsupportedOperationException.class, () -> this.service.getAllAcls(new GetAcls2Request()));
    }

    private void upsert() throws Exception {
        CreateAclRequest request = new CreateAclRequest();
        request.setMetaData(this.account);
        Assertions.assertEquals(200, this.service.createAcl(request).getCode());
        this.created = true;
        log.info("V1 账号写入成功：账号={}，Topic 权限={}，Group 权限={}",
            this.account.getAccessKey(), this.account.getTopicPerms(), this.account.getGroupPerms());
    }

    private DeleteAclRequest deleteRequest() {
        RocketMQAclAccountMetadata identity = new RocketMQAclAccountMetadata();
        identity.setAccessKey(this.account.getAccessKey());
        DeleteAclRequest request = new DeleteAclRequest();
        request.setMetaData(identity);
        request.setDeleteAccount(true);
        return request;
    }

    private void awaitVersionChange(String before) throws Exception {
        long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(15);
        String after = this.version();
        // Legacy version reporting follows the file watcher rather than the synchronous write response.
        while (before.equals(after) && System.nanoTime() < deadline) {
            Thread.sleep(100);
            after = this.version();
        }
        Assertions.assertNotEquals(before, after);
        log.info("V1 Broker 配置版本已变化：更新前={}，更新后={}", before, after);
    }

    private String version() throws Exception {
        RemotingCommand response = this.client.invokeSync(RemotingCommand.createRequestCommand(52, null), 3000);
        Assertions.assertEquals(ResponseCode.SUCCESS, response.getCode(), "An ACL-enabled 1.0 Broker is required");
        Assertions.assertNotNull(response.getExtFields());
        String version = response.getExtFields().get("version");
        Assertions.assertNotNull(version);
        return version;
    }
}
