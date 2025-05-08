/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


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
