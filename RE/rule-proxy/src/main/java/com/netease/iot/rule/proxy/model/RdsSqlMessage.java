package com.netease.iot.rule.proxy.model;

import java.io.Serializable;

public class RdsSqlMessage implements Serializable {
    private String url;
    private String database;
    private String username;
    private String password;
    private String tablename;
    private String sql;

    public RdsSqlMessage(String url, String database, String username, String password, String tablename) {
        this.url = url;
        this.database = database;
        this.username = username;
        this.password = password;
        this.tablename = tablename;
    }

    public RdsSqlMessage() {
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getTablename() {
        return tablename;
    }

    public void setTablename(String tablename) {
        this.tablename = tablename;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }
}
