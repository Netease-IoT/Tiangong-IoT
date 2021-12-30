package com.netease.iot.rule.proxy.util;

import com.google.common.collect.Lists;

import java.util.List;

public class IotConstants {

    public static final String RULE_FIELD_RULEID = "RuleId";
    public static final String RULE_FIELD_RULENAME = "RuleName";
    public static final String RULE_FIELD_RULEDESCRITPION = "RuleDescription";
    public static final String RULE_FIELD_SOURCETYPE = "SourceType";
    public static final String RULE_FIELD_SINKETYPE = "SinkType";
    public static final String RULE_FIELD_SOURCEJSON = "SourceJson";
    public static final String RULE_FIELD_SINKJSON = "SinkJson";

    public static final String RULE_FIELD_SQL = "Sql";
    public static final String RULE_SQL_REPLACE = "mqttTopic";

    public static final String LIST_FIELD_LIMIT = "Limit";
    public static final String LIST_FIELD_OFFSET = "Offset";
    public static final String LIST_FIELD_RULENAME = "RuleName";

    public static final String KAFKA_FIELD_INSTANCEID = "InstanceId";
    public static final String KAFKA_FIELD_INSTANCENAME = "InstanceName";
    public static final String KAFKA_FIELD_VERSION = "Version";
    public static final String KAFKA_FIELD_BROKERS = "Brokers";
    public static final String KAFKA_FIELD_TOPIC = "Topic";
    public static final String KAFKA_FIELD_GROUPID = "Groupid";
    public static final String KAFKA_FIELD_MQTTTOPIC = "MqttTopic";
    public static final String KAFKA_FIELD_PRODUCTNAME = "ProductName";
    public static final String KAFKA_SOURCE_TYPE = "KAFKA";

    public static final String TSDB_FIELD_INSTANCEID = "InstanceId";
    public static final String TSDB_FIELD_INSTANCENAME = "InstanceName";
    public static final String TSDB_FIELD_TABLENAME = "Tablename";
    public static final String TSDB_FIELD_DATABASE = "Database";
    public static final String TSDB_FIELD_FIELDS = "Fields";
    public static final String TSDB_FIELD_TAGS = "Tags";
    public static final String TSDB_FIELD_TIMEFIELD = "TimeField";
    public static final String TSDB_FIELD_TIMEUNIT = "TimeUnit";
    public static final String TSDB_FIELD_URL = "Url";
    public static final String TSDB_FIELD_USERNAME = "Username";
    public static final String TSDB_FIELD_PASSWORD = "Password";
    public static final String TSDB_SINK_TYPE = "TSDB";

    public static final List<String> TSDB_FIELDS_TYPE = Lists.newArrayList("BOOLEAN", "INTEGER", "FLOAT", "VARCHAR", "BIGINT");

    public static final String RDS_FIELD_INSTANCEID = "InstanceId";
    public static final String RDS_FIELD_INSTANCENAME = "InstanceName";
    public static final String RDS_FIELD_TABLENAME = "Tablename";
    public static final String RDS_FIELD_DATABASE = "Database";
    public static final String RDS_FIELD_URL = "Url";
    public static final String RDS_FIELD_USERNAME = "Username";
    public static final String RDS_FIELD_PASSWORLD = "Password";
    public static final String RDS_SINK_TYPE = "RDS";

    public static final String RETURN_CODE_JSONERROR = "JsonError";
    public static final String RETURN_CODE_RULEPARAMETERSERROR = "RuleParametersError";
    public static final String RETURN_CODE_SOURCEPARAMETERSERROR = "SourceParametersError";
    public static final String RETURN_CODE_SINKPARAMETERSERROR = "SinkParametersError";
    public static final String RETURN_CODE_GETRDSMETAERROR = "GetRdsMetaError";
    public static final String RETURN_CODE_RULENAMEREPEATERROR = "RuleNameRepeatError";
    public static final String RETURN_CODE_RULESAVEERRORERROR = "RuleSaveError";
    public static final String RETURN_CODE_RULENOTFOUNDERROR = "RuleNotFoundError";
    public static final String RETURN_CODE_INVALIDSQLERROR = "InvalidSqlError";
    public static final String RETURN_CODE_STARTRULEERROR = "StartRuleError";
    public static final String RETURN_CODE_LACKOFRESOURCEERROR = "LackOfResourceError";
    public static final String RETURN_CODE_STOPRULEERROR = "StopRuleError";
    public static final String RETURN_CODE_DELETERULEERROR = "DeleteRuleError";
    public static final String RETURN_CODE_GETDBLISTERROR = "GetDbListError";
    public static final String RETURN_CODE_GETTABLELISTERROR = "GetTableListError";
    public static final String RETURN_CODE_GETRULEALARMINSTANCESERROR = "GetRuleAlarmInstancesError";

    public static final String RETURN_CODE_OPERATORERROR = "OperatorError";

    public static final String RULE_STATUS_NOTREADY = "NOTREADY";
    public static final String RULE_STATUS_READY = "READY";
    public static final String RULE_STATUS_STARTING = "STARTING";
    public static final String RULE_STATUS_RUNNING = "RUNNING";
    public static final String RULE_STATUS_SUSPENDING = "SUSPENDING";
    public static final String RULE_STATUS_SUSPENDED = "SUSPENDED";
    public static final String RULE_STATUS_FAILED = "FAILED";

    public static final String HEAD_REQUESTID = "X-Request-Id";
    public static final String HEAD_PRODUCTID = "X-Product-Id";
    public static final String HEAD_LANGUAGE = "X-Language-Mark";

    public static final String HEAD_LANGUAGE_TYPE_CN = "zh_CN";

    public static final String RDS_SQL_DATABASE = "SHOW DATABASES";

    public static final String RETURN_COMMON_SUCCESS = "Success";
    public static final String RETURN_COMMON_FAIL = "Fail";

    public static final String ALARM_INSTSNCE_DEFAULT = "default";
}