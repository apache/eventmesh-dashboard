package org.apache.eventmesh.dashboard.console.service.deploy;

import org.apache.eventmesh.dashboard.console.modle.deploy.GetPortsDO;

import java.util.List;

public interface PortService {


    List<String> getPorts(GetPortsDO getPortsDO);

}
