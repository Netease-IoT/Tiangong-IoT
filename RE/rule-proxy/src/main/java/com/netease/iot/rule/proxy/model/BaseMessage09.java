package com.netease.iot.rule.proxy.model;


import com.netease.iot.rule.proxy.util.CommonUtil;

public class BaseMessage09 extends ProductMessage {
    public String jobId;
    public String resourcePath;
    public String flinkPath;

    public final String jobName;

    public BaseMessage09(String product, String jobName, String resourcePath, String flinkPath) {
        super(product);
        this.jobName = jobName;
        this.resourcePath = resourcePath;
        this.flinkPath = flinkPath;
    }

    @Override
    public String toString() {
        return "(" + CommonUtil.getRepresentString(product, jobName) + "-" + uuid + ")";
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getJobName() {
        return jobName;
    }

    public String getResourcePath() {
        return resourcePath;
    }

    public void setResourcePath(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    public String getFlinkPath() {
        return flinkPath;
    }

    public void setFlinkPath(String flinkPath) {
        this.flinkPath = flinkPath;
    }
}
