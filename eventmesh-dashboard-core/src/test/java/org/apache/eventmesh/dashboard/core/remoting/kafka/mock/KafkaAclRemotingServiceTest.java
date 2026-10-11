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


package org.apache.eventmesh.dashboard.core.remoting.kafka.mock;

import org.apache.eventmesh.dashboard.common.model.metadata.AclMetadata;
import org.apache.eventmesh.dashboard.common.model.remoting.acl.CreateAclRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.acl.DeleteAclRequest;
import org.apache.eventmesh.dashboard.common.model.remoting.acl.GetAcls2Request;
import org.apache.eventmesh.dashboard.core.function.SDK.ClientWrapper;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKTypeEnum;
import org.apache.eventmesh.dashboard.core.remoting.kafka.KafkaAclRemotingService;
import org.apache.eventmesh.dashboard.core.remoting.kafka.KafkaTestLog;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.acl.AccessControlEntry;
import org.apache.kafka.common.acl.AclBinding;
import org.apache.kafka.common.acl.AclBindingFilter;
import org.apache.kafka.common.acl.AclOperation;
import org.apache.kafka.common.acl.AclPermissionType;
import org.apache.kafka.common.errors.ClusterAuthorizationException;
import org.apache.kafka.common.errors.SecurityDisabledException;
import org.apache.kafka.common.internals.KafkaFutureImpl;
import org.apache.kafka.common.resource.PatternType;
import org.apache.kafka.common.resource.ResourcePattern;
import org.apache.kafka.common.resource.ResourceType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

@ExtendWith(KafkaTestLog.class)
class KafkaAclRemotingServiceTest {

    private AdminClient client;
    private KafkaAclRemotingService service;

    @BeforeEach
    void setUp() {
        client = Mockito.mock(AdminClient.class, Mockito.RETURNS_DEEP_STUBS);
        ClientWrapper wrapper = new ClientWrapper();
        wrapper.getClientMap().put(SDKTypeEnum.ADMIN, client);
        service = new KafkaAclRemotingService();
        service.setClientWrapper(wrapper);
    }

    /** 创建时完整转换 ACL 七个字段，并复用托管客户端。 */
    @Test
    @DisplayName("模拟响应：创建 ACL 完整转换七个字段")
    void createExactBinding() throws Exception {
        var expected = binding("orders", PatternType.PREFIXED, AclOperation.READ, AclPermissionType.DENY);
        Mockito.when(client.createAcls(Mockito.eq(List.of(expected)), Mockito.any()).all()).thenReturn(KafkaFuture.completedFuture(null));
        var metadata = metadata();
        metadata.setPatternType((int) PatternType.PREFIXED.code());
        metadata.setPermissionType("DENY");
        CreateAclRequest request = new CreateAclRequest();
        request.setMetaData(metadata);
        Assertions.assertEquals(200, service.createAcl(request).getCode());
        Mockito.verify(client).createAcls(Mockito.eq(List.of(expected)), Mockito.argThat(options -> options.timeoutMs() == 10000));
    }

    /** 删除完整绑定，星号仍为精确字段值，不扩大为任意规则。 */
    @Test
    @DisplayName("模拟响应：删除仅匹配完整绑定并保留星号字面值")
    void deleteExactWildcardBinding() throws Exception {
        var expected = binding("*", PatternType.LITERAL, AclOperation.ALL, AclPermissionType.ALLOW).toFilter();
        Mockito.when(client.deleteAcls(Mockito.eq(List.of(expected)), Mockito.any()).all()).thenReturn(KafkaFuture.completedFuture(List.of()));
        var metadata = metadata();
        metadata.setResourceName("*");
        metadata.setOperation((int) AclOperation.ALL.code());
        DeleteAclRequest request = new DeleteAclRequest();
        request.setMetaData(metadata);
        Assertions.assertEquals(200, service.deleteAcl(request).getCode());
        Mockito.verify(client).deleteAcls(Mockito.eq(List.of(expected)), Mockito.any());
    }

    /** 查询保留同一用户的多条完整规则，并按字段排序。 */
    @Test
    @DisplayName("模拟响应：查询完整保留并排序同一用户的多条规则")
    void queryCompleteBindings() throws Exception {
        var read = binding("orders", PatternType.LITERAL, AclOperation.READ, AclPermissionType.ALLOW);
        var write = binding("orders", PatternType.LITERAL, AclOperation.WRITE, AclPermissionType.ALLOW);
        Mockito.when(client.describeAcls(Mockito.eq(AclBindingFilter.ANY), Mockito.any()).values())
            .thenReturn(KafkaFuture.completedFuture(List.of(write, read)));
        var result = service.getAllAcls(new GetAcls2Request());
        Assertions.assertEquals(200, result.getCode());
        Assertions.assertEquals(2, result.getData().size());
        var row = result.getData().stream().filter(acl -> acl.getOperation() == AclOperation.READ.code()).findFirst().orElseThrow();
        Assertions.assertEquals("User:alice", row.getPrincipal());
        Assertions.assertEquals("*", row.getHost());
        Assertions.assertEquals("TOPIC", row.getResourceType());
        Assertions.assertEquals("orders", row.getResourceName());
        Assertions.assertEquals((int) PatternType.LITERAL.code(), row.getPatternType());
        Assertions.assertEquals("ALLOW", row.getPermissionType());
        Assertions.assertEquals(List.of(read, write), result.getData().stream().map(this::binding).toList());
    }

    @Test
    @DisplayName("模拟响应：ACL 七个字段逐级排序不受 Broker 返回顺序影响")
    void querySortsByAllBindingFields() throws Exception {
        List<Consumer<AclMetadata>> mutations = List.of(acl -> acl.setPermissionType("DENY"),
            acl -> acl.setOperation((int) AclOperation.DESCRIBE_CONFIGS.code()), acl -> acl.setHost("127.0.0.1"),
            acl -> acl.setPrincipal("User:bob"), acl -> acl.setPatternType((int) PatternType.PREFIXED.code()),
            acl -> acl.setResourceName("orders-z"), acl -> acl.setResourceType("TRANSACTIONAL_ID"));
        List<AclBinding> expected = new ArrayList<>();
        expected.add(binding(metadata()));
        for (var mutation : mutations) {
            var metadata = metadata();
            mutation.accept(metadata);
            expected.add(binding(metadata));
        }
        List<AclBinding> reversed = new ArrayList<>(expected);
        Collections.reverse(reversed);
        Mockito.when(client.describeAcls(Mockito.eq(AclBindingFilter.ANY), Mockito.any()).values())
            .thenReturn(KafkaFuture.completedFuture(reversed), KafkaFuture.completedFuture(expected));
        Assertions.assertEquals(expected, service.getAllAcls(new GetAcls2Request()).getData().stream().map(this::binding).toList());
        Assertions.assertEquals(expected, service.getAllAcls(new GetAcls2Request()).getData().stream().map(this::binding).toList());
    }

    @Test
    @DisplayName("公共模型：填写 Kafka 同名字段不改变原有主体身份规则")
    void sharedIdentityDoesNotInferKafkaFromFields() {
        var metadata = metadata();
        Assertions.assertEquals("User:alice", metadata.nodeUnique());
        metadata.setResourceType("GROUP");
        metadata.setResourceName("payments");
        metadata.setPatternType((int) PatternType.PREFIXED.code());
        metadata.setHost("127.0.0.1");
        metadata.setOperation((int) AclOperation.WRITE.code());
        metadata.setPermissionType("DENY");
        Assertions.assertEquals("User:alice", metadata.nodeUnique());
    }

    /** 查询只按已提供的字段筛选，支持 Kafka MATCH 模式。 */
    @Test
    @DisplayName("模拟响应：ACL 查询支持部分条件和 MATCH")
    void queryFilters() throws Exception {
        Mockito.when(client.describeAcls(Mockito.any(), Mockito.any()).values()).thenReturn(KafkaFuture.completedFuture(List.of()));
        var metadata = new AclMetadata();
        metadata.setPrincipal("User:alice");
        metadata.setResourceName("orders");
        metadata.setPatternType((int) PatternType.MATCH.code());
        GetAcls2Request request = new GetAcls2Request();
        request.setMetaData(metadata);
        Mockito.clearInvocations(client);
        Assertions.assertTrue(service.getAllAcls(request).getData().isEmpty());
        ArgumentCaptor<AclBindingFilter> captured = ArgumentCaptor.forClass(AclBindingFilter.class);
        Mockito.verify(client).describeAcls(captured.capture(), Mockito.any());
        Assertions.assertEquals(ResourceType.ANY, captured.getValue().patternFilter().resourceType());
        Assertions.assertEquals(PatternType.MATCH, captured.getValue().patternFilter().patternType());
        Assertions.assertEquals("orders", captured.getValue().patternFilter().name());
        Assertions.assertEquals("User:alice", captured.getValue().entryFilter().principal());
        Assertions.assertNull(captured.getValue().entryFilter().host());
        Assertions.assertEquals(AclOperation.ANY, captured.getValue().entryFilter().operation());
    }

    /** 缺少任何绑定字段时，创建和删除都不能调用 Broker。 */
    @Test
    @DisplayName("模拟响应：缺少任意 ACL 字段时拒绝写入")
    void missingFieldsNeverWrite() {
        List<Consumer<AclMetadata>> omissions = List.of(acl -> acl.setResourceType(null), acl -> acl.setResourceName(null),
            acl -> acl.setPatternType(null), acl -> acl.setPrincipal(null), acl -> acl.setHost(null),
            acl -> acl.setOperation(null), acl -> acl.setPermissionType(null));
        for (var omission : omissions) {
            var metadata = metadata();
            omission.accept(metadata);
            assertWritesRejected(metadata);
        }
        Assertions.assertThrows(IllegalArgumentException.class, () -> service.createAcl(null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> service.deleteAcl(null));
        Mockito.verifyNoInteractions(client);
    }

    /** ANY、MATCH、未知值及数值溢出不能用于创建或删除。 */
    @Test
    @DisplayName("模拟响应：拒绝宽泛过滤值、未知枚举和溢出编码写入")
    void invalidFieldsNeverWrite() {
        List<Consumer<AclMetadata>> invalid = List.of(acl -> acl.setResourceType("ANY"), acl -> acl.setResourceType("invalid"),
            acl -> acl.setPatternType((int) PatternType.MATCH.code()), acl -> acl.setPatternType((int) PatternType.ANY.code()),
            acl -> acl.setPatternType(258), acl -> acl.setOperation(259), acl -> acl.setOperation(-1), acl -> acl.setOperation(0),
            acl -> acl.setOperation((int) AclOperation.ANY.code()), acl -> acl.setPermissionType("ANY"),
            acl -> acl.setPermissionType("invalid"), acl -> acl.setPrincipal("alice"), acl -> acl.setPrincipal("User:"),
            acl -> acl.setResourceName(" "), acl -> acl.setHost(" "), acl -> acl.setActions(List.of("Read")));
        for (var mutation : invalid) {
            var metadata = metadata();
            mutation.accept(metadata);
            assertWritesRejected(metadata);
        }
        Mockito.verifyNoInteractions(client);
    }

    /** 整账号删除选项不会被当作单条规则删除。 */
    @Test
    @DisplayName("模拟响应：拒绝 RocketMQ 整账号删除选项")
    void accountDeletionRejected() {
        DeleteAclRequest request = new DeleteAclRequest();
        request.setMetaData(metadata());
        request.setDeleteAccount(true);
        Assertions.assertThrows(IllegalArgumentException.class, () -> service.deleteAcl(request));
        Mockito.verifyNoInteractions(client);
    }

    /** Broker 未启用 ACL 时必须保留错误，不能返回空列表。 */
    @Test
    @DisplayName("模拟响应：ACL 未启用错误不能伪装成空列表")
    void queryFailurePreservesCause() {
        SecurityDisabledException failure = new SecurityDisabledException("ACL disabled");
        KafkaFutureImpl<java.util.Collection<AclBinding>> future = new KafkaFutureImpl<>();
        future.completeExceptionally(failure);
        Mockito.when(client.describeAcls(Mockito.any(), Mockito.any()).values()).thenReturn(future);
        Assertions.assertSame(failure, Assertions.assertThrows(ExecutionException.class, () -> service.getAllAcls(null)).getCause());
    }

    /** 创建或删除无权限时保留 Broker 异常。 */
    @Test
    @DisplayName("模拟响应：ACL 创建和删除失败保留原始原因")
    void mutationFailuresPreserveCause() {
        var failure = new ClusterAuthorizationException("denied");
        KafkaFutureImpl<Void> create = new KafkaFutureImpl<>();
        create.completeExceptionally(failure);
        KafkaFutureImpl<java.util.Collection<AclBinding>> delete = new KafkaFutureImpl<>();
        delete.completeExceptionally(failure);
        Mockito.when(client.createAcls(Mockito.anyCollection(), Mockito.any()).all()).thenReturn(create);
        Mockito.when(client.deleteAcls(Mockito.anyCollection(), Mockito.any()).all()).thenReturn(delete);
        CreateAclRequest createRequest = new CreateAclRequest();
        createRequest.setMetaData(metadata());
        DeleteAclRequest deleteRequest = new DeleteAclRequest();
        deleteRequest.setMetaData(metadata());
        Assertions.assertSame(failure, Assertions.assertThrows(ExecutionException.class, () -> service.createAcl(createRequest)).getCause());
        Assertions.assertSame(failure, Assertions.assertThrows(ExecutionException.class, () -> service.deleteAcl(deleteRequest)).getCause());
    }

    /** 超时明确失败，中断标志被恢复。 */
    @Test
    @DisplayName("模拟响应：ACL 等待超时和中断明确失败")
    void boundedWaitAndInterrupt() throws Exception {
        KafkaFuture<java.util.Collection<AclBinding>> future = Mockito.mock(KafkaFuture.class);
        Mockito.when(client.describeAcls(Mockito.any(), Mockito.any()).values()).thenReturn(future);
        Mockito.when(future.get(10000, TimeUnit.MILLISECONDS)).thenThrow(new TimeoutException()).thenThrow(new InterruptedException());
        Assertions.assertThrows(TimeoutException.class, () -> service.getAllAcls(null));
        try {
            Assertions.assertThrows(InterruptedException.class, () -> service.getAllAcls(null));
            Assertions.assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    /** 缺失查询响应不能伪造成功。 */
    @Test
    @DisplayName("模拟响应：ACL 响应缺失明确失败")
    void missingResponseFails() {
        Mockito.when(client.describeAcls(Mockito.any(), Mockito.any()).values()).thenReturn(KafkaFuture.completedFuture(null));
        Assertions.assertThrows(IllegalStateException.class, () -> service.getAllAcls(null));
    }

    private void assertWritesRejected(AclMetadata metadata) {
        CreateAclRequest create = new CreateAclRequest();
        create.setMetaData(metadata);
        DeleteAclRequest delete = new DeleteAclRequest();
        delete.setMetaData(metadata);
        Assertions.assertThrows(IllegalArgumentException.class, () -> service.createAcl(create));
        Assertions.assertThrows(IllegalArgumentException.class, () -> service.deleteAcl(delete));
    }

    private AclMetadata metadata() {
        AclMetadata metadata = new AclMetadata();
        metadata.setResourceType("TOPIC");
        metadata.setResourceName("orders");
        metadata.setPatternType((int) PatternType.LITERAL.code());
        metadata.setPrincipal("User:alice");
        metadata.setHost("*");
        metadata.setOperation((int) AclOperation.READ.code());
        metadata.setPermissionType("ALLOW");
        return metadata;
    }

    private AclBinding binding(String resource, PatternType pattern, AclOperation operation, AclPermissionType permission) {
        return new AclBinding(new ResourcePattern(ResourceType.TOPIC, resource, pattern),
            new AccessControlEntry("User:alice", "*", operation, permission));
    }

    private AclBinding binding(AclMetadata metadata) {
        return new AclBinding(new ResourcePattern(ResourceType.valueOf(metadata.getResourceType()), metadata.getResourceName(),
            PatternType.fromCode(metadata.getPatternType().byteValue())), new AccessControlEntry(metadata.getPrincipal(), metadata.getHost(),
            AclOperation.fromCode(metadata.getOperation().byteValue()), AclPermissionType.valueOf(metadata.getPermissionType())));
    }
}
