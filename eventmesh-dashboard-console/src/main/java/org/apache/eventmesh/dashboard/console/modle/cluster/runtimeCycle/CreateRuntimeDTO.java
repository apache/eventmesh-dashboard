package org.apache.eventmesh.dashboard.console.modle.cluster.runtimeCycle;

import org.apache.eventmesh.dashboard.console.modle.ClusterIdDTO;

import lombok.Data;

@Data
public class CreateRuntimeDTO  extends ClusterIdDTO {

    private int runtimeNum;

}
