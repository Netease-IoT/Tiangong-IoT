package com.netease.iot.rule.proxy.model;


import com.netease.iot.rule.proxy.metadata.FlinkJob;

import java.util.LinkedList;
import java.util.Properties;

public class JobCreateMessage extends BaseMessage {

    public final String sqlJobDefinition;    // UTF-8
    public final Properties conf;
    private LinkedList<byte[]> jarFiles;
    private FlinkJob job;
    private boolean startWithSavePoint = false;

    public JobCreateMessage(String product, String jobName, String sqlJobDefinition, Properties conf) {
        super(product, jobName);
        this.conf = conf;
        this.sqlJobDefinition = sqlJobDefinition;
    }

    public LinkedList<byte[]> getJarFiles() {
        return jarFiles;
    }

    public void setJarFiles(LinkedList<byte[]> jarFiles) {
        this.jarFiles = jarFiles;
    }

    public FlinkJob getJob() {
        return job;
    }

    public void setJob(FlinkJob job) {
        this.job = job;
    }

    public boolean isStartWithSavePoint() {
        return startWithSavePoint;
    }

    public void setStartWithSavePoint(boolean startWithSavePoint) {
        this.startWithSavePoint = startWithSavePoint;
    }
}
