package com.netease.iot.rule.proxy.model;

import com.google.common.base.MoreObjects;
import com.google.gson.Gson;

import java.io.Serializable;

public class ResultMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Gson GSON = new Gson();

    private int code;
    private Object data;
    private String msg;

    public ResultMessage(boolean success, String msg) {
        this(success ? 0 : -1, "", msg);
    }

    public ResultMessage(int code, Object data, String msg) {
        this.code = code;
        this.data = data;
        this.msg = msg;
    }

    public String getMsg() {
        return msg;
    }

    public boolean isSuccess() {
        return code == 0;
    }

    public void setData(Object d) {
        this.data = d;
    }

    public Object getData() {
        return this.data;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("success", isSuccess())
                .add("data", data != null ? GSON.toJson(data) : "")
                .add("msg", msg)
                .toString();
    }
}
