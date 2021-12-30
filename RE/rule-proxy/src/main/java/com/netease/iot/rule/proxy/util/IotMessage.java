package com.netease.iot.rule.proxy.util;

import com.google.common.collect.Maps;

import java.util.Map;


public class IotMessage {

    private static final Map<String, Map<String, String>> MESSAGEMAP = Maps.newHashMap();
    private static final String LANGUAGE_CN = "cn";
    private static final String LANGUAGE_OTHER = "other";

    static {
        Map<String, String> map = Maps.newHashMap();
        map.put(LANGUAGE_OTHER, "json format error");
        map.put(LANGUAGE_CN, "数据格式校验错误");
        MESSAGEMAP.put(IotConstants.RETURN_CODE_JSONERROR, map);

        map = Maps.newHashMap();
        map.put(LANGUAGE_OTHER, "rule parameters error");
        map.put(LANGUAGE_CN, "规则参数异常");
        MESSAGEMAP.put(IotConstants.RETURN_CODE_RULEPARAMETERSERROR, map);

        map = Maps.newHashMap();
        map.put(LANGUAGE_OTHER, "rule source parameters error");
        map.put(LANGUAGE_CN, "规则数据源参数异常");
        MESSAGEMAP.put(IotConstants.RETURN_CODE_SOURCEPARAMETERSERROR, map);

        map = Maps.newHashMap();
        map.put(LANGUAGE_OTHER, "rule sink parameters error");
        map.put(LANGUAGE_CN, "规则目的地参数异常");
        MESSAGEMAP.put(IotConstants.RETURN_CODE_SINKPARAMETERSERROR, map);

        map = Maps.newHashMap();
        map.put(LANGUAGE_OTHER, "rule name repeat error");
        map.put(LANGUAGE_CN, "任务名称重复");
        MESSAGEMAP.put(IotConstants.RETURN_CODE_RULENAMEREPEATERROR, map);

        map = Maps.newHashMap();
        map.put(LANGUAGE_OTHER, "rule save error");
        map.put(LANGUAGE_CN, "任务保存异常");
        MESSAGEMAP.put(IotConstants.RETURN_CODE_RULESAVEERRORERROR, map);

        map = Maps.newHashMap();
        map.put(LANGUAGE_OTHER, "rule not found error");
        map.put(LANGUAGE_CN, "规则不存在");
        MESSAGEMAP.put(IotConstants.RETURN_CODE_RULENOTFOUNDERROR, map);

        map = Maps.newHashMap();
        map.put(LANGUAGE_OTHER, "start rule error");
        map.put(LANGUAGE_CN, "启动任务失败");
        MESSAGEMAP.put(IotConstants.RETURN_CODE_STARTRULEERROR, map);

        map = Maps.newHashMap();
        map.put(LANGUAGE_OTHER, "stop rule error");
        map.put(LANGUAGE_CN, "停止任务失败");
        MESSAGEMAP.put(IotConstants.RETURN_CODE_STOPRULEERROR, map);

        map = Maps.newHashMap();
        map.put(LANGUAGE_OTHER, "delete rule error");
        map.put(LANGUAGE_CN, "删除任务失败");
        MESSAGEMAP.put(IotConstants.RETURN_CODE_DELETERULEERROR, map);

        map = Maps.newHashMap();
        map.put(LANGUAGE_OTHER, "get databases error");
        map.put(LANGUAGE_CN, "获取数据库失败");
        MESSAGEMAP.put(IotConstants.RETURN_CODE_GETDBLISTERROR, map);

        map = Maps.newHashMap();
        map.put(LANGUAGE_OTHER, "get tables error");
        map.put(LANGUAGE_CN, "获取数据库表失败");
        MESSAGEMAP.put(IotConstants.RETURN_CODE_GETTABLELISTERROR, map);

        map = Maps.newHashMap();
        map.put(LANGUAGE_OTHER, "get rds meta error");
        map.put(LANGUAGE_CN, "获取rds元数据信息失败");
        MESSAGEMAP.put(IotConstants.RETURN_CODE_GETRDSMETAERROR, map);

        map = Maps.newHashMap();
        map.put(LANGUAGE_OTHER, "lack ofr resource error");
        map.put(LANGUAGE_CN, "资源不足");
        MESSAGEMAP.put(IotConstants.RETURN_CODE_LACKOFRESOURCEERROR, map);

        map = Maps.newHashMap();
        map.put(LANGUAGE_OTHER, "operator error");
        map.put(LANGUAGE_CN, "操作异常");
        MESSAGEMAP.put(IotConstants.RETURN_CODE_OPERATORERROR, map);

        map = Maps.newHashMap();
        map.put(LANGUAGE_OTHER, "get rule alarm instances error");
        map.put(LANGUAGE_CN, "获取报警实例异常");
        MESSAGEMAP.put(IotConstants.RETURN_CODE_GETRULEALARMINSTANCESERROR, map);
    }

    public static String get(String code, String language) {
        Map<String, String> map = MESSAGEMAP.get(code);
        if (language.equals(IotConstants.HEAD_LANGUAGE_TYPE_CN)) {
            return map.get(LANGUAGE_CN);
        } else {
            return map.get(LANGUAGE_OTHER);
        }
    }
}
