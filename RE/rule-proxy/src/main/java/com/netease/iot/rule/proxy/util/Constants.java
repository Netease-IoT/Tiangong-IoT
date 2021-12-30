package com.netease.iot.rule.proxy.util;

import com.google.common.collect.Lists;
import com.google.gson.Gson;


public class Constants {

    public static final String EMAIL = "sendEmail";
    public static final String POPO = "sendPOPO";
    public static final String STONE = "sendStone";
    public static final String MESSAGE = "sendSMS";
    public static final String CALL = "sendVoice";

    // metric scope
    public static final String JOB_ID = "job_id";
    public static final String HOST = "host";
    public static final String PRODUCT_JOB_NAME = "job_name";
    public static final String PRODUCT_JOB_DELIMITER_STRING = "##";
    public static final String TASK_NAME = "task_name";
    public static final String OPERATOR_NAME = "operator_name";
    public static final String SUBTASK_INDEX = "subtask_index";
    public static final String GAUGE_NAME = "gauge_name";

    // alarm
    public static final String RECEIVE_TYPE_ARRAY = new Gson().toJson(Lists.newArrayList(POPO, MESSAGE, CALL, STONE, EMAIL));
    public static final int THRESHOLD = 300;
    public static final int INTERVAL = 15;

    public static final String JOB_RUNNING_NEW = "RUNNING";


    // Job operations
    public static final int RUN = 1;
    public static final int STOP = 2;
    public static final int DELETE = 3;

    // Job status
    public static final int OFFLINE = 1;
    public static final int ONLINE = 2;

    public static final String SOURCE = "source";
    public static final String SINK = "sink";
    public static final String UNIQUE_ID = "uniqueId";
    public static final String OPERATION = "operation";
    public static final String FETCH_SIZE = "fetchSize";
    public static final String TABLE_NAME = "tableName";


    public static final int DEBUG_CODE_SOURCE_CHANGE = 4;
    public static final int DEBUG_CODE_SINK_CHANGE = 5;
    public static final int DEBUG_CODE_BOTH_CHANGE = 6;



    //metric default ts unit
    public static final long METRIC_TIME_UNIT = 1000;

    // flink job name
    public static final String JOB_NAME_SPLITER = "#";

    public static final String KEY_SINK = "sink.parallelism";
    public static final String KEY_SOURCE = "src.parallelism";
    public static final String KEY_ENV = "env.parallelism";
    public static final String KEY_JTM = "ytm";

    public static final Integer JOB_VERSION_OLD = 0;
    public static final Integer JOB_VERSION_NEW = 1;

    public static final String FLINK_URL_SUFFIX = "/#/overview";

    public static final Integer JOB_OPERATING_STATUS_UNLOCK = 0;
    public static final Integer JOB_OPERATING_STATUS_LOCK = 1;
}
