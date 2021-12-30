package com.netease.iot.rule.proxy.domain;

public class ClusterConfig {

    private Integer clusterId;
    private String clusterName;
    private String resourcePath;
    private String flinkPath;
    private String cpkPath;
    private String yarnPath;
    private String yarnName;
    private Integer isPublic;
    private String remark;

    public Integer getClusterId() {
        return clusterId;
    }

    public void setClusterId(Integer clusterId) {
        this.clusterId = clusterId;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getResourcePath() {
        return resourcePath;
    }

    public void setResourcePath(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    public String getYarnPath() {
        return yarnPath;
    }

    public void setYarnPath(String yarnPath) {
        this.yarnPath = yarnPath;
    }

    public String getYarnName() {
        return yarnName;
    }

    public void setYarnName(String yarnName) {
        this.yarnName = yarnName;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Integer getIsPublic() {
        return isPublic;
    }

    public void setIsPublic(Integer isPublic) {
        this.isPublic = isPublic;
    }

    public String getFlinkPath() {
        return flinkPath;
    }

    public void setFlinkPath(String flinkPath) {
        this.flinkPath = flinkPath;
    }

    public String getCpkPath() {
        return cpkPath;
    }

    public void setCpkPath(String cpkPath) {
        this.cpkPath = cpkPath;
    }
}
