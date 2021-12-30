package com.netease.iot.rule.proxy.service;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.netease.iot.rule.proxy.ProxyConfig;
import com.netease.iot.rule.proxy.StreamServerClient09;
import com.netease.iot.rule.proxy.StreamServerManager09;
import com.netease.iot.rule.proxy.domain.OfflineJob;
import com.netease.iot.rule.proxy.domain.OnlineJob;
import com.netease.iot.rule.proxy.domain.Server;
import com.netease.iot.rule.proxy.mapper.IotOfflineJobMapper;
import com.netease.iot.rule.proxy.mapper.OnlineJobMapper;
import com.netease.iot.rule.proxy.mapper.ServerMapper;
import com.netease.iot.rule.proxy.mapper.TenantMapper;
import com.netease.iot.rule.proxy.message.*;
import com.netease.iot.rule.proxy.metadata.TaskStateEnum;
import com.netease.iot.rule.proxy.model.RdsSqlMessage;
import com.netease.iot.rule.proxy.model.ResultMessage;
import com.netease.iot.rule.proxy.util.CommonUtil;
import com.netease.iot.rule.proxy.util.IotConstants;
import com.netease.iot.rule.proxy.util.IotMessage;
import com.netease.iot.rule.proxy.util.IotUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.netease.iot.rule.proxy.util.Constants.JOB_OPERATING_STATUS_LOCK;
import static com.netease.iot.rule.proxy.util.Constants.JOB_OPERATING_STATUS_UNLOCK;



@Component
public class IotService {

    private static final Logger LOG = LoggerFactory.getLogger(IotService.class);
    @Autowired
    private IotOfflineJobMapper iotOfflineJobMapper;
    @Autowired
    private OnlineJobMapper onlineJobMapper;
    @Autowired
    private TenantMapper tenantMapper;
    @Autowired
    private JobService09 jobService09;
    @Autowired
    private ServerMapper serverMapper;

    private static String getMD5(String message) {
        return DigestUtils.md5Hex(message);
    }

    private boolean checkRuleParameters(OfflineJob offlineJob, JSONObject jsonObject) {
        String ruleName = jsonObject.getString(IotConstants.RULE_FIELD_RULENAME);
        if (StringUtils.isEmpty(ruleName) || !IotUtil.checkString(ruleName, 63)) {
            return false;
        }
        offlineJob.setJobName(ruleName);

        String ruleDescription = jsonObject.getString(IotConstants.RULE_FIELD_RULEDESCRITPION);
        if (!StringUtils.isEmpty(ruleDescription) && !IotUtil.checkStringSize(ruleDescription, 128)) {
            return false;
        }
        offlineJob.setRuleDescription(ruleDescription);
        String sourceType = jsonObject.getString(IotConstants.RULE_FIELD_SOURCETYPE);
        if (!sourceType.equals(IotConstants.KAFKA_SOURCE_TYPE)) {
            return false;
        }
        offlineJob.setSourceType(sourceType);

        String sinkType = jsonObject.getString(IotConstants.RULE_FIELD_SINKETYPE);
        if (!sinkType.equals(IotConstants.RDS_SINK_TYPE) && !sinkType.equals(IotConstants.TSDB_SINK_TYPE)) {
            return false;
        }
        offlineJob.setSinkType(sinkType);

        return true;
    }

    private boolean checkKafkaParameters(OfflineJob offlineJob, JSONObject jsonObject, String jobId) {
        JSONObject kafkaObject = jsonObject.getJSONObject(IotConstants.RULE_FIELD_SOURCEJSON);
        String instanceId = kafkaObject.getString(IotConstants.KAFKA_FIELD_INSTANCEID);
        if (StringUtils.isEmpty(instanceId)) {
            return false;
        }
        String instanceName = kafkaObject.getString(IotConstants.KAFKA_FIELD_INSTANCENAME);
        if (StringUtils.isEmpty(instanceName)) {
            return false;
        }
        String version = kafkaObject.getString(IotConstants.KAFKA_FIELD_VERSION);
        if (StringUtils.isEmpty(version) || !IotUtil.checkKafkaVersion(version)) {
            return false;
        }
        String brokers = kafkaObject.getString(IotConstants.KAFKA_FIELD_BROKERS);
        if (StringUtils.isEmpty(brokers)) {
            return false;
        }
        String topic = kafkaObject.getString(IotConstants.KAFKA_FIELD_TOPIC);
        if (StringUtils.isEmpty(topic)) {
            return false;
        }
        String groupId = kafkaObject.getString(IotConstants.KAFKA_FIELD_GROUPID);
        if (StringUtils.isEmpty(groupId)) {
            groupId = StringUtils.isEmpty(jobId) ? "rule-" + System.currentTimeMillis()
                    : JSONObject.parseObject(iotOfflineJobMapper.getDetail(jobId, null).getSourceJson()).getString(IotConstants.KAFKA_FIELD_TOPIC);
            kafkaObject.put(IotConstants.KAFKA_FIELD_GROUPID, groupId);
        }
        String mqttTopic = kafkaObject.getString(IotConstants.KAFKA_FIELD_MQTTTOPIC);
        if (StringUtils.isEmpty(mqttTopic)) {
            return false;
        }
        String productName = kafkaObject.getString(IotConstants.KAFKA_FIELD_PRODUCTNAME);
        if (StringUtils.isEmpty(productName)) {
            return false;
        }
        offlineJob.setSourceJson(kafkaObject.toJSONString());
        return true;
    }

    private boolean checkTsdbParamters(OfflineJob offlineJob, JSONObject jsonObject) {
        JSONObject tsdbObject = jsonObject.getJSONObject(IotConstants.RULE_FIELD_SINKJSON);
        String instanceId = tsdbObject.getString(IotConstants.TSDB_FIELD_INSTANCEID);
        if (StringUtils.isEmpty(instanceId)) {
            return false;
        }
        String instanceName = tsdbObject.getString(IotConstants.TSDB_FIELD_INSTANCENAME);
        if (StringUtils.isEmpty(instanceName)) {
            return false;
        }
        String tableName = tsdbObject.getString(IotConstants.TSDB_FIELD_TABLENAME);
        if (StringUtils.isEmpty(tableName)) {
            return false;
        }
        String database = tsdbObject.getString(IotConstants.TSDB_FIELD_DATABASE);
        if (StringUtils.isEmpty(database)) {
            return false;
        }
        String fields = tsdbObject.getString(IotConstants.TSDB_FIELD_FIELDS);
        if (StringUtils.isEmpty(fields)) {
            return false;
        }
        JSONObject fieldsJson = JSONObject.parseObject(fields);
        for (String field : fieldsJson.keySet()) {
            String type = fieldsJson.getString(field);
            if (!IotConstants.TSDB_FIELDS_TYPE.contains(type.toUpperCase())) {
                return false;
            }
        }
        String timeField = tsdbObject.getString(IotConstants.TSDB_FIELD_TIMEFIELD);
        if (StringUtils.isEmpty(timeField)) {
            return false;
        }
        String timeUnit = tsdbObject.getString(IotConstants.TSDB_FIELD_TIMEUNIT);
        if (StringUtils.isEmpty(timeUnit)) {
            return false;
        }
        String tags = tsdbObject.getString(IotConstants.TSDB_FIELD_TAGS);
        if (StringUtils.isEmpty(tags)) {
            return false;
        }
        String url = tsdbObject.getString(IotConstants.TSDB_FIELD_URL);
        if (StringUtils.isEmpty(url)) {
            return false;
        }
        String username = tsdbObject.getString(IotConstants.TSDB_FIELD_USERNAME);
        if (StringUtils.isEmpty(username)) {
            return false;
        }
        String password = tsdbObject.getString(IotConstants.TSDB_FIELD_PASSWORD);
        if (StringUtils.isEmpty(password)) {
            return false;
        }
        offlineJob.setSinkJson(tsdbObject.toJSONString());
        return true;
    }

    private boolean checkRdsParamters(OfflineJob offlineJob, JSONObject jsonObject) {
        JSONObject rdsObject = jsonObject.getJSONObject(IotConstants.RULE_FIELD_SINKJSON);

        String instanceId = rdsObject.getString(IotConstants.RDS_FIELD_INSTANCEID);
        if (StringUtils.isEmpty(instanceId)) {
            return false;
        }
        String instanceName = rdsObject.getString(IotConstants.RDS_FIELD_INSTANCENAME);
        if (StringUtils.isEmpty(instanceName)) {
            return false;
        }
        String tableName = rdsObject.getString(IotConstants.RDS_FIELD_TABLENAME);
        if (StringUtils.isEmpty(tableName)) {
            return false;
        }
        String database = rdsObject.getString(IotConstants.RDS_FIELD_DATABASE);
        if (StringUtils.isEmpty(database)) {
            return false;
        }
        String url = rdsObject.getString(IotConstants.RDS_FIELD_URL);
        if (StringUtils.isEmpty(url)) {
            return false;
        }
        String username = rdsObject.getString(IotConstants.RDS_FIELD_USERNAME);
        if (StringUtils.isEmpty(username)) {
            return false;
        }
        String password = rdsObject.getString(IotConstants.RDS_FIELD_PASSWORLD);
        if (StringUtils.isEmpty(password)) {
            return false;
        }
        offlineJob.setSinkJson(rdsObject.toJSONString());
        return true;
    }

    private String getKafkaSql(String jsonString) {

        StringBuilder sb = new StringBuilder();
        JSONObject jsonObject = JSONObject.parseObject(jsonString);
        sb.append("CREATE TABLE source (").append("\r\n")
                .append("  content VARCHAR").append("\r\n")
                .append(") PROPERTIES (").append("\r\n")
                .append("  category = 'source',").append("\r\n")
                .append("  type = 'kafka',").append("\r\n")
                .append("  auto_offset_reset = 'latest',").append("\r\n")
                .append("  delimiter = 'raw',").append("\r\n")
                .append("  version = '").append(jsonObject.getString(IotConstants.KAFKA_FIELD_VERSION)).append("',").append("\r\n")
                .append("  topic = '").append(jsonObject.getString(IotConstants.KAFKA_FIELD_TOPIC)).append("',").append("\r\n")
                .append("  brokers = '").append(jsonObject.getString(IotConstants.KAFKA_FIELD_BROKERS)).append("',").append("\r\n")
                .append("  group_id = '").append(jsonObject.getString(IotConstants.KAFKA_FIELD_GROUPID)).append("'").append("\r\n")
                .append(");").append("\r\n");
        return sb.toString();
    }

    /**
     * @param jsonString
     * @return
     */
    private String getTsdbSql(String jsonString) {
        StringBuilder sb = new StringBuilder();
        JSONObject jsonObject = JSONObject.parseObject(jsonString);
        sb.append("CREATE TABLE sink (").append("\r\n");

        sb.append("\t").append(jsonObject.getString(IotConstants.TSDB_FIELD_TIMEFIELD)).append("\t").append("BIGINT").append(",\r\n");

        String[] tags = jsonObject.getString(IotConstants.TSDB_FIELD_TAGS).split(",");
        for (String tag : tags) {
            sb.append("\t").append(tag).append("\t").append("VARCHAR").append(",\r\n");
        }

        JSONObject fieldsObject = jsonObject.getJSONObject(IotConstants.TSDB_FIELD_FIELDS);
        int num = fieldsObject.size();
        for (Map.Entry<String, Object> entry : fieldsObject.entrySet()) {
            if (num == 1) {
                sb.append("\t").append(entry.getKey()).append("\t").append(entry.getValue().toString()).append("\r\n");
            } else {
                sb.append("\t").append(entry.getKey()).append("\t").append(entry.getValue().toString()).append(",\r\n");
            }
            num--;
        }

        sb.append(") PROPERTIES (").append("\r\n")
                .append("  category = 'sink',").append("\r\n")
                .append("  type = 'tsdb',").append("\r\n")
                .append("  tsdb_url = '").append(jsonObject.getString(IotConstants.TSDB_FIELD_URL)).append("',").append("\r\n")
                .append("  tsdb_user = '").append(jsonObject.getString(IotConstants.TSDB_FIELD_USERNAME)).append("',").append("\r\n")
                .append("  tsdb_password = '").append(jsonObject.getString(IotConstants.TSDB_FIELD_PASSWORD)).append("',").append("\r\n")
                .append("  tsdb_database = '").append(jsonObject.getString(IotConstants.TSDB_FIELD_DATABASE)).append("',").append("\r\n")
                .append("  tsdb_table = '").append(jsonObject.getString(IotConstants.TSDB_FIELD_TABLENAME)).append("',").append("\r\n")
                .append("  tsdb_tags = '").append(jsonObject.getString(IotConstants.TSDB_FIELD_TAGS)).append("',").append("\r\n")
                .append("  tsdb_time_field = '").append(jsonObject.getString(IotConstants.TSDB_FIELD_TIMEFIELD)).append("',").append("\r\n")
                .append("  tsdb_time_unit = '").append(jsonObject.getString(IotConstants.TSDB_FIELD_TIMEUNIT)).append("',").append("\r\n")
                .append("  buffer_size = '10000',").append("\r\n")
                .append("  FLUSH_INTERVAL = '1000'").append("\r\n")
                .append(");").append("\r\n");
        return sb.toString();
    }

    private List<String> getDatabases(String url, String username, String password) {
        List<String> databaseLists = Lists.newArrayList();
        Connection conn = null;
        PreparedStatement preparedStatement = null;
        ResultSet rs = null;
        try {
            conn = DriverManager.getConnection(url, username, password);
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            preparedStatement = conn.prepareStatement(IotConstants.RDS_SQL_DATABASE);
            rs = preparedStatement.executeQuery();
            while (rs.next()) {
                databaseLists.add(rs.getString(1));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            try {
                preparedStatement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            try {
                rs.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return databaseLists;
    }

    private List<String> getTables(String url, String database, String username, String password) {
        List<String> tableLists = Lists.newArrayList();
        Connection conn = null;
        ResultSet resultSet = null;
        if (url.endsWith("/")) {
            url += database;
        } else {
            url += "/" + database;
        }
        try {
            conn = DriverManager.getConnection(url, username, password);
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            DatabaseMetaData metaData = conn.getMetaData();
            resultSet = metaData.getTables(conn.getCatalog(), database, null, new String[]{"TABLE"});
            while (resultSet.next()) {
                tableLists.add(resultSet.getString("TABLE_NAME"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            try {
                resultSet.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return tableLists;
    }

    private String getRdsSql(String akka, RdsSqlMessage msg) {
        StreamServerClient09 streamServerClient = new StreamServerClient09(
                StreamServerManager09.ClientType.Web.name(),
                akka,
                "");

        final Future<Object> future = streamServerClient.submit(msg, ProxyConfig.DEFAULT_SERVER_REMOTE_AKKA_TIMEOUT);

        try {
            ResultMessage serverResult = (ResultMessage) Await.result(future, new FiniteDuration(300, TimeUnit.SECONDS));
            LOG.info("serverResult:" + serverResult);
            return serverResult.getData().toString();
        } catch (Exception e) {
            e.printStackTrace();
            LOG.error("get rds meta error");
            return null;
        }
    }

    public IotBaseResultMessage createOrUpdateRule(String jsonString, String requestId, String tenantId, String language) {
        doLog(" update or create ", requestId);
        JSONObject jsonObject;
        try {
            jsonObject = JSONObject.parseObject(jsonString);
        } catch (Exception e) {
            LOG.error("parse json error", e);
            return ErrorResultMessage.cook(IotConstants.RETURN_CODE_JSONERROR,
                    IotMessage.get(IotConstants.RETURN_CODE_JSONERROR, language), requestId);
        }

        try {
            String jobId = jsonObject.getString(IotConstants.RULE_FIELD_RULEID);

            Integer serverId = tenantMapper.getServerId(tenantId);
            if (null == serverId) {
                LOG.error(" serverId not found for  tenantId :{},requestId : {}", tenantId, requestId);
                return ErrorResultMessage.cook(IotConstants.RETURN_CODE_OPERATORERROR,
                        IotMessage.get(IotConstants.RETURN_CODE_OPERATORERROR, language), requestId);
            }

            OfflineJob offlineJob = new OfflineJob();
            offlineJob.setProduct(tenantId);

            if (!checkRuleParameters(offlineJob, jsonObject)) {
                return ErrorResultMessage.cook(IotConstants.RETURN_CODE_RULEPARAMETERSERROR,
                        IotMessage.get(IotConstants.RETURN_CODE_RULEPARAMETERSERROR, language), requestId);
            }
            if (!checkKafkaParameters(offlineJob, jsonObject, jobId)) {
                return ErrorResultMessage.cook(IotConstants.RETURN_CODE_SOURCEPARAMETERSERROR,
                        IotMessage.get(IotConstants.RETURN_CODE_SOURCEPARAMETERSERROR, language), requestId);
            }

            String sourceSql = getKafkaSql(offlineJob.getSourceJson());
            String sinkSql;
            String sinkType = offlineJob.getSinkType();
            switch (sinkType) {
            case IotConstants.RDS_SINK_TYPE:
                if (!checkRdsParamters(offlineJob, jsonObject)) {
                    return ErrorResultMessage.cook(IotConstants.RETURN_CODE_SINKPARAMETERSERROR,
                            IotMessage.get(IotConstants.RETURN_CODE_SINKPARAMETERSERROR, language), requestId);
                }
                JSONObject sinkJsonObject = JSONObject.parseObject(offlineJob.getSinkJson());

                RdsSqlMessage message = new RdsSqlMessage(sinkJsonObject.getString(IotConstants.RDS_FIELD_URL),
                        sinkJsonObject.getString(IotConstants.RDS_FIELD_DATABASE),
                        sinkJsonObject.getString(IotConstants.RDS_FIELD_USERNAME),
                        sinkJsonObject.getString(IotConstants.RDS_FIELD_PASSWORLD),
                        sinkJsonObject.getString(IotConstants.RDS_FIELD_TABLENAME));
                Server server = serverMapper.getServer(serverId).get(0);
                sinkSql = getRdsSql(server.getAkkaPath(), message);
                if (StringUtils.isEmpty(sinkSql)) {
                    return ErrorResultMessage.cook(IotConstants.RETURN_CODE_GETRDSMETAERROR,
                            IotMessage.get(IotConstants.RETURN_CODE_GETRDSMETAERROR, language), requestId);
                }
                break;
            case IotConstants.TSDB_SINK_TYPE:
                if (!checkTsdbParamters(offlineJob, jsonObject)) {
                    return ErrorResultMessage.cook(IotConstants.RETURN_CODE_SINKPARAMETERSERROR,
                            IotMessage.get(IotConstants.RETURN_CODE_SINKPARAMETERSERROR, language), requestId);
                }
                sinkSql = getTsdbSql(offlineJob.getSinkJson());
                break;
            default:
                return ErrorResultMessage.cook(IotConstants.RETURN_CODE_SINKPARAMETERSERROR,
                        IotMessage.get(IotConstants.RETURN_CODE_SINKPARAMETERSERROR, language), requestId);
            }
            String perpareSql = sourceSql + sinkSql + ProxyConfig.IOT_AUTO_SQL;
            offlineJob.setPrepareSql(perpareSql);

            String nowTime = CommonUtil.getCurrentDateStr();
            if (StringUtils.isEmpty(jobId)) {
                //create
                String jobName = offlineJob.getJobName();
                int ruleNameCount = iotOfflineJobMapper.checkRuleName(null, jobName, offlineJob.getProduct());
                if (ruleNameCount > 0) {
                    return ErrorResultMessage.cook(IotConstants.RETURN_CODE_RULENAMEREPEATERROR,
                            IotMessage.get(IotConstants.RETURN_CODE_RULENAMEREPEATERROR, language), requestId);
                }
                jobId = getMD5(CommonUtil.getRepresentString(offlineJob.getProduct(), jobName, nowTime));
                int jobIdCount = iotOfflineJobMapper.checkJobId(jobId);
                if (jobIdCount > 0) {
                    return ErrorResultMessage.cook(IotConstants.RETURN_CODE_RULESAVEERRORERROR,
                            IotMessage.get(IotConstants.RETURN_CODE_RULESAVEERRORERROR, language), requestId);
                }
                int jobCount = iotOfflineJobMapper.createJob(offlineJob.getProduct(), jobId, offlineJob.getJobName(),
                        nowTime, 1, serverId, ProxyConfig.IOT_CONF, perpareSql, offlineJob.getSourceType(),
                        offlineJob.getSourceJson(), offlineJob.getSinkType(), offlineJob.getSinkJson(), offlineJob.getRuleDescription());
                if (jobCount == 0) {
                    return ErrorResultMessage.cook(IotConstants.RETURN_CODE_RULESAVEERRORERROR,
                            IotMessage.get(IotConstants.RETURN_CODE_RULESAVEERRORERROR, language), requestId);
                }
                onlineJobMapper.iotDeployJob(jobId, jobName, "", ProxyConfig.IOT_CONF, 1, offlineJob.getProduct(),
                        offlineJob.getProduct(), offlineJob.getProduct(), nowTime, nowTime, "", nowTime, 1, serverId, 1, "");
            } else {
                OfflineJob oldOfflineJob = iotOfflineJobMapper.getDetail(jobId, offlineJob.getProduct());
                if (null == oldOfflineJob) {
                    return ErrorResultMessage.cook(IotConstants.RETURN_CODE_RULENOTFOUNDERROR,
                            IotMessage.get(IotConstants.RETURN_CODE_RULENOTFOUNDERROR, language), requestId);
                }
                String jobName = offlineJob.getJobName();
                int ruleNameCount = iotOfflineJobMapper.checkRuleName(jobId, jobName, offlineJob.getProduct());
                if (ruleNameCount > 0) {
                    return ErrorResultMessage.cook(IotConstants.RETURN_CODE_RULENAMEREPEATERROR,
                            IotMessage.get(IotConstants.RETURN_CODE_RULENAMEREPEATERROR, language), requestId);
                }
                int jobCount = iotOfflineJobMapper.updateJob(offlineJob.getProduct(), jobId, jobName, offlineJob.getRuleDescription(),
                        CommonUtil.getCurrentDateStr(), perpareSql, offlineJob.getSourceType(),
                        offlineJob.getSourceJson(), offlineJob.getSinkType(), offlineJob.getSinkJson(), offlineJob.getRuleDescription());
                if (jobCount == 0) {
                    return ErrorResultMessage.cook(IotConstants.RETURN_CODE_RULESAVEERRORERROR,
                            IotMessage.get(IotConstants.RETURN_CODE_RULESAVEERRORERROR, language), requestId);
                }
                String fullSql = offlineJob.getPrepareSql() + (null == oldOfflineJob.getSql() ? "" : oldOfflineJob.getSql());
                onlineJobMapper.iotReDeployJob(jobId, jobName, offlineJob.getProduct(), offlineJob.getProduct(), nowTime, nowTime, fullSql, fullSql,
                        Lists.newArrayList(TaskStateEnum.RUNNING.ordinal(), TaskStateEnum.STARTING.ordinal(), TaskStateEnum.RESUMING.ordinal())
                );
            }
            return SaveResultMessage.cook(requestId, jobId);
        } catch (Exception e) {
            LOG.error(" update or create rule error ,requestId=" + requestId, e);
            return ErrorResultMessage.cook(IotConstants.RETURN_CODE_OPERATORERROR,
                    IotMessage.get(IotConstants.RETURN_CODE_OPERATORERROR, language), requestId);
        }
    }

    public IotBaseResultMessage submitSql(Map<String, String> map, String requestId, String tenantId, String language) {
        try {
            doLog("submit sql", requestId);
            String ruleId = map.get(IotConstants.RULE_FIELD_RULEID);
            String sql = map.get(IotConstants.RULE_FIELD_SQL);
            OfflineJob offlineJob = iotOfflineJobMapper.getDetail(ruleId, tenantId);
            if (null == offlineJob) {
                return ErrorResultMessage.cook(IotConstants.RETURN_CODE_RULENOTFOUNDERROR,
                        IotMessage.get(IotConstants.RETURN_CODE_JSONERROR, language), requestId);
            }
            String mqttTopic = JSONObject.parseObject(offlineJob.getSourceJson()).getString(IotConstants.KAFKA_FIELD_MQTTTOPIC);
            String replaceSql = replaceSql(sql, mqttTopic);
            String realSql = offlineJob.getPrepareSql() + replaceSql;

            ResultMessage resultMessage = jobService09.syntaxCheck(offlineJob.getProduct(),
                    offlineJob.getUserId(), offlineJob.getId(), realSql, offlineJob.getServerId(), offlineJob.getClusterId());

            if (!resultMessage.isSuccess()) {
                return ErrorResultMessage.cook(IotConstants.RETURN_CODE_INVALIDSQLERROR,
                        resultMessage.getMsg(), requestId);
            }
            iotOfflineJobMapper.updateSql(ruleId, replaceSql);
            onlineJobMapper.updateSql(ruleId, realSql);
            onlineJobMapper.setJobStatus(ruleId, TaskStateEnum.READY.ordinal(), CommonUtil.getCurrentDateStr());
            return SuccessResultMessage.cook(requestId);
        } catch (Exception e) {
            LOG.error(" submit sql error ,requestId=" + requestId, e);
            return ErrorResultMessage.cook(IotConstants.RETURN_CODE_OPERATORERROR,
                    IotMessage.get(IotConstants.RETURN_CODE_OPERATORERROR, language), requestId);
        }
    }

    public IotBaseResultMessage getSql(Map<String, String> map, String requestId, String tenantId, String language) {
        try {
            doLog("get Sql", requestId);
            String ruleId = map.get(IotConstants.RULE_FIELD_RULEID);
            OfflineJob offlineJob = iotOfflineJobMapper.getDetail(ruleId, tenantId);
            if (null == offlineJob) {
                return ErrorResultMessage.cook(IotConstants.RETURN_CODE_RULENOTFOUNDERROR,
                        IotMessage.get(IotConstants.RETURN_CODE_RULENOTFOUNDERROR, language), requestId);
            } else {
                return EditSqlResultMessage.cook(requestId, ruleId, offlineJob.getPrepareSql(), offlineJob.getSql());
            }
        } catch (Exception e) {
            LOG.error(" get sql error ,requestId=" + requestId, e);
            return ErrorResultMessage.cook(IotConstants.RETURN_CODE_OPERATORERROR,
                    IotMessage.get(IotConstants.RETURN_CODE_OPERATORERROR, language), requestId);
        }
    }

    public IotBaseResultMessage getRule(Map<String, String> map, String requestId, String tenantId, String language) {
        try {
            doLog("get rule", requestId);
            String ruleId = map.get(IotConstants.RULE_FIELD_RULEID);
            OfflineJob offlineJob = iotOfflineJobMapper.getDetail(ruleId, tenantId);
            OnlineJob onlineJob = onlineJobMapper.getJobDetail(ruleId);
            if (null == offlineJob || null == onlineJob) {
                return ErrorResultMessage.cook(IotConstants.RETURN_CODE_RULENOTFOUNDERROR,
                        IotMessage.get(IotConstants.RETURN_CODE_RULENOTFOUNDERROR, language), requestId);
            } else {
                offlineJob.setStatus(onlineJob.getStatus());
                RuleDetailResultMessage ruleDetailResultMessage = getRuleFromJob(offlineJob, requestId);
                return ruleDetailResultMessage;
            }
        } catch (Exception e) {
            LOG.error(" get rule error ,requestId=" + requestId, e);
            return ErrorResultMessage.cook(IotConstants.RETURN_CODE_OPERATORERROR,
                    IotMessage.get(IotConstants.RETURN_CODE_OPERATORERROR, language), requestId);
        }
    }

    public IotBaseResultMessage getRuleList(Map<String, String> map, String requestId, String tenantId, String language) {
        try {
            doLog("get rule list", requestId);
            int limit = Integer.parseInt(map.get(IotConstants.LIST_FIELD_LIMIT));
            int offset = Integer.parseInt(map.get(IotConstants.LIST_FIELD_OFFSET)) / limit + 1;
            String ruleName = map.get(IotConstants.LIST_FIELD_RULENAME);

            Map<String, Object> stringObjectMap = Maps.newHashMap();
            stringObjectMap.put("product", tenantId);
            stringObjectMap.put("pageSize", limit);
            stringObjectMap.put("currPage", offset);
            stringObjectMap.put("jobName", ruleName);

            List<OfflineJob> jobList = iotOfflineJobMapper.searchJobByJobNameByPage(stringObjectMap);
            List<RuleDetailResultMessage> ruleList = Lists.newArrayList();
            for (OfflineJob job : jobList) {
                ruleList.add(getRuleFromJob(job, requestId));
            }
            Integer count = iotOfflineJobMapper.searchJobByJobNameCount(stringObjectMap);
            return ListRuleResultMessage.cook(requestId, ruleList, count);
        } catch (NumberFormatException e) {
            LOG.error(" get rule list error ,requestId=" + requestId, e);
            return ErrorResultMessage.cook(IotConstants.RETURN_CODE_OPERATORERROR,
                    IotMessage.get(IotConstants.RETURN_CODE_OPERATORERROR, language), requestId);
        }
    }

    private String replaceSql(String sql, String replace) {
        if (StringUtils.isEmpty(sql)) {
            return "";
        }
        String checkString = "(\\$\\{)([" + IotConstants.RULE_SQL_REPLACE + "]+)(\\})";
        Pattern p = Pattern.compile(checkString);
        Matcher m = p.matcher(sql);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, replace);
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private RuleDetailResultMessage getRuleFromJob(OfflineJob offlineJob, String requestId) {
        RuleDetailResultMessage ruleDetailResultMessage = new RuleDetailResultMessage(requestId);
        ruleDetailResultMessage.setRuleId(offlineJob.getId());
        ruleDetailResultMessage.setRuleName(offlineJob.getJobName());
        ruleDetailResultMessage.setRuleDescription(offlineJob.getRuleDescription());
        ruleDetailResultMessage.setSourceType(offlineJob.getSourceType());
        ruleDetailResultMessage.setSourceJson(offlineJob.getSourceJson());
        ruleDetailResultMessage.setSinkType(offlineJob.getSinkType());
        ruleDetailResultMessage.setSinkJson(offlineJob.getSinkJson());
        ruleDetailResultMessage.setPrepareSql(offlineJob.getPrepareSql());
        ruleDetailResultMessage.setUserSql(offlineJob.getSql());
        switch (offlineJob.getStatus()) {
        case -1:
            ruleDetailResultMessage.setStatus(IotConstants.RULE_STATUS_NOTREADY);
            break;
        case 0:
            ruleDetailResultMessage.setStatus(IotConstants.RULE_STATUS_READY);
            break;
        case 1:
            ruleDetailResultMessage.setStatus(IotConstants.RULE_STATUS_STARTING);
            break;
        case 2:
            ruleDetailResultMessage.setStatus(IotConstants.RULE_STATUS_RUNNING);
            break;
        case 3:
            ruleDetailResultMessage.setStatus(IotConstants.RULE_STATUS_SUSPENDING);
            break;
        case 4:
            ruleDetailResultMessage.setStatus(IotConstants.RULE_STATUS_SUSPENDED);
            break;
        case 7:
            ruleDetailResultMessage.setStatus(IotConstants.RULE_STATUS_STARTING);
            break;
        case 8:
            ruleDetailResultMessage.setStatus(IotConstants.RULE_STATUS_FAILED);
            break;
        default:
            ruleDetailResultMessage.setStatus(IotConstants.RULE_STATUS_FAILED);
            break;
        }
        ruleDetailResultMessage.setCreateAt(CommonUtil.getIotTime(offlineJob.getCreateTime()));
        ruleDetailResultMessage.setUpdateAt(CommonUtil.getIotTime(offlineJob.getEditTime()));
        return ruleDetailResultMessage;
    }

    public IotBaseResultMessage startRule(Map<String, String> map, String requestId, String tenantId, String language) {
        try {
            doLog("start rule", requestId);
            String jobId = map.get(IotConstants.RULE_FIELD_RULEID);
            OfflineJob job = iotOfflineJobMapper.getDetail(jobId, tenantId);
            if (null == job) {
                return ErrorResultMessage.cook(IotConstants.RETURN_CODE_RULENOTFOUNDERROR,
                        IotMessage.get(IotConstants.RETURN_CODE_RULENOTFOUNDERROR, language), requestId);
            }
            OnlineJob onlineJob = onlineJobMapper.getJobDetail(jobId);
//            if (onlineJob.getStatus() == -1) {
//                return ErrorResultMessage.cook(IotConstants.RETURN_CODE_STARTRULEERROR,
//                        IotMessage.get(IotConstants.RETURN_CODE_STARTRULEERROR, language), requestId);
//            }
            int count = onlineJobMapper.iotOnlineJobCount(tenantId);
            if (count >= ProxyConfig.IOT_LIMIT) {
                return ErrorResultMessage.cook(IotConstants.RETURN_CODE_LACKOFRESOURCEERROR,
                        IotMessage.get(IotConstants.RETURN_CODE_LACKOFRESOURCEERROR, language), requestId);
            }
            LOG.info(" try call server ");
            return setJobStatus(map, tenantId, requestId, "1", IotConstants.RETURN_CODE_STARTRULEERROR, language);
        } catch (Exception e) {
            LOG.error(" start rule error ,requestId=" + requestId, e);
            return ErrorResultMessage.cook(IotConstants.RETURN_CODE_OPERATORERROR,
                    IotMessage.get(IotConstants.RETURN_CODE_OPERATORERROR, language), requestId);
        }
    }

    public IotBaseResultMessage stopRule(Map<String, String> map, String tenantId, String requestId, String language) {
        try {
            doLog("stop rule", requestId);
            return setJobStatus(map, tenantId, requestId, "2", IotConstants.RETURN_CODE_STOPRULEERROR, language);
        } catch (Exception e) {
            LOG.error(" stop rule error ,requestId=" + requestId, e);
            return ErrorResultMessage.cook(IotConstants.RETURN_CODE_OPERATORERROR,
                    IotMessage.get(IotConstants.RETURN_CODE_OPERATORERROR, language), requestId);
        }
    }

    public IotBaseResultMessage deleteRule(Map<String, String> map, String tenantId, String requestId, String language) {
        try {
            doLog("delete rule", requestId);
            IotBaseResultMessage result = setJobStatus(map, tenantId, requestId, "3", IotConstants.RETURN_CODE_DELETERULEERROR, language);
            if (result instanceof SuccessResultMessage) {
                String jobId = map.get(IotConstants.RULE_FIELD_RULEID);
                int count = iotOfflineJobMapper.deleteJob(jobId);
                if (count == 0) {
                    return ErrorResultMessage.cook(IotConstants.RETURN_CODE_DELETERULEERROR,
                            IotMessage.get(IotConstants.RETURN_CODE_DELETERULEERROR, language), requestId);
                } else {
                    return result;
                }
            } else {
                return result;
            }
        } catch (Exception e) {
            LOG.error(" delete rule error ,requestId=" + requestId, e);
            return ErrorResultMessage.cook(IotConstants.RETURN_CODE_OPERATORERROR,
                    IotMessage.get(IotConstants.RETURN_CODE_OPERATORERROR, language), requestId);
        }
    }

    private IotBaseResultMessage setJobStatus(Map<String, String> map, String tenantId, String requestId, String setStatus, String errorCode, String language) {
        boolean finalUpdateOperatrStatus = true;
        String jobId = null;
        try {
            jobId = map.get(IotConstants.RULE_FIELD_RULEID);
            int count = iotOfflineJobMapper.updateOperatingStatus(jobId, JOB_OPERATING_STATUS_UNLOCK, JOB_OPERATING_STATUS_LOCK);
            if (count == 0) {
                finalUpdateOperatrStatus = false;
                LOG.error(" rule operate status error ");
                return ErrorResultMessage.cook(errorCode, IotMessage.get(errorCode, language) + jobId, requestId);
            }

            OfflineJob offlineJob = iotOfflineJobMapper.getDetail(jobId, tenantId);
            OnlineJob onlineJob = onlineJobMapper.getJobDetail(jobId);
            if (null == offlineJob || null == onlineJob) {
                return ErrorResultMessage.cook(IotConstants.RETURN_CODE_RULENOTFOUNDERROR,
                        IotMessage.get(errorCode, language), requestId);
            }
            ResultMessage resultMessage = jobService09.setJobStatus(offlineJob.getProduct(), offlineJob.getProduct(), jobId, setStatus, "false");
            if (resultMessage.isSuccess()) {
                return SuccessResultMessage.cook(requestId);
            } else {
                return ErrorResultMessage.cook(errorCode, resultMessage.getMsg(), requestId);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ErrorResultMessage.cook(errorCode, IotMessage.get(errorCode, language), requestId);
        } finally {
            if (finalUpdateOperatrStatus) {
                iotOfflineJobMapper.updateOperatingStatus(jobId, JOB_OPERATING_STATUS_LOCK, JOB_OPERATING_STATUS_UNLOCK);
            }
        }
    }

    public IotBaseResultMessage getDB(Map<String, String> map, String tenantId, String requestId, String language) {
        try {
            String url = map.get(IotConstants.RDS_FIELD_URL);
            String username = map.get(IotConstants.RDS_FIELD_USERNAME);
            String password = map.get(IotConstants.RDS_FIELD_PASSWORLD);
            List<String> dbList = getDatabases(url, username, password);
            return DatabasesListResultMessage.cook(requestId, dbList);
        } catch (Exception e) {
            return ErrorResultMessage.cook(IotConstants.RETURN_CODE_GETDBLISTERROR,
                    IotMessage.get(IotConstants.RETURN_CODE_GETDBLISTERROR, language), requestId);
        }
    }

    public IotBaseResultMessage getTable(Map<String, String> map, String tenantId, String requestId, String language) {
        try {
            String url = map.get(IotConstants.RDS_FIELD_URL);
            String username = map.get(IotConstants.RDS_FIELD_USERNAME);
            String password = map.get(IotConstants.RDS_FIELD_PASSWORLD);
            String database = map.get(IotConstants.RDS_FIELD_DATABASE);
            List<String> tableList = getTables(url, database, username, password);
            return TableListResultMessage.cook(requestId, tableList);
        } catch (Exception e) {
            return ErrorResultMessage.cook(IotConstants.RETURN_CODE_GETTABLELISTERROR,
                    IotMessage.get(IotConstants.RETURN_CODE_GETTABLELISTERROR, language), requestId);
        }
    }

    public IotBaseResultMessage getRuleAlarmInstances(String tenantId, String requestId, String language) {
        try {
            doLog("get rule alarmInstances", requestId);
            List<String> instances = new ArrayList<>();
            instances.add(IotConstants.ALARM_INSTSNCE_DEFAULT);
            return AlarmInstanceListMessage.cook(requestId, instances);
        } catch (Exception e) {
            return ErrorResultMessage.cook(IotConstants.RETURN_CODE_GETRULEALARMINSTANCESERROR,
                    IotMessage.get(IotConstants.RETURN_CODE_GETRULEALARMINSTANCESERROR, language), requestId);
        }
    }

    public IotBaseResultMessage checkName(Map<String, String> map, String tenantId, String requestId, String language) {
        try {
            doLog("check rule name", requestId);
            String ruleId = map.get(IotConstants.RULE_FIELD_RULEID);
            String ruleName = map.get(IotConstants.RULE_FIELD_RULENAME);
            int ruleNameCount = iotOfflineJobMapper.checkRuleName(ruleId, ruleName, tenantId);
            if (0 == ruleNameCount) {
                return CommonResultMessage.cook(requestId, IotConstants.RETURN_COMMON_SUCCESS);
            } else {
                return CommonResultMessage.cook(requestId, IotConstants.RETURN_COMMON_FAIL);
            }
        } catch (Exception e) {
            LOG.error(" check name error ,requestId=" + requestId, e);
            return ErrorResultMessage.cook(IotConstants.RETURN_CODE_OPERATORERROR,
                    IotMessage.get(IotConstants.RETURN_CODE_OPERATORERROR, language), requestId);
        }
    }

    public IotBaseResultMessage checkSql(Map<String, String> map, String tenantId, String requestId, String language) {
        try {
            doLog("check sql", requestId);
            String sql = map.get(IotConstants.RULE_FIELD_SQL);
            String ruleId = map.get(IotConstants.RULE_FIELD_RULEID);
            OfflineJob offlineJob = iotOfflineJobMapper.getDetail(ruleId, tenantId);
            if (null == offlineJob) {
                return ErrorResultMessage.cook(IotConstants.RETURN_CODE_RULENOTFOUNDERROR,
                        IotMessage.get(IotConstants.RETURN_CODE_JSONERROR, language), requestId);
            }
            ResultMessage resultMessage = checkSlq(offlineJob, sql);
            if (!resultMessage.isSuccess()) {
                return CommonResultMessage.cook(requestId, IotConstants.RETURN_COMMON_FAIL, resultMessage.getMsg());
            }
            return CommonResultMessage.cook(requestId, IotConstants.RETURN_COMMON_SUCCESS, resultMessage.getMsg());
        } catch (Exception e) {
            LOG.error(" check sql error ,requestId=" + requestId, e);
            return ErrorResultMessage.cook(IotConstants.RETURN_CODE_OPERATORERROR,
                    IotMessage.get(IotConstants.RETURN_CODE_OPERATORERROR, language), requestId);
        }
    }

    private ResultMessage checkSlq(OfflineJob offlineJob, String sql) {

        String mqttTopic = JSONObject.parseObject(offlineJob.getSourceJson()).getString(IotConstants.KAFKA_FIELD_MQTTTOPIC);
        String realSql = replaceSql(sql, mqttTopic);

        String fullSql = offlineJob.getPrepareSql() + realSql;
        return jobService09.syntaxCheck(offlineJob.getProduct(),
                offlineJob.getUserId(), offlineJob.getId(), fullSql, offlineJob.getServerId(), offlineJob.getClusterId());
    }

    private void doLog(String type, String requestId) {
        LOG.info(" do {} for requestId {}", type, requestId);
    }
}