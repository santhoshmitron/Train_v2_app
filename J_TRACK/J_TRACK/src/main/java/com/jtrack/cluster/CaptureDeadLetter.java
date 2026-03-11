package com.jtrack.cluster;
import akka.actor.AbstractActor;
import akka.actor.DeadLetter;
import akka.event.Logging;
import akka.event.LoggingAdapter;
//import jio.rtrs.pigeon.pojo.KafkaProducerMessage;
import org.squbs.actorregistry.japi.ActorLookup;
public class CaptureDeadLetter extends AbstractActor {

    final ActorLookup<?> lookup = ActorLookup.create(context());
    LoggingAdapter log = Logging.getLogger(context().system(), this);

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(DeadLetter.class, deadLetter ->{
                   // KafkaProducerMessage pMsg = new KafkaProducerMessage(deadLetter.toString() , "FINLR_DEADLETTER" , "topic99");
                    //lookup.tell(pMsg, getSelf());
                })
                .build();
    }
}

