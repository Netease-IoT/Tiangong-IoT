package com.netease.iot.rule.proxy.model;

import java.util.List;

public class JobSaveDataMessage extends JobSyntaxCheckMessage {
    private String tableName;
    private String data;
    private String uniqueId;

    public JobSaveDataMessage(String project, String jobName, String sql, List<byte[]> jarFiles,
                              String tableName, String data, String uniqueId) {
        super(project, jobName, sql, jarFiles);
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
