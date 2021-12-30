package com.netease.iot.rule.proxy.domain;

public class MonitorMeta {

    private String jobId;
    private Integer group;
    private String mailList;
    private String receiveType;
    private Integer alarmInterval;
    private Integer threshold;

    public String getReceiveType() {
        return receiveType;
    }

    public void setReceiveType(String receiveType) {
        this.receiveType = receiveType;
    }

    public String getMailList() {
        return mailList;
    }

    public void setMailList(String mailList) {
        this.mailList = mailList;
    }

    public Integer getAlarmInterval() {
        return alarmInterval;
    }

    public void setAlarmInterval(int alarmInterval) {
        this.alarmInterval = alarmInterval;
    }

    public Integer getThreshold() {
        return threshold;
    }

    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }

    public Integer getGroup() {
        return group;
    }

    public void setGroup(int group) {
        this.group = group;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }
}
