package com.netease.iot.rule.proxy.util;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;


import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.netease.iot.rule.proxy.util.Constants.JOB_NAME_SPLITER;


/**
 * @author xiaojianhua
 */
public class CommonUtil {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final TimeZone IOTTIMEZONE = TimeZone.getTimeZone("UTC");
    private static final DateFormat IOTDF = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    static {
        IOTDF.setTimeZone(IOTTIMEZONE);
    }

    public static String getProduct(String productAndJob) {
        return productAndJob.split(Constants.PRODUCT_JOB_DELIMITER_STRING)[0];
    }

    public static String getJob(String productAndJob) {
        return productAndJob.split(Constants.PRODUCT_JOB_DELIMITER_STRING)[1];
    }

    public static Map<String, String> getScopeMap(String scope) {

        Map<String, String> map = new HashMap<>();
        String[] fields = scope.split("\\.");

        String jobId = fields[0];
        String host = fields[1];
        String productAndJob = fields[2];
        String taskName = fields[3];
        String operatorName = fields[4];
        String subtaskIndex = fields[5];
        String gaugeName = fields[6];

        Preconditions.checkArgument(jobId != null && host != null && productAndJob != null && taskName != null
                        && operatorName != null && subtaskIndex != null && gaugeName != null,
                "Illegal metric scope.");

        map.put(Constants.JOB_ID, jobId);
        map.put(Constants.HOST, host);
        map.put(Constants.PRODUCT_JOB_NAME, productAndJob);
        map.put(Constants.TASK_NAME, taskName);
        map.put(Constants.OPERATOR_NAME, operatorName);
        map.put(Constants.SUBTASK_INDEX, subtaskIndex);
        map.put(Constants.GAUGE_NAME, gaugeName);

        return map;
    }

    public static String convertTimeReadable(long delaySeconds) {
        long seconds = delaySeconds % 60;
        long minutes = delaySeconds / 60 % 60;
        long hours = delaySeconds / (60 * 60) % 24;
        long days = delaySeconds / (60 * 60 * 24);
        return days + " day, " + hours + " hour, " + minutes + " minutes, " + seconds + " seconds";
    }

    public static synchronized String getCurrentDateStr() {
        return DATE_FORMAT.format(new Date());
    }

    public static synchronized String getIotCurrentDateStr() {
        return IOTDF.format(new Date());
    }

    public static String getIotTime(String time) {
        try {
            return IOTDF.format(DATE_FORMAT.parse(time));
        } catch (ParseException e) {
            return "";
        }
    }

    public static final long dateStr2long(String dateStr) throws ParseException {
        Date date = DATE_FORMAT.parse(dateStr);
        return date.getTime();
    }

    public static String getRepresentString(String product, String jobName) {
        return String.format("%s%s%s%s", product, JOB_NAME_SPLITER, JOB_NAME_SPLITER, jobName);
    }

    public static String getRepresentString(String product, String jobName, String time) {
        return String.format("%s%s%s%s%s", product, JOB_NAME_SPLITER, JOB_NAME_SPLITER, jobName, time);
    }

    public static String getNodeName(String prefix, String operatorName) {
        String[] args = operatorName.split(prefix, 2);
        if (args.length == 2) {
            return args[1].trim();
        } else {
            return "unkown";
        }
    }

    public static TimeUnit getTimeUnit(String timeunit) throws IllegalArgumentException {
        if (Strings.isNullOrEmpty(timeunit)) {
            return null;
        } else if (timeunit.equalsIgnoreCase("NANOSECONDS")) {
            return TimeUnit.NANOSECONDS;
        } else if (timeunit.equalsIgnoreCase("MILLISECONDS")) {
            return TimeUnit.MILLISECONDS;
        } else if (timeunit.equalsIgnoreCase("SECONDS")) {
            return TimeUnit.SECONDS;
        } else if (timeunit.equalsIgnoreCase("MINUTES")) {
            return TimeUnit.MINUTES;
        } else if (timeunit.equalsIgnoreCase("HOURS")) {
            return TimeUnit.HOURS;
        } else if (timeunit.equalsIgnoreCase("DAYS")) {
            return TimeUnit.DAYS;
        } else {
            throw new IllegalArgumentException("Illegal timeunit: " + timeunit);
        }
    }

    public static String getMockTopicPrefix() {
        return UUID.randomUUID().toString() + "_";
    }
}
