package org.apache.eventmesh.dashboard.core.function.SDK.config;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class AbstractMultiCreateSDKConfig extends AbstractCreateSDKConfig {

    private List<NetAddress> netAddresseList = new CopyOnWriteArrayList<>();

    public void addNetAddress(NetAddress netAddress) {
        this.netAddresseList.add(netAddress);
    }

    public void removeNetAddress(NetAddress netAddress) {
        this.netAddresseList.remove(netAddress);
    }

    @Override
    public String doUniqueKey() {
        StringBuffer sb = new StringBuffer();
        netAddresseList.forEach(netAddress -> {
            sb.append(netAddress.doUniqueKey());
            sb.append(";");
        });
        return sb.toString();
    }

    public String[] getNetAddresses() {
        List<String> netAddresses = new ArrayList<>();
        netAddresseList.forEach(netAddress -> {
            netAddresses.add(netAddress.doUniqueKey());
        });
        return netAddresses.toArray(new String[netAddresses.size()]);
    }
}
