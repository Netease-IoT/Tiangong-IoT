package com.netease.iot.rule.proxy.mapper;

import com.netease.iot.rule.proxy.domain.DebugInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;


@Mapper
public interface DebugInfoMapper {

    /**
     * Get debug info
     *
     * @param jobId
     * @return
     */
    DebugInfo getDebugInfo(@Param("jobId") String jobId);

    /**
     * Update debug info
     *
     * @param jobId
     * @param sessionId
     * @param status
     */
    void updateDebugStatus(
            @Param("jobId") String jobId,
            @Param("sessionId") String sessionId,
            @Param("status") int status,
            @Param("userId") String userId,
            @Param("sqlString") String sqlString,
            @Param("rawSql") String rawSql,
            @Param("sourceType") String sourceType,
            @Param("sinkType") String sinkType);

    /**
     * Insert to debug message
     *
     * @param jobId
     * @param sessionId
     * @param status
     */
    void insertDebugInfo(
            @Param("jobId") String jobId,
            @Param("sessionId") String sessionId,
            @Param("status") int status,
            @Param("userId") String userId,
            @Param("sqlString") String sqlString,
            @Param("rawSql") String rawSql,
            @Param("sourceType") String sourceType,
            @Param("sinkType") String sinkType);
}
