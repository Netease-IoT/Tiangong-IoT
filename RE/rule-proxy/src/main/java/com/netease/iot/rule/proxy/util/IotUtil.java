package com.netease.iot.rule.proxy.util;

import java.util.regex.Pattern;


public class IotUtil {

    public static boolean checkString(String value, int size) {
        String regEx = "^[0-9a-zA-Z_]{1," + size + "}$";
        return Pattern.matches(regEx, value);
    }

    public static boolean checkStringSize(String value, int size) {
        String regEx = "^.{0," + size + "}$";
        return Pattern.matches(regEx, value);
    }

    public static boolean checkKafkaVersion(String version) {
        if (version.startsWith("0.8.")) {
            return true;
        } else if (version.startsWith("0.9.")) {
            return true;
        } else if (version.startsWith("0.10.")) {
            return true;
        } else {
            return version.startsWith("0.11.");
        }
    }
}