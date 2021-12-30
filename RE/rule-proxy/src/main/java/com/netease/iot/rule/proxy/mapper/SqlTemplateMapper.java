package com.netease.iot.rule.proxy.mapper;

import com.netease.iot.rule.proxy.domain.SqlTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SqlTemplateMapper {
    List<SqlTemplate> getSqlTemplateByParentId(@Param("version") String version, @Param("parentId") int id);
}
