package com.netease.iot.rule.proxy.akka;

import akka.actor.UntypedActor;

public class Actor extends UntypedActor {

    private AkkaMessageHandler akkaMessageHandler;

    public Actor() {
    }

    public Actor(AkkaMessageHandler akkaMessageHandler) {
        this.akkaMessageHandler = akkaMessageHandler;
    }

    @Override
    public void onReceive(Object message) {
        akkaMessageHandler.handleMessage(message, getSender());
    }
}
