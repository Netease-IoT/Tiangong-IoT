package com.netease.iot.rule.proxy.message;

import com.alibaba.fastjson.annotation.JSONField;

import java.util.List;

public class AlarmInstanceListMessage  extends SuccessResultMessage {
    private List<String> instances;

    public AlarmInstanceListMessage(String requestId, List<String> instances) {
        super(requestId);
        this.instances = instances;
    }

    public static AlarmInstanceListMessage cook(String requestId, List<String> instances) {
        return new AlarmInstanceListMessage(requestId, instances);
    }

    @JSONField(name = "instances")
    public List<String> getInstances() {
        return instances;
    }

    public void setInstances(List<String> instances) {
        this.instances = instances;
    }
}
