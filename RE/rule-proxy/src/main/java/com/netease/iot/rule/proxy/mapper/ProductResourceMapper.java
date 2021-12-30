package com.netease.iot.rule.proxy.mapper;

import com.netease.iot.rule.proxy.domain.ProductResource;
import org.apache.ibatis.annotations.Param;

public interface ProductResourceMapper {

    ProductResource getProductInfo(@Param("productName") String productName);

    void updateResource(@Param("productId") Long productId,
                        @Param("nowSlot") Long nowSlot,
                        @Param("nowMem")Long nowMem);
}
