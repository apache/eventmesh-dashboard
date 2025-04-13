package org.apache.eventmesh.dashboard.console.modle.vo;

import org.apache.eventmesh.dashboard.console.modle.ClusterIdDTO;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RuntimeIdDTO extends ClusterIdDTO {

    private Long runtimeId;

}
