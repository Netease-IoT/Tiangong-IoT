package com.netease.iot.rule.proxy.model;


import com.netease.iot.rule.proxy.metadata.FlinkJob;


public class JobSuspendMessage extends BaseMessage {
    private FlinkJob job;

    public JobSuspendMessage(String project, String jobName) {
        super(project, jobName);
    }

    public FlinkJob getJob() {
        return job;
    }

    public void setJob(FlinkJob job) {
        this.job = job;
    }
}
