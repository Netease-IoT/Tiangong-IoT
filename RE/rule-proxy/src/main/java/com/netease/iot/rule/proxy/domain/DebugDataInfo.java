package com.netease.iot.rule.proxy.domain;

import java.sql.Timestamp;

public class DebugDataInfo {
    private String jobId;
    private String tableName;
    private String lastTopic;
    private Timestamp updateTime;

    public DebugDataInfo() {
    }

    public DebugDataInfo(String jobId, String tableName, String lastTopic) {
        this.jobId = jobId;
        this.tableName = tableName;
        this.lastTopic = lastTopic;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getLastTopic() {
        return lastTopic;
    }

    public void setLastTopic(String lastTopic) {
        this.lastTopic = lastTopic;
    }

    public Timestamp getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Timestamp updateTime) {
        this.updateTime = updateTime;
    }
}
