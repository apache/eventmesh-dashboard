package org.apache.eventmesh.dashboard.console.function.health.check;

import org.apache.eventmesh.dashboard.console.function.health.check.config.HealthCheckObjectConfig;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import lombok.Getter;
import lombok.Setter;

public abstract class AbstractTopicHealthCheckService extends AbstractHealthCheckService {

    @Setter
    @Getter
    private Integer offset = 0;

    @Setter
    @Getter
    private Integer queue = 0;

    private AtomicLong atomicLong = new AtomicLong();

    public AbstractTopicHealthCheckService(
        HealthCheckObjectConfig healthCheckObjectConfig) {
        super(healthCheckObjectConfig);
    }

    protected byte[] messageContext() {
        return ("{ 'uid': " + atomicLong.incrementAndGet() + "}").getBytes();
    }

    protected boolean isCurrentValue(String context) {
        if (Objects.isNull(context)) {
            return false;
        }
        JSONObject json = JSON.parseObject(context);
        if (Objects.isNull(json.get("uid"))) {
            return false;
        }
        return Objects.equals(Integer.valueOf(json.get("uid").toString()), atomicLong.get());
    }

    protected boolean isCurrentValue(byte[] context) {
        if (Objects.isNull(context)) {
            return false;
        }
        return this.isCurrentValue(new String(context));
    }

}
