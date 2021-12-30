package com.netease.iot.rule.proxy.model;

import java.util.List;

public class FetchDataMessage09 extends JobSyntaxCheckMessage09 {
    private String option;
    private boolean isHistory = false;

    public FetchDataMessage09(String project, String jobName, String sql, List<byte[]> jarFiles, String option, String resourcePath, String flinkPath) {
        super(project, jobName, sql, jarFiles, resourcePath, flinkPath);
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
