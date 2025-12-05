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


package org.apache.eventmesh.dashboard.common.model;

import org.apache.eventmesh.dashboard.common.enums.ClusterFramework;
import org.apache.eventmesh.dashboard.common.enums.MetadataType;
import org.apache.eventmesh.dashboard.common.enums.ReplicationDimension;

import java.util.ArrayList;
import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClusterSyncMetadata {

    public static final ClusterSyncMetadata EMPTY_OBJECT = new ClusterSyncMetadata(new ArrayList<>(), ReplicationDimension.NOT, ClusterFramework.NOT);

    public static List<MetadataType> TEST_ONE = new ArrayList<>();

    public static List<MetadataType> META = new ArrayList<>();

    public static List<MetadataType> STORAGE = new ArrayList<>();

    public static List<MetadataType> LANTERN = new ArrayList<>();

    public static List<MetadataType> AUTH = new ArrayList<>();

    static {

        TEST_ONE.add(MetadataType.TOPIC);

        META.add(MetadataType.RUNTIME);

        //STORAGE.add(MetadataType.CONFIG);
        STORAGE.add(MetadataType.TOPIC);
        //STORAGE.add(MetadataType.GROUP);
        //STORAGE.add(MetadataType.NET_CONNECT);
        //STORAGE.add(MetadataType.GROUP_MEMBER);

        AUTH.add(MetadataType.USER);
        AUTH.add(MetadataType.ACL);

    }

    private List<MetadataType> metadataTypeList;

    private ReplicationDimension replicationDimension;

    private ClusterFramework clusterFramework;


}
