package org.apache.eventmesh.dashboard.console.mapstruct.deploy;

import org.apache.eventmesh.dashboard.console.entity.cases.CaseEntity;
import org.apache.eventmesh.dashboard.console.modle.deploy.cases.QueryCaseByObjectTypeDTO;

import org.mapstruct.factory.Mappers;

/**
 *
 */
public interface CaseControllerMapper {

    CaseControllerMapper INSTANCE = Mappers.getMapper(CaseControllerMapper.class);


    CaseEntity queryCaseByObjectType(QueryCaseByObjectTypeDTO queryCaseByObjectTypeDTO);

}
