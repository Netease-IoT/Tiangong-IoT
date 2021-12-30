package com.netease.iot.rule.proxy.mapper;

import com.netease.iot.rule.proxy.domain.JobParameter;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface JobParameterMapper {
    List<JobParameter> getJobConfParameterList(String userId);
}
