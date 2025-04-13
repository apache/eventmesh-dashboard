package org.apache.eventmesh.dashboard.console.mapper.deploy;

import org.apache.eventmesh.dashboard.console.entity.cases.PortEntity;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PortMapper {



    void insertPort(PortEntity portEntity);

    PortEntity lockPort(PortEntity portEntity);

    void updatePort(PortEntity portEntity);
}
