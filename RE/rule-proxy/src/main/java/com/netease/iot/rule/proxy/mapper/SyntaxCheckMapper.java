package com.netease.iot.rule.proxy.mapper;

import com.netease.iot.rule.proxy.domain.SyntaxCheck;
import org.apache.ibatis.annotations.Param;

public interface SyntaxCheckMapper {

    SyntaxCheck getSyntaxCheckMessage(
            @Param("jobId") String jobId,
            @Param("userId") String userId);

    void insertSyntaxMessage(
            @Param("jobId") String jobId,
            @Param("userId") String userId,
            @Param("rawSql") String rawSql,
            @Param("tableMessage") String tableMessage);

    void updateSyntaxMessage(
            @Param("jobId") String jobId,
            @Param("userId") String userId,
            @Param("rawSql") String rawSql,
            @Param("tableMessage") String tableMessage);
}
