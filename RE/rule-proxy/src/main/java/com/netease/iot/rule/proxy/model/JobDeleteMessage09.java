package com.netease.iot.rule.proxy.model;


import com.netease.iot.rule.proxy.metadata.FlinkJob09;

public class JobDeleteMessage09 extends BaseMessage09 {
    private FlinkJob09 job;
    private String cpkPath;

    public JobDeleteMessage09(String product, String jobName, String flinkJobId, String resourcePath, String flinkPath, String cpkPath) {
        super(product, jobName, resourcePath, flinkPath);
        this.cpkPath = cpkPath;
    }

    public FlinkJob09 getJob() {
        return job;
    }

    public void setJob(FlinkJob09 job) {
        this.job = job;
    }

    public String getCpkPath() {
        return cpkPath;
    }

    public void setCpkPath(String cpkPath) {
        this.cpkPath = cpkPath;
    }
}
