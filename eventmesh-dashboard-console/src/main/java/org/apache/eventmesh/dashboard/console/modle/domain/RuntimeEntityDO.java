package org.apache.eventmesh.dashboard.console.modle.domain;

import org.apache.eventmesh.dashboard.console.entity.cluster.RuntimeEntity;
import org.apache.eventmesh.dashboard.console.entity.function.ConfigEntity;
import org.apache.eventmesh.dashboard.core.cluster.RuntimeBaseDO;

import lombok.Data;
import lombok.EqualsAndHashCode;


@Data
@EqualsAndHashCode(callSuper = false)
public class RuntimeEntityDO extends RuntimeBaseDO<RuntimeEntity, Object, ConfigEntity> {


}
