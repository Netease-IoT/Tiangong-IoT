package com.netease.iot.rule.proxy.model;


import com.netease.iot.rule.proxy.metadata.FlinkJob09;


public class JobSuspendMessage09 extends BaseMessage09 {
    private FlinkJob09 job;

    public JobSuspendMessage09(String project, String jobName, String resourcePath, String flinkPath) {
        super(project, jobName, resourcePath, flinkPath);
    }

    public FlinkJob09 getJob() {
        return job;
    }

    public void setJob(FlinkJob09 job) {
        this.job = job;
    }
}