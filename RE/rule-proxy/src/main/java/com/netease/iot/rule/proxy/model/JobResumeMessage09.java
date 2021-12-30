package com.netease.iot.rule.proxy.model;

import java.util.Properties;


public class JobResumeMessage09 extends BaseMessage09 {

    public JobCreateMessage09 jobCreateMessage;

    public JobResumeMessage09(String product, String jobName, String sqlJobDefinition, Properties conf, String resourcePath, String flinkPath, String cpkPath) {
        super(product, jobName, resourcePath, flinkPath);
        jobCreateMessage = new JobCreateMessage09(product, jobName, sqlJobDefinition, conf, resourcePath, flinkPath, cpkPath);
    }
}
