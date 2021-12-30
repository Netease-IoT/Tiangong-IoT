package com.netease.iot.rule.proxy;

import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.pattern.Patterns;
import akka.util.Timeout;

import com.netease.iot.rule.proxy.akka.ActorSystemFactory;
import com.netease.iot.rule.proxy.akka.Submit;
import com.netease.iot.rule.proxy.domain.Cluster;
import com.netease.iot.rule.proxy.model.BaseMessage;
import com.netease.iot.rule.proxy.model.JobResumeMessage;
import scala.concurrent.Future;

import java.util.concurrent.TimeUnit;


public class StreamServerClient implements Submit {

    private String targetStreamServer;

    private ActorSystem actorSystem;

    private ActorSelection actor;

    private Cluster cluster;

    public StreamServerClient(String client, Cluster cluster) {
        this.cluster = cluster;
        this.targetStreamServer = cluster.getProduct();
        actorSystem = ActorSystemFactory.getSendActorSystem(client);
        actor = actorSystem.actorSelection(cluster.getAkkaPath());
    }

    /**
     * Submit message to server.
     */
    public Future<Object> submit(Object message, int timeout) {
        if (BaseMessage.class.isAssignableFrom(message.getClass())) {
            BaseMessage msg = (BaseMessage) message;
            if (JobResumeMessage.class.isAssignableFrom(message.getClass())) {
                msg = ((JobResumeMessage) message).jobCreateMessage;
            }
            if (cluster != null) {
                msg.setClusterUrl(cluster.getClusterUrl());
                msg.setClusterPort(cluster.getClusterPort());
                msg.setClusterWebUi(cluster.getClusterWebUi());
                msg.setHA(cluster.isHA());
                msg.setAppId(cluster.getAppId());
            }
            return Patterns.ask(actor, message, new Timeout(timeout, TimeUnit.SECONDS));
        } else {
            return Patterns.ask(actor, message, new Timeout(timeout, TimeUnit.SECONDS));
        }
    }

    public Cluster getCluster() {
        return cluster;
    }
}
