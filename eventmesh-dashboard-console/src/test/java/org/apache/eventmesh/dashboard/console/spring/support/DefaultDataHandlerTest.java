package org.apache.eventmesh.dashboard.console.spring.support;


import org.apache.eventmesh.dashboard.console.model.domain.ClusterMetadataDomainTest;

import org.junit.Test;

public class DefaultDataHandlerTest {

    private ClusterMetadataDomainTest clusterMetadataDomainTest = new ClusterMetadataDomainTest();

    @Test
    public void test() {
        clusterMetadataDomainTest.clusterMetadataDomain.rootClusterDHO();
    }
}
