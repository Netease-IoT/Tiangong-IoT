package com.netease.iot.rule.proxy.model;

import java.util.List;
import java.util.Map;


public class JobDebugMessage extends JobSyntaxCheckMessage {

    private Map<String, String> sourceData;

    public JobDebugMessage(String product, String jobName, String sql, List<byte[]> jarFiles,
                           Map<String, String> sourceData) {
        super(product, jobName, sql, jarFiles);
        this.sourceData = sourceData;
    }

    public Map<String, String> getSourceData() {
        return sourceData;
    }
}
