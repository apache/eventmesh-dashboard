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

package org.apache.eventmesh.dashboard.console.baseid;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.eventmesh.dashboard.console.controller.cluster.AclController;
import org.apache.eventmesh.dashboard.console.entity.cluster.AclEntity;
import org.apache.eventmesh.dashboard.console.service.cluster.AclService;


import org.junit.Test;

public class ControllerFailureTest {

    @Test
    public void selectAclReturnsTheStoredEntity() {
        AclService service = mock(AclService.class);
        AclController controller = new AclController();
        AclEntity request = new AclEntity();
        AclEntity stored = new AclEntity();
        when(service.selectById(request)).thenReturn(stored);
        setAclService(controller, service);

        AclEntity result = controller.selectAcl(request);

        assertSame(stored, result);
        verify(service).selectById(request);
    }

    private void setAclService(AclController controller, AclService service) {
        try {
            java.lang.reflect.Field field = AclController.class.getDeclaredField("aclService");
            field.setAccessible(true);
            field.set(controller, service);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
