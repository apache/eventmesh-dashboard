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


package org.apache.eventmesh.dashboard.core.remoting.kafka.live.kraft;

import org.apache.eventmesh.dashboard.common.enums.ClusterType;
import org.apache.eventmesh.dashboard.common.model.metadata.AclMetadata;
import org.apache.eventmesh.dashboard.common.model.metadata.ClusterMetadata;
import org.apache.eventmesh.dashboard.common.model.remoting.acl.CreateAclRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.acl.DeleteAclRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.acl.GetAcls2Request;
import org.apache.eventmesh.dashboard.core.function.SDK.ConfigManage;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKManage;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKTypeEnum;
import org.apache.eventmesh.dashboard.core.function.SDK.config.NetAddress;
import org.apache.eventmesh.dashboard.core.remoting.Remoting2Manage;
import org.apache.eventmesh.dashboard.core.remoting.kafka.KafkaTestLog;
import org.apache.eventmesh.dashboard.service.remoting.kafka.AclRemotingService;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.common.acl.AccessControlEntry;
import org.apache.kafka.common.acl.AccessControlEntryFilter;
import org.apache.kafka.common.acl.AclBinding;
import org.apache.kafka.common.acl.AclBindingFilter;
import org.apache.kafka.common.acl.AclOperation;
import org.apache.kafka.common.acl.AclPermissionType;
import org.apache.kafka.common.resource.PatternType;
import org.apache.kafka.common.resource.ResourcePattern;
import org.apache.kafka.common.resource.ResourcePatternFilter;
import org.apache.kafka.common.resource.ResourceType;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import lombok.extern.slf4j.Slf4j;

/**
 * KRaft ACL CRUD：IDEA 可独立运行每个方法，连接启用 StandardAuthorizer 的专用本地 Broker。
 * 此环境允许 User:ANONYMOUS 管理规则，仅验证 ACL CRUD，不验证 SASL/TLS 或业务客户端权限执行。
 */
@Slf4j
@Tag("kraft")
@DisplayName("KRaft 环境：ACL 查询、创建、删除")
@ExtendWith(KafkaTestLog.class)
class KafkaAclIntegrationTest {

    private static final String BROKER_HOST = "127.0.0.1";
    private static final int BROKER_PORT = 39092;

    private ClusterMetadata cluster;
    private AdminClient client;
    private AdminClient ping;
    private AclRemotingService service;
    private String principal;
    private String resource;

    @BeforeEach
    void setUp() throws Exception {
        principal = "User:dashboard-acl-" + UUID.randomUUID();
        resource = "dashboard-acl-" + UUID.randomUUID();
        cluster = new ClusterMetadata();
        cluster.setId(System.nanoTime());
        cluster.setClusterType(ClusterType.STORAGE_KAFKA_RAFT);
        var config = ConfigManage.getInstance().getMultiCreateSdkConfig(cluster.getClusterType(), SDKTypeEnum.ADMIN);
        config.setKey(cluster.getId().toString());
        config.addNetAddress(NetAddress.create(BROKER_HOST, BROKER_PORT));
        SDKManage.getInstance().createClient(SDKTypeEnum.ADMIN, cluster, config, cluster.getClusterType());
        client = SDKManage.getInstance().getClient(SDKTypeEnum.ADMIN, cluster.getUnique());
        ping = SDKManage.getInstance().getClient(SDKTypeEnum.PING, cluster.getUnique());
        service = Remoting2Manage.getInstance().createRemotingService(AclRemotingService.class, cluster);
        log.info("【数据准备】地址={}:{}，Principal={}，资源={}", BROKER_HOST, BROKER_PORT, principal, resource);
    }

    /** 创建单条规则并重复创建，官方回读应只有一条。 */
    @Test
    @DisplayName("真实 Kafka：创建 ACL 并验证重复创建幂等")
    void createAcl() throws Exception {
        var metadata = metadata(AclOperation.READ, PatternType.LITERAL, AclPermissionType.ALLOW);
        CreateAclRequest request = createRequest(metadata);
        Assertions.assertEquals(200, service.createAcl(request).getCode());
        Assertions.assertEquals(200, service.createAcl(request).getCode());
        awaitBindings(List.of(binding(AclOperation.READ, PatternType.LITERAL, AclPermissionType.ALLOW)));
    }

    /** 查询只返回指定主体的完整规则，包含前缀模式和拒绝权限。 */
    @Test
    @DisplayName("真实 Kafka：查询 ACL 保留完整字段和前缀拒绝规则")
    void queryAcls() throws Exception {
        var read = binding(AclOperation.READ, PatternType.PREFIXED, AclPermissionType.DENY);
        var write = binding(AclOperation.WRITE, PatternType.PREFIXED, AclPermissionType.DENY);
        client.createAcls(List.of(write, read)).all().get(10, TimeUnit.SECONDS);
        awaitBindings(List.of(read, write));
        GetAcls2Request request = new GetAcls2Request();
        AclMetadata filter = new AclMetadata();
        filter.setPrincipal(principal);
        request.setMetaData(filter);
        List<AclMetadata> rows = service.getAllAcls(request).getData();
        Assertions.assertEquals(2, rows.size());
        Assertions.assertEquals(List.of((int) AclOperation.READ.code(), (int) AclOperation.WRITE.code()),
            rows.stream().map(AclMetadata::getOperation).toList());
        Assertions.assertEquals(List.of(principal, principal), rows.stream().map(AclMetadata::getPrincipal).toList());
        AclMetadata actual = rows.get(0);
        Assertions.assertEquals(principal, actual.getPrincipal());
        Assertions.assertEquals("*", actual.getHost());
        Assertions.assertEquals("TOPIC", actual.getResourceType());
        Assertions.assertEquals(resource, actual.getResourceName());
        Assertions.assertEquals((int) PatternType.PREFIXED.code(), actual.getPatternType());
        Assertions.assertEquals((int) AclOperation.READ.code(), actual.getOperation());
        Assertions.assertEquals("DENY", actual.getPermissionType());
        log.info("【查询结果】Principal={}，资源={}，权限={}", actual.getPrincipal(), actual.getResourceName(), actual.getPermissionType());
    }

    /** 删除 READ 规则后，同一主体同资源的 WRITE 规则保持存在。 */
    @Test
    @DisplayName("真实 Kafka：精确删除 ACL 保留相邻规则")
    void deleteAcl() throws Exception {
        var read = binding(AclOperation.READ, PatternType.LITERAL, AclPermissionType.ALLOW);
        var write = binding(AclOperation.WRITE, PatternType.LITERAL, AclPermissionType.ALLOW);
        client.createAcls(List.of(read, write)).all().get(10, TimeUnit.SECONDS);
        awaitBindings(List.of(read, write));
        DeleteAclRequest request = new DeleteAclRequest();
        request.setMetaData(metadata(AclOperation.READ, PatternType.LITERAL, AclPermissionType.ALLOW));
        Assertions.assertEquals(200, service.deleteAcl(request).getCode());
        awaitBindings(List.of(write));
        Assertions.assertEquals(200, service.deleteAcl(request).getCode());
        awaitBindings(List.of(write));
    }

    /** 通过框架反射 ADD、QUEUE_ALL、DELETE，验证请求构造和列表结果。 */
    @Test
    @DisplayName("真实 Kafka：ACL 框架反射新增、查询和删除")
    void reflectiveAclOperations() throws Exception {
        var read = metadata(AclOperation.READ, PatternType.LITERAL, AclPermissionType.ALLOW);
        var write = metadata(AclOperation.WRITE, PatternType.LITERAL, AclPermissionType.ALLOW);
        write.setId(2L);
        var handler = Remoting2Manage.getInstance().createDataMetadataHandler(AclRemotingService.class, cluster);
        handler.handleAll(List.of(), List.of(read, write), List.of(), List.of());
        awaitBindings(List.of(binding(AclOperation.READ, PatternType.LITERAL, AclPermissionType.ALLOW),
            binding(AclOperation.WRITE, PatternType.LITERAL, AclPermissionType.ALLOW)));
        List<AclMetadata> rows = handler.getData().stream().map(AclMetadata.class::cast)
            .filter(row -> principal.equals(row.getPrincipal())).toList();
        Assertions.assertEquals(List.of((int) AclOperation.READ.code(), (int) AclOperation.WRITE.code()),
            rows.stream().map(AclMetadata::getOperation).toList());
        handler.handleAll(List.of(), List.of(), List.of(), List.of(read));
        awaitBindings(List.of(binding(AclOperation.WRITE, PatternType.LITERAL, AclPermissionType.ALLOW)));
        handler.handleAll(List.of(), List.of(), List.of(), List.of(write));
        awaitBindings(List.of());
    }

    private CreateAclRequest createRequest(AclMetadata metadata) {
        CreateAclRequest request = new CreateAclRequest();
        request.setMetaData(metadata);
        return request;
    }

    private AclMetadata metadata(AclOperation operation, PatternType pattern, AclPermissionType permission) {
        AclMetadata metadata = new AclMetadata();
        metadata.setId(1L);
        metadata.setPrincipal(principal);
        metadata.setHost("*");
        metadata.setResourceType("TOPIC");
        metadata.setResourceName(resource);
        metadata.setPatternType((int) pattern.code());
        metadata.setOperation((int) operation.code());
        metadata.setPermissionType(permission.name());
        return metadata;
    }

    private AclBinding binding(AclOperation operation, PatternType pattern, AclPermissionType permission) {
        return new AclBinding(new ResourcePattern(ResourceType.TOPIC, resource, pattern),
            new AccessControlEntry(principal, "*", operation, permission));
    }

    private AclBindingFilter ownedRules() {
        return new AclBindingFilter(ResourcePatternFilter.ANY,
            new AccessControlEntryFilter(principal, null, AclOperation.ANY, AclPermissionType.ANY));
    }

    private void awaitBindings(List<AclBinding> expected) throws Exception {
        Collection<AclBinding> actual = List.of();
        for (int attempt = 0; attempt < 50; attempt++) {
            actual = client.describeAcls(ownedRules()).values().get(10, TimeUnit.SECONDS);
            if (actual.size() == expected.size() && actual.containsAll(expected)) {
                log.info("【规则回读】Principal={}，实际条数={}，预期条数={}", principal, actual.size(), expected.size());
                return;
            }
            Thread.sleep(100);
        }
        Assertions.assertEquals(new java.util.HashSet<>(expected), new java.util.HashSet<>(actual));
    }

    @AfterEach
    void cleanUp() throws Exception {
        try {
            if (client != null) {
                client.deleteAcls(List.of(ownedRules())).all().get(10, TimeUnit.SECONDS);
                awaitBindings(List.of());
            }
        } finally {
            try {
                if (client != null) {
                    client.close(Duration.ofSeconds(5));
                }
            } finally {
                try {
                    if (ping != null && ping != client) {
                        ping.close(Duration.ofSeconds(5));
                    }
                } finally {
                    if (cluster != null) {
                        SDKManage.getInstance().deleteClient(null, cluster.getUnique());
                    }
                }
            }
        }
    }
}
