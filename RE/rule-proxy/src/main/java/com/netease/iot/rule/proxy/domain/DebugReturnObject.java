package com.netease.iot.rule.proxy.domain;


public class DebugReturnObject {
    private String tableName;
    private String data;
    private String tableSchema;

    public DebugReturnObject(String tableName, String data, String tableSchema) {
        this.tableName = tableName;
        this.data = data;
        this.tableSchema = tableSchema;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getTableSchema() {
        return tableSchema;
    }

    public void setTableSchema(String tableSchema) {
        this.tableSchema = tableSchema;
    }
}
