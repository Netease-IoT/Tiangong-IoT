package com.netease.iot.rule.proxy.model;

import java.util.List;


public class JobSyntaxCheckMessage extends BaseMessage {
    private final String jobSql;
    private final List<byte[]> jarFiles;

    public JobSyntaxCheckMessage(String project, String jobName, String sql, List<byte[]> jarFiles) {
        super(project, jobName);

        this.jobSql = sql;
        this.jarFiles = jarFiles;
    }

    public List<byte[]> getJarFiles() {
        return jarFiles;
    }

    public String getJobSql() {
        return jobSql;
    }
}
