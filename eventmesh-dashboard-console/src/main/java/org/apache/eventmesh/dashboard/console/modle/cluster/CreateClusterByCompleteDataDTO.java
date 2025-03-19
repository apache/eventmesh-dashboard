package org.apache.eventmesh.dashboard.console.modle.cluster;

import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class CreateClusterByCompleteDataDTO extends CreateClusterBySimpleDataDTO {


    private List<Object> runtimeCLuster;

    private List<Object> metadataCLuster;

    private List<Object> storageCLuster;
}
