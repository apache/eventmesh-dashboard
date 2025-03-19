package org.apache.eventmesh.dashboard.core.function.SDK.config;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode
public class NetAddress {


    public static NetAddress create(String address, int port) {
        NetAddress netAddress = new NetAddress();
        netAddress.address = address;
        netAddress.port = port;
        return netAddress;
    }

    private String address;

    private Integer port;


    public String doUniqueKey() {
        return this.address + ":" + this.port;
    }

}
