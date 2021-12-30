package com.netease.iot.rule.proxy.domain;

import java.io.Serializable;

public class Server implements Serializable {

    private Integer serverId;
    private String akkaPath;
    private String serverName;
    private String version;
    private Integer versionStatus;

    public Integer getServerId() {
        return serverId;
    }

    public void setServerId(Integer serverId) {
        this.serverId = serverId;
    }

    public String getAkkaPath() {
        return akkaPath;
    }

    public void setAkkaPath(String akkaPath) {
        this.akkaPath = akkaPath;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Integer getVersionStatus() {
        return versionStatus;
    }

    public void setVersionStatus(Integer versionStatus) {
        this.versionStatus = versionStatus;
    }
}
