package org.apache.eventmesh.dashboard.console.entity.base;


import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class BaseOrganizationEntity extends BaseIdEntity {


    private Long organizationId;


}
