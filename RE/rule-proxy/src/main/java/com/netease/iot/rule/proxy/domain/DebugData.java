package com.netease.iot.rule.proxy.domain;


public class DebugData {

    private String jobId;
    private String tableName;
    private String data;

    public DebugData(String jobId, String tableName, String data) {
        this.jobId = jobId;
        this.tableName = tableName;
        this.data = data;
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

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DebugData that = (DebugData) o;

        if (!jobId.equals(that.jobId)) {
            return false;
        }
        if (!tableName.equals(that.tableName)) {
            return false;
        }

        return data != null ? data.equals(that.data) : that.data == null;
    }

    @Override
    public int hashCode() {
        int result = jobId.hashCode();
        result = 31 * result + tableName.hashCode();
        result = 31 * result + (data != null ? data.hashCode() : 0);
        return result;
    }
}
