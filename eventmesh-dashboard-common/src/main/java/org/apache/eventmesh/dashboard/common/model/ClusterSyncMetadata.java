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

        STORAGE.add(MetadataType.CONFIG);
        STORAGE.add(MetadataType.TOPIC);
        STORAGE.add(MetadataType.GROUP);
        STORAGE.add(MetadataType.NET_CONNECT);
        STORAGE.add(MetadataType.SUBSCRIBER);

        AUTH.add(MetadataType.USER);
        AUTH.add(MetadataType.ACL);

    }

    private List<MetadataType> metadataTypeList;

    private ReplicationDimension replicationDimension;

    private ClusterFramework clusterFramework;


}
