package com.netease.iot.rule.proxy.metadata;

import java.io.Serializable;


public class FlinkJob09 implements Serializable {

    private static final long serialVersionUID = 1L;
    private String jobId;
    private String flinkJobId;
    private String savePointPath;
    private String jarPath;
    private String applicationId;

    public FlinkJob09(String jobId, String flinkJobId, String savePointPath, String jarPath, String applicationId) {
        this.jobId = jobId;
        this.flinkJobId = flinkJobId;
        this.savePointPath = savePointPath;
        this.jarPath = jarPath;
        this.applicationId = applicationId;
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

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }
}
