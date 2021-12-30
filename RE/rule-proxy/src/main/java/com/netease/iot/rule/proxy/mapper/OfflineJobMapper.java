package com.netease.iot.rule.proxy.mapper;

import com.netease.iot.rule.proxy.domain.OfflineJob;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface OfflineJobMapper {

    List<OfflineJob> getJobList(@Param("product") String product);

    List<OfflineJob> getJobListByPage(
            Map<String, Object> data);

    Integer getJobListCount(Map<String, Object> data);

    List<OfflineJob> searchJobByJobNameByPage(Map<String, Object> data);

    Integer searchJobByJobNameCount(Map<String, Object> data);

    OfflineJob getJobDetail(@Param("jobId") String jobId);

    int createJob(
            @Param("product") String product,
            @Param("creator") String userId,
            @Param("jobId") String jobId,
            @Param("fileName") String fileName,
            @Param("sql") String sql,
            @Param("createTime") String createTime,
            @Param("clusterId") Integer clusterId,
            @Param("serverId") Integer serverId,
            @Param("versionStatus") Integer versionStatus);

    int saveJob(
            @Param("editor") String userId,
            @Param("sql") String sql,
            @Param("jobId") String jobId,
            @Param("fileName") String jobName,
            @Param("editTime") String editTime,
            @Param("clusterId") Integer clusterId,
            @Param("serverId") Integer serverId,
            @Param("versionStatus") Integer versionStatus);

    int saveOldJob(
            @Param("editor") String userId,
            @Param("sql") String sql,
            @Param("jobId") String jobId,
            @Param("fileName") String jobName,
            @Param("editTime") String editTime);

    boolean setJobConf(
            @Param("editor") String userId,
            @Param("jobId") String jobId,
            @Param("conf") String conf,
            @Param("editTime") String editTime);

    boolean setJobClusterId(
            @Param("editor") String userId,
            @Param("jobId") String jobId,
            @Param("clusterId") Integer clusterId,
            @Param("serverId") Integer serverId,
            @Param("editTime") String editTime);

    boolean updateVersion(@Param("jobId") String jobId);

    boolean deleteJob(@Param("jobId") String jobId);

    List<OfflineJob> batchGetJob(@Param("jobList") List<String> jobList,
                                 @Param("product") String product);

    boolean saveSavePointToOffLine(@Param("jobId") String jobId,
                                   @Param("savePointPath") String savePointPath);

    boolean cleanConf(@Param("jobId") String jobId);

    int updateOperatingStatus(
            @Param("jobId") String jobId,
            @Param("checkStatus") int checkStatus,
            @Param("updateStatus") int updateStatus
    );
}
