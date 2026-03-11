package com.jtrack.kafka;

import akka.actor.AbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.function.Function;
import akka.kafka.ProducerSettings;
import akka.kafka.javadsl.Producer;
import akka.stream.ActorMaterializer;
import akka.stream.ActorMaterializerSettings;
import akka.stream.Materializer;
import akka.stream.Supervision;
import akka.stream.javadsl.Source;
import com.jtrack.pojo.J_Message;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import scala.Option;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.jtrack.util.RedisUtil;

public class KafkaJ_Producer extends AbstractActor {

    KafkaProducer producer;
    ProducerSettings<String, String> producerSettings;
    LoggingAdapter log = Logging.getLogger(context().system(), this);
    Config jtrackConfig;
    Materializer mat;
    String produce_topic;

    /*There are three ways to handle exceptions from application code:

    Stop - The stream is completed with failure.
    Resume - The element is dropped and the stream continues.
    Restart - The element is dropped and the stream continues after restarting the operator.
              Restarting an operator means that any accumulated state is cleared.
              This is typically performed by creating a new instance of the operator.
*/
    Function<Throwable, Supervision.Directive> decider = exec -> {
        if(exec instanceof Exception){
            log.info("KafkaJtrack_Producer , Supervision Strategy of Exception: " +exec.getMessage());
            log.error("KafkaJtrack_Producer , Supervision Strategy of Error: " +exec.toString());
            return Supervision.restart();
        }
        else {
            log.info("KafkaJtrack_Producer , Supervision other than Exception: " +exec.getMessage());
            log.error("KafkaJtrack_Producer , Supervision other than Exception: " +exec.toString());
            return Supervision.stop();
        }
    };


    KafkaJ_Producer(){
        jtrackConfig = ConfigFactory.load();
        producerSettings = ProducerSettings
                .create(context().system() , new StringSerializer() , new StringSerializer())
                .withProperty("acks", "all")
                .withProperty("retries", "2")
                .withBootstrapServers(jtrackConfig.getString("jtrack.kafka.consumer.producer_jtrack.server"));
        producer =  producerSettings.createKafkaProducer();
        produce_topic = jtrackConfig.getString("jtrack.kafka.consumer.producer_jtrack.topic");
        ActorMaterializerSettings sett = ActorMaterializerSettings.create(context().system()).withSupervisionStrategy(decider);
        mat = ActorMaterializer.create(sett, context().system());
    }

    @Override
    public void preStart() throws Exception {
        log.info("Kafka_Producer preStart");
        super.preStart();
    }

    @Override
    public void postStop() throws Exception {
        log.info("Kafka_Producer : Kafka Producer stopped");
        super.postStop();
    }

    @Override
    public void preRestart(Throwable reason, Option<Object> message) throws Exception {
        log.info(" PreRestart of the Kafka_Producer for " +reason.getMessage());
        self().tell(message, getSelf());
        super.preRestart(reason, message);
    }

    @Override
    public Receive createReceive() {

        return receiveBuilder()
                .match(J_Message.class, msg ->{
                    log.info("Received Message for Closed");
                    String ld = new Gson().toJson(msg);

                    // Publish to Kafka
                    Source.range(1, 1)
                            .map(number ->"")
                            .map(value -> new ProducerRecord<String, String>(produce_topic, ld))
                            .log("Producing to Kafka " , log)
                            .runWith(Producer.plainSink(producerSettings, producer), mat);
                    
                    // Also publish to Redis as backup/additional channel
                    RedisUtil.publishCloseEvent(msg, log);
                    RedisUtil.setGateStatus(msg.getGateId(), msg.getClose(), log);
                    
                    log.info("Producing to topic and Redis");
                })
                .matchAny(r -> {
                    log.info("KafkaProducerActor : MatchAny: " +r);

                }).build();

    }
}
