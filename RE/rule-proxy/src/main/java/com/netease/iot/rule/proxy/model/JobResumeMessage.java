package com.netease.iot.rule.proxy.model;

import java.util.Properties;


public class JobResumeMessage extends BaseMessage {

    public JobCreateMessage jobCreateMessage;

    public JobResumeMessage(String product, String jobName, String sqlJobDefinition, Properties conf) {
        super(product, jobName);
        jobCreateMessage = new JobCreateMessage(product, jobName, sqlJobDefinition, conf);
    }
}
