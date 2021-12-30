package com.netease.iot.rule.proxy.metadata;

import java.io.Serializable;


public class FlinkJob implements Serializable {
    private String jobId;
    private String flinkJobId;
    private String savePointPath;
    private String jarPath;

    public FlinkJob(String jobId, String flinkJobId, String savePointPath, String jarPath) {
        this.jobId = jobId;
        this.flinkJobId = flinkJobId;
        this.savePointPath = savePointPath;
        this.jarPath = jarPath;
    }

    public String getFlinkJobId() {
        return flinkJobId;
    }

    public void setFlinkJobId(String flinkJobId) {
        this.flinkJobId = flinkJobId;
    }

    public String getSavePointPath() {
        return savePointPath;
    }

    public void setSavePointPath(String savePointPath) {
        this.savePointPath = savePointPath;
    }

    public String getJarPath() {
        return jarPath;
    }

    public void setJarPath(String jarPath) {
        this.jarPath = jarPath;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }
}
