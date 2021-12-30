package com.netease.iot.rule.proxy.util;


import com.netease.iot.rule.proxy.model.ResultMessage;

/**
 * @author xiaojianhua
 */
public class ResultMessageUtils {

    public static ResultMessage cookSuccessMessage(Object data) {
        return new ResultMessage(0, data, "success");
    }

    public static ResultMessage cookFailedMessage(String failedMsg) {
        return new ResultMessage(-1, null, failedMsg);
    }
}
