package com.netease.iot.rule.proxy.model;

public class JobDeleteMessage extends BaseMessage {

    private String flinkJobId;

    public JobDeleteMessage(String product, String jobName, String flinkJobId) {
        super(product, jobName);
        this.flinkJobId = flinkJobId;
    }

    public String getFlinkJobId() {
        return flinkJobId;
    }

    public void setFlinkJobId(String flinkJobId) {
        this.flinkJobId = flinkJobId;
    }
}
