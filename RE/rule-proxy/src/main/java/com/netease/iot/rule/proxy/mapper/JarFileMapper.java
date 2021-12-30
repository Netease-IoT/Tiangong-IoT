package com.netease.iot.rule.proxy.mapper;

import com.netease.iot.rule.proxy.domain.JarFile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface JarFileMapper {

    List<JarFile> listByProduct(@Param("product") String product);

    /**
     * @param product
     * @param jarId
     * @param fileName
     * @param uploadedPath
     * @return
     */
    boolean insertJar(@Param("product") String product,
                      @Param("jarId") String jarId,
                      @Param("fileName") String fileName,
                      @Param("filePath") String uploadedPath,
                      @Param("jarSize") long jarSize,
                      @Param("explanation") String explanation,
                      @Param("createTime") String createTime
    );

    JarFile getJar(@Param("jarId") String jarId);

    void removeJar(@Param("jarId") String jarId);
}
