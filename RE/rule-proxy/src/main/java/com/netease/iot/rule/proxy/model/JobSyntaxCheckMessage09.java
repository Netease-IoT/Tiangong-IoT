package com.netease.iot.rule.proxy.model;

import java.util.List;


public class JobSyntaxCheckMessage09 extends BaseMessage09 {
    private final String jobSql;
    private final List<byte[]> jarFiles;

    public JobSyntaxCheckMessage09(String project, String jobName, String sql, List<byte[]> jarFiles, String resourcePath, String flinkPath) {
        super(project, jobName, resourcePath, flinkPath);

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
