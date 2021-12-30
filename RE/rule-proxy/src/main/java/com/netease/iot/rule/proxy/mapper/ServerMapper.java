package com.netease.iot.rule.proxy.mapper;

import com.netease.iot.rule.proxy.domain.Server;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ServerMapper {

    List<Server> getServer(@Param("serverId") Integer serverId);

    Integer checkServer(@Param("version") String version);

    void saveServer(@Param("serverName") String serverName,
                    @Param("akkaPath") String akkaPath,
                    @Param("version") String version,
                    @Param("versionStatus") Integer versionStatus);
}
