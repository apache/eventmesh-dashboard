package org.apache.eventmesh.dashboard.console.modle.deploy;

import org.apache.eventmesh.dashboard.console.modle.ClusterIdDTO;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class GetPortsDO extends ClusterIdDTO {

    private Integer portNum;

}
