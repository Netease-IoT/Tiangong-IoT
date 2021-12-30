package com.netease.iot.rule.proxy.model;

import java.util.List;

public class JobSaveDataMessage09 extends JobSyntaxCheckMessage09 {
    private String tableName;
    private String data;
    private String uniqueId;

    public JobSaveDataMessage09(String project, String jobName, String sql, List<byte[]> jarFiles,
                                String tableName, String data, String uniqueId, String resourcePath, String flinkPath) {
        super(project, jobName, sql, jarFiles, resourcePath, flinkPath);
        this.tableName = tableName;
        this.data = data;
        this.uniqueId = uniqueId;
    }

    public String getTableName() {
        return tableName;
    }

    public String getData() {
        return data;
    }

    public String getUniqueId() {
        return uniqueId;
    }
}
