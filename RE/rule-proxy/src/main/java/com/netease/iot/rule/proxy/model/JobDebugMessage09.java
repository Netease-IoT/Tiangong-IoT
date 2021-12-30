package com.netease.iot.rule.proxy.model;

import java.util.List;
import java.util.Map;

public class JobDebugMessage09 extends JobSyntaxCheckMessage09 {

    private Map<String, String> sourceData;

    public JobDebugMessage09(String product, String jobName, String sql, List<byte[]> jarFiles,
                             Map<String, String> sourceData, String resourcePath, String flinkPath) {
        super(product, jobName, sql, jarFiles, resourcePath, flinkPath);
        this.sourceData = sourceData;
    }

    public Map<String, String> getSourceData() {
        return sourceData;
    }
}
