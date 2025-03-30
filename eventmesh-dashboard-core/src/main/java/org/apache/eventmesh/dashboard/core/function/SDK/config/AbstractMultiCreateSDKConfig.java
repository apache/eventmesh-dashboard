package org.apache.eventmesh.dashboard.core.function.SDK.config;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class AbstractMultiCreateSDKConfig extends AbstractCreateSDKConfig {

    private List<NetAddress> netAddresseList = new CopyOnWriteArrayList<>();

    private List<NetAddress> metaAddressList = new CopyOnWriteArrayList<>();

    public void addNetAddress(NetAddress netAddress) {
        this.netAddresseList.add(netAddress);
    }

    public void removeNetAddress(NetAddress netAddress) {
        this.netAddresseList.remove(netAddress);
    }

    public void addMetaAddress(NetAddress netAddress) {
        this.metaAddressList.add(netAddress);
    }

    public void removeMetaAddress(NetAddress netAddress) {
        this.metaAddressList.remove(netAddress);
    }

    private String doUniqueKey(List<NetAddress> netAddresseList) {
        StringBuffer sb = new StringBuffer();
        netAddresseList.forEach(netAddress -> {
            sb.append(netAddress.doUniqueKey());
            sb.append(";");
        });
        return sb.toString();
    }

    @Override
    public String doUniqueKey() {
        return this.doUniqueKey(this.netAddresseList);
    }

    public String doUniqueKeyByMeta() {
        return this.doUniqueKey(this.metaAddressList);
    }


    public String[] getNetAddressesByMeta() {
        return this.getNetAddresses(this.metaAddressList);
    }

    public String[] getNetAddresses() {
        return this.getNetAddresses(this.netAddresseList);
    }

    private String[] getNetAddresses(List<NetAddress> netAddresseList) {
        List<String> netAddresses = new ArrayList<>();
        netAddresseList.forEach(netAddress -> {
            netAddresses.add(netAddress.doUniqueKey());
        });
        return netAddresses.toArray(new String[netAddresses.size()]);
    }
}
