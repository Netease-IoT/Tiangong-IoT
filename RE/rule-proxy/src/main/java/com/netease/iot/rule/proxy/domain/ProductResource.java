package com.netease.iot.rule.proxy.domain;

public class ProductResource {

    private Long productId;
    private String productName;
    private Long totalSlot;
    private Long totalMem;
    private Long usedSlot;
    private Long usedMem;

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Long getTotalSlot() {
        return totalSlot;
    }

    public void setTotalSlot(Long totalSlot) {
        this.totalSlot = totalSlot;
    }

    public Long getTotalMem() {
        return totalMem;
    }

    public void setTotalMem(Long totalMem) {
        this.totalMem = totalMem;
    }

    public Long getUsedSlot() {
        return usedSlot;
    }

    public void setUsedSlot(Long usedSlot) {
        this.usedSlot = usedSlot;
    }

    public Long getUsedMem() {
        return usedMem;
    }

    public void setUsedMem(Long usedMem) {
        this.usedMem = usedMem;
    }
}
