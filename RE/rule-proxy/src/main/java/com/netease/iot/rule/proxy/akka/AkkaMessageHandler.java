package com.netease.iot.rule.proxy.akka;

import akka.actor.ActorRef;

public interface AkkaMessageHandler {

    /**
     * Handle message.
     */
    void handleMessage(Object message, ActorRef sender);
}
