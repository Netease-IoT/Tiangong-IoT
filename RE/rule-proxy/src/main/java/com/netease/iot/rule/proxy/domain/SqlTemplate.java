package com.netease.iot.rule.proxy.domain;

import java.io.Serializable;
import java.util.List;

public class SqlTemplate implements Serializable {
    private int id;
    private Long parentId;
    private String type;
    private String sql;
    private List<SqlTemplate> children;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setChildren(List<SqlTemplate> children) {
        this.children = children;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public List<SqlTemplate> getChildren() {
        return children;
    }
}
