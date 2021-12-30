package com.netease.iot.rule.proxy.model;

import java.util.List;

public class FetchDataMessage extends JobSyntaxCheckMessage {
    private String option;
    private boolean isHistory = false;

    public FetchDataMessage(String project, String jobName, String sql, List<byte[]> jarFiles, String option) {
        super(project, jobName, sql, jarFiles);
        this.option = option;
    }

    public String getOption() {
        return option;
    }

    public void setOption(String option) {
        this.option = option;
    }

    public boolean isHistory() {
        return isHistory;
    }

    public void setHistory(boolean history) {
        isHistory = history;
    }
}
