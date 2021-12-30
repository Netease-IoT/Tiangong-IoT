package com.netease.iot.rule.proxy.model;


import com.netease.iot.rule.proxy.metadata.FlinkJob09;

import java.util.LinkedList;
import java.util.Properties;

public class JobCreateMessage09 extends BaseMessage09 {

    public final String sqlJobDefinition;    // UTF-8
    public final Properties conf;
    private LinkedList<byte[]> jarFiles;
    private FlinkJob09 job;
    private boolean startWithSavePoint = false;
    private String cpkPath;

    public JobCreateMessage09(String product,
                              String jobName,
                              String sqlJobDefinition,
                              Properties conf,
                              String resourcePath,
                              String flinkPath,
                              String cpkPath) {
        super(product, jobName, resourcePath, flinkPath);
        this.sqlJobDefinition = sqlJobDefinition;
        this.conf = conf;
        this.cpkPath = cpkPath;
    }

    public LinkedList<byte[]> getJarFiles() {
        return jarFiles;
    }

    public void setJarFiles(LinkedList<byte[]> jarFiles) {
        this.jarFiles = jarFiles;
    }

    public FlinkJob09 getJob() {
        return job;
    }

    public void setJob(FlinkJob09 job) {
        this.job = job;
    }

    public boolean isStartWithSavePoint() {
        return startWithSavePoint;
    }

    public void setStartWithSavePoint(boolean startWithSavePoint) {
        this.startWithSavePoint = startWithSavePoint;
    }

    public Properties getConf() {
        return conf;
    }

    public String getCpkPath() {
        return cpkPath;
    }

    public void setCpkPath(String cpkPath) {
        this.cpkPath = cpkPath;
    }
}
