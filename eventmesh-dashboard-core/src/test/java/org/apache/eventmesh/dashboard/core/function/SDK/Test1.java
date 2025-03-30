package org.apache.eventmesh.dashboard.core.function.SDK;

import org.apache.eventmesh.dashboard.common.util.ClasspathScanner;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

public class Test1 {




    @Test
    public void ClasspathScannerTest() throws Exception {
        Set<Class<?>> interfaceSet = new HashSet<>();
        interfaceSet.add(SDKOperation.class);
        ClasspathScanner classpathScanner = ClasspathScanner.builder().base(SDKManage.class).subPath("/operation").interfaceSet(interfaceSet).build();
        List<Class<?>> classList = classpathScanner.getClazz();
        classList.size();
    }

    @Test
    public void sdkManageTest() throws Exception {
        SDKManage.getInstance();
    }
}
