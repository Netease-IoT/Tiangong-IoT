package com.netease.iot.rule.proxy.mapper;


import com.netease.iot.rule.proxy.domain.OfflineJob;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface IotOfflineJobMapper {

    int createJob(
            @Param("product") String product,
            @Param("jobId") String jobId,
            @Param("jobName") String jobName,
            @Param("createTime") String createTime,
            @Param("clusterId") Integer clusterId,
            @Param("serverId") Integer serverId,
            @Param("conf") String conf,
            @Param("prepareSql") String prepareSql,
            @Param("sourceType") String sourceType,
            @Param("sourceJson") String sourceJson,
            @Param("sinkType") String sinkType,
            @Param("sinkJson") String sinkJson,
            @Param("ruleDescription") String ruleDescription
    );

    int updateJob(
            @Param("product") String product,
            @Param("jobId") String jobId,
            @Param("ruleName") String jobName,
            @Param("ruleDescription") String jobDescription,
            @Param("editTime") String editTime,
            @Param("prepareSql") String prepareSql,
            @Param("sourceType") String sourceType,
            @Param("sourceJson") String sourceJson,
            @Param("sinkType") String sinkType,
            @Param("sinkJson") String sinkJson,
            @Param("ruleDescription") String ruleDescription
    );

    int updateSql(@Param("jobId") String jobId,
                  @Param("sql") String sql);

    int checkRuleName(@Param("jobId") String jobId,
                      @Param("jobName") String jobName,
                      @Param("product") String product);

    int checkJobId(@Param("jobId") String jobId);

    int updateOperatingStatus(
            @Param("jobId") String jobId,
            @Param("checkStatus") int checkStatus,
            @Param("updateStatus") int updateStatus
    );

    OfflineJob getDetail(@Param("jobId") String jobId,
                         @Param("product") String product);

    List<OfflineJob> searchJobByJobNameByPage(Map<String, Object> data);

    int searchJobByJobNameCount(Map<String, Object> data);

    int deleteJob(@Param("jobId") String jobId);
}
