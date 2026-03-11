package com.jtrack.cluster;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.cluster.sharding.ClusterSharding;
import akka.cluster.sharding.ClusterShardingSettings;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.jtrack.receiver.New_J_Entity;
import com.jtrack.cluster.J_Extractor;

public class J_InitCluster extends AbstractActor {

    LoggingAdapter log = Logging.getLogger(context().system(), this);

    J_InitCluster(){
        log.info("J_InitCluster : Cluster Initialization Started");

        ClusterShardingSettings settings =  ClusterShardingSettings.create(context().system());
        ActorRef fin_l_r_clusterRef = akka.cluster.sharding.ClusterSharding$.MODULE$.get(context().system()).start("J_ClusterInit", Props.create(New_J_Entity.class), settings, new J_Extractor());
    }
    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .matchAny(any -> {

                })
                .build();
    }
}
