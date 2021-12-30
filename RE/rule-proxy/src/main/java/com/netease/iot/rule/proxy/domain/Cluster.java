package com.netease.iot.rule.proxy.domain;

import java.io.Serializable;

public class Cluster implements Serializable {
    private Long id;
    private String clusterName;
    private String product;
    private String akkaConfig;
    private String akkaPath;
    private String clusterUrl;
    private String clusterPort;
    private String clusterWebUi;
    private boolean isHA;
    private String appId;
    private String serverDescription;
    private String clusterDescription;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getAkkaConfig() {
        return akkaConfig;
    }

    public void setAkkaConfig(String akkaConfig) {
        this.akkaConfig = akkaConfig;
    }

    public String getAkkaPath() {
        return akkaPath;
    }

    public void setAkkaPath(String akkaPath) {
        this.akkaPath = akkaPath;
    }

    public String getClusterUrl() {
        return clusterUrl;
    }

    public void setClusterUrl(String clusterUrl) {
        this.clusterUrl = clusterUrl;
    }

    public String getClusterPort() {
        return clusterPort;
    }

    public void setClusterPort(String clusterPort) {
        this.clusterPort = clusterPort;
    }

    public String getClusterWebUi() {
        return clusterWebUi;
    }

    public void setClusterWebUi(String clusterWebUi) {
        this.clusterWebUi = clusterWebUi;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public boolean isHA() {
        return isHA;
    }

    public void setHA(boolean HA) {
        isHA = HA;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getServerDescription() {
        return serverDescription;
    }

    public void setServerDescription(String serverDescription) {
        this.serverDescription = serverDescription;
    }

    public String getClusterDescription() {
        return clusterDescription;
    }

    public void setClusterDescription(String clusterDescription) {
        this.clusterDescription = clusterDescription;
    }
}
