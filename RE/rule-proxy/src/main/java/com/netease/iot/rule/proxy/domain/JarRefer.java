package com.netease.iot.rule.proxy.domain;

import java.io.Serializable;

public class JarRefer implements Serializable {
    private String jobId;
    private String jarId;
    private int type;

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getJarId() {
        return jarId;
    }

    public void setJarId(String jarId) {
        this.jarId = jarId;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
}
