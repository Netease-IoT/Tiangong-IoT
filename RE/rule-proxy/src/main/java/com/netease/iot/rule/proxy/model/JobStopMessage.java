package com.netease.iot.rule.proxy.model;

public class JobStopMessage extends BaseMessage {

    public JobStopMessage(String product, String jobName) {
        super(product, jobName);
    }
}
