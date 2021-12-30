package com.netease.iot.rule.proxy.domain;

import java.sql.Timestamp;

public class SyntaxCheck {
    private String jobId;
    private String userId;
    private String rawSql;
    private String tableMessage;
    private Timestamp updateTime;

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTableMessage() {
        return tableMessage;
    }

    public void setTableMessage(String tableMessage) {
        this.tableMessage = tableMessage;
    }

    public String getRawSql() {
        return rawSql;
    }

    public void setRawSql(String rawSql) {
        this.rawSql = rawSql;
    }

    public Timestamp getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Timestamp updateTime) {
        this.updateTime = updateTime;
    }
}
