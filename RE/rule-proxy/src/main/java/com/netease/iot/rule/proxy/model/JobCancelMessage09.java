package com.netease.iot.rule.proxy.model;


import com.netease.iot.rule.proxy.metadata.FlinkJob09;


public class JobCancelMessage09 extends BaseMessage09 {
    private FlinkJob09 job;
    private String webUI;

    public JobCancelMessage09(String project, String jobName, String resourcePath, String webUI, String flinkPath) {
        super(project, jobName, resourcePath, flinkPath);
        this.webUI = webUI;
    }

    public FlinkJob09 getJob() {
        return job;
    }

    public void setJob(FlinkJob09 job) {
        this.job = job;
    }

    public String getWebUI() {
        return webUI;
    }

    public void setWebUI(String webUI) {
        this.webUI = webUI;
    }
}
