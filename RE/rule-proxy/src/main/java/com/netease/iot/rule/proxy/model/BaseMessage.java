package com.netease.iot.rule.proxy.model;


import com.netease.iot.rule.proxy.util.CommonUtil;

public class BaseMessage extends ProductMessage {

    //TODO this can be a parameters map to avoid modify this class frequently,
    //cause every time BaseMessage modified old server become incompatible.
    public String clusterUrl;
    public String clusterPort;
    public String clusterWebUi;
    public boolean isHA;
    public String appId;

    public final String jobName;

    public BaseMessage(String product, String jobName) {
        this(product, jobName, null, null, null);
    }

    public BaseMessage(String product, String jobName, String clusterUrl, String clusterPort, String clusterWebUi) {
        super(product);
        this.jobName = jobName;
        this.clusterUrl = clusterUrl;
        this.clusterPort = clusterPort;
        this.clusterWebUi = clusterWebUi;
    }

    @Override
    public String toString() {
        return "(" + CommonUtil.getRepresentString(product, jobName) + "-" + uuid + ")";
    }

    public void setClusterUrl(String url) {
        this.clusterUrl = url;
    }

    public String getClusterUrl() {
        return clusterUrl;
    }

    public void setClusterPort(String port) {
        this.clusterPort = port;
    }

    public String getClusterPort() {
        return clusterPort;
    }

    public String getClusterWebUi() {
        return clusterWebUi;
    }

    public void setClusterWebUi(String clusterWebUi) {
        this.clusterWebUi = clusterWebUi;
    }

    public boolean isHA() {
        return isHA;
    }

    public void setHA(boolean ha) {
        isHA = ha;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }
}
