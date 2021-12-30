package com.netease.iot.rule.proxy.mapper;

import com.netease.iot.rule.proxy.domain.JarRefer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface JarReferMapper {
    void addReference(
            @Param("jobId") String jobId,
            @Param("jarId") String jarId,
            @Param("type") int type);

    void delReference(
            @Param("jobId") String jobId,
            @Param("jarId") String jarId,
            @Param("type") int type);

    List<JarRefer> getJobIdList(
            @Param("jarId") String jarId,
            @Param("type") int type);

    List<JarRefer> getJobJarList(
            @Param("jobId") String jobId,
            @Param("type") int type);
}
