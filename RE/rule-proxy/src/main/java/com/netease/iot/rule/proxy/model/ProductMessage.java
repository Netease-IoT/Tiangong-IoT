package com.netease.iot.rule.proxy.model;

import java.io.Serializable;
import java.util.UUID;

public class ProductMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    public String product;

    public final UUID uuid;

    public ProductMessage(String product) {
        this.product = product;
        uuid = UUID.randomUUID();
    }
}
