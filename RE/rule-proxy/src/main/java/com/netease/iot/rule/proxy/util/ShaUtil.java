package com.netease.iot.rule.proxy.util;

import java.security.MessageDigest;


public class ShaUtil {
    /**
     * get String's sha256
     */
    public static String getSHA256StrJava(String str) {
        if (null == str) {
            return str;
        }
        MessageDigest messageDigest;
        String encodeStr = "";
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(str.getBytes("UTF-8"));
            encodeStr = byte2Hex(messageDigest.digest());
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
        return encodeStr;
    }

    /**
     * byte to hexadecimal
     */
    private static String byte2Hex(byte[] bytes) {
        StringBuffer stringBuffer = new StringBuffer();
        String temp = null;
        for (int i = 0; i < bytes.length; i++) {
            temp = Integer.toHexString(bytes[i] & 0xFF);
            if (temp.length() == 1) {
                stringBuffer.append("0");
            }
            stringBuffer.append(temp);
        }
        return stringBuffer.toString();
    }
}
