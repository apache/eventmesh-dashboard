package org.apache.eventmesh.dashboard.core.message.operate;

import org.apache.eventmesh.dashboard.core.function.SDK.config.CreateSDKConfig;
import org.apache.eventmesh.dashboard.core.message.model.ConsumerDTO;

import java.util.ArrayList;
import java.util.List;


public abstract class AbstractConsumerOperate<CL, C extends CreateSDKConfig>
    extends AbstractMessageOperate<CL, C, ConsumerDTO> implements ConsumerOperate {

    private List<Object> data = new ArrayList<>();

    private int tate;


    public List<Object> pull() {
        synchronized (this.data) {
            List<Object> data = this.data;
            this.data = new ArrayList<>();
            return data;
        }
    }

}
