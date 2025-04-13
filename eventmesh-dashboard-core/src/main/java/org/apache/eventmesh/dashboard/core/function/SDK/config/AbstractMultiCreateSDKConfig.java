package org.apache.eventmesh.dashboard.core.function.SDK.config;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class AbstractMultiCreateSDKConfig extends AbstractCreateSDKConfig {


    private List<NetAddress> netAddresseList = new CopyOnWriteArrayList<>();

    /**
     * TODO
     * TODO
     * TODO 有意义？
     *  设定次字段的时候，是为了解决 kafka 老版本操作的时候是 需要操作zk。 目前还没解决这个问题
     */
    private List<NetAddress> metaAddressList = new CopyOnWriteArrayList<>();


    public boolean isNullAddress() {
        return netAddresseList.isEmpty();
    }

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
    protected String uniqueKey() {
        return "m_";
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
