package com.netease.iot.rule.proxy.akka;

public interface Submit {

    /**
     * Submit message.
     */
    Object submit(Object message, int timeout) throws Exception;
}
