package org.apache.eventmesh.dashboard.console.entity.cases;


import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class ResourcesConfigEntity extends CaseEntity {

    private Integer cpuNum;

    private Integer memNum;

    private Integer diskNum;

    private Integer gpuNum;


}
