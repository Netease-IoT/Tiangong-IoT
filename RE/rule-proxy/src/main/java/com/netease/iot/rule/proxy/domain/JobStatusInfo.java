package com.netease.iot.rule.proxy.domain;


public class JobStatusInfo {
    private String jobId;
    private boolean isOnline;
    private boolean hasModify;
    private Integer versionStatus;

    public JobStatusInfo(String jobId, boolean isOnline, boolean hasModify, Integer versionStatus) {
        this.jobId = jobId;
        this.isOnline = isOnline;
        this.hasModify = hasModify;
        this.versionStatus = versionStatus;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }

    public boolean isHasModify() {
        return hasModify;
    }

    public void setHasModify(boolean hasModify) {
        this.hasModify = hasModify;
    }

    public Integer getVersionStatus() {
        return versionStatus;
    }

    public void setVersionStatus(Integer versionStatus) {
        this.versionStatus = versionStatus;
    }
}
