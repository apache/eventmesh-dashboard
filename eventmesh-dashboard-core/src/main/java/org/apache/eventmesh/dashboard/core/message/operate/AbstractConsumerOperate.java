package org.apache.eventmesh.dashboard.core.message.operate;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractConsumerOperate extends AbstractMessageOperate implements ConsumerOperate {

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
