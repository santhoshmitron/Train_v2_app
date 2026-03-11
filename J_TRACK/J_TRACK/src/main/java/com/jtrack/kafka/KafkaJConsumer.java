package com.jtrack.kafka;

import akka.Done;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.kafka.ConsumerMessage;
import akka.kafka.ConsumerSettings;
import akka.kafka.Subscriptions;
import akka.kafka.javadsl.Consumer;
import akka.stream.ClosedShape;
import akka.stream.Graph;
import akka.stream.SourceShape;
import akka.stream.Supervision;
import akka.stream.javadsl.GraphDSL;
import akka.stream.javadsl.RunnableGraph;
import akka.stream.javadsl.Sink;
import com.github.levkhomich.akka.tracing.TracingExtension;
import com.github.levkhomich.akka.tracing.TracingExtensionImpl;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jtrack.pojo.Gate;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.squbs.actorregistry.japi.ActorLookup;
import org.squbs.stream.AbstractPerpetualStream;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import akka.cluster.sharding.ClusterSharding;
import akka.actor.ActorRef;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class KafkaJConsumer extends AbstractPerpetualStream<CompletionStage<Done>> implements ConsumerRebalanceListener {

    //Sink<String, CompletionStage<Done>> consoleSink = Sink.<String>foreach(System.out::println);
    Sink<Done, CompletionStage<Done>> consoleSink = Sink.<Done>foreach((a)->{
    });
    final ActorLookup<?> lookup = ActorLookup.create(context());
    protected ConsumerSettings<String, String> consumerSettings;
    LoggingAdapter log = Logging.getLogger(context().system(), this);
    Config jtrackConfig;
    String kafkaTopic;
    String offsetRest;
    String clientId;
    TracingExtensionImpl trace;
    int throttleLimit;
    int throttleTimeInMinutes;

    final Function<Throwable, Supervision.Directive> decider =
            exc -> {
                if (exc instanceof Exception)
                    return Supervision.stop();
                else
                    return Supervision.stop();
            };


    public void onPartitionsRevoked(Collection<TopicPartition> partitions){
        log.info("Partition Revoked in KAFKA Jtrack ");
        for(TopicPartition partition : partitions){
            log.info("Topic is : " +partition.topic() +" Partition: " +partition.partition() +" Revoked");
        }

    }

    public void onPartitionsAssigned(Collection<TopicPartition> partitions){
        log.info("Partition Assigned in KAFKA Jtrack ");
        for(TopicPartition partition : partitions){
            log.info("Topic is : " +partition.topic() +" Partition: " +partition.partition() +" Assigned");
        }
    }

    public KafkaJConsumer(){
        jtrackConfig = ConfigFactory.load();
        kafkaTopic = jtrackConfig.getString("jtrack.kafka.consumer.jtrack.topic");
        offsetRest = jtrackConfig.getString("jtrack.kafka.consumer.jtrack.offset_reset");
        clientId = jtrackConfig.getString("jtrack.kafka.consumer.jtrack.clientId");
        throttleLimit = jtrackConfig.getInt("jtrack.kafka.consumer.jtrack.throttleLimit");
        throttleTimeInMinutes = jtrackConfig.getInt("jtrack.kafka.consumer.jtrack.throttleTimeInMinutes");

        trace = (TracingExtensionImpl) TracingExtension.apply(context().system());

        consumerSettings = ConsumerSettings.create(materializer().system(), new StringDeserializer(), new StringDeserializer())
                .withClientId(clientId)
                .withBootstrapServers(jtrackConfig.getString("jtrack.kafka.consumer.jtrack.server"))
                .withGroupId(jtrackConfig.getString("jtrack.kafka.consumer.jtrack.consumerGroup"))
                .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, offsetRest);
    }



    @Override
    public RunnableGraph<CompletionStage<Done>> streamGraph() {
        log.info("Kafka_Jtrack_consumer Actor Started: " + context().self().path());
        String kafkaTopic = jtrackConfig.getString("jtrack.kafka.consumer.jtrack.topic");

        final Graph<ClosedShape, CompletionStage<Done>> graph = GraphDSL.create(consoleSink, (b, sink) -> {
            SourceShape<Done> source = consumeAsyncCommitWithDuration(b , kafkaTopic);
            b.from(source).to(sink);
            return ClosedShape.getInstance();
        });

        return RunnableGraph.fromGraph(graph);
    }


    public SourceShape<Done> consumeAsyncCommitWithDuration(GraphDSL.Builder<CompletionStage<Done>> b, String topic){
        SourceShape<Done> source = b.add(Consumer.committableSource(consumerSettings, Subscriptions.topics(topic)).
                throttle(throttleLimit , Duration.ofMinutes(throttleTimeInMinutes))
                .mapAsync(20, msg -> processDataToActorClusterProducer(msg.record(), topic)
                        .thenApply(done -> msg.committableOffset()))
                .groupedWithin(1000 , scala.concurrent.duration.Duration.apply(10 , TimeUnit.SECONDS))
                .map(group -> foldLeft(group))
                .mapAsync(3, c ->{
                    c.commitJavadsl();
                    log.info("JTrack:Offsets Commited: " +c.offsets() +": topic: " +topic);
                    return CompletableFuture.completedFuture(Done.getInstance());
                }));
        return source;
    }

    private ConsumerMessage.CommittableOffsetBatch foldLeft(List<ConsumerMessage.CommittableOffset> group) {
        ConsumerMessage.CommittableOffsetBatch batch = ConsumerMessage.emptyCommittableOffsetBatch();
        log.info("JTrack : Commiting offset " +batch.offsets());
        for (ConsumerMessage.CommittableOffset elem: group) {
            batch = batch.updated(elem);
        }
        return batch;
    }


    public CompletionStage<Done> processDataToActorClusterProducer(ConsumerRecord<String , String> commitableMessage, String topic){
        String traceKey = "jtrack_consumer";
        String uniquenessKey = "";
        String txnNumber = "";
        JsonObject jobj = null;
        try{
            JsonParser parser = new JsonParser();
            jobj = parser.parse(commitableMessage.value()).getAsJsonObject();
        }catch(Exception e){
           log.error(e , "Jtrack: Error while parsing Kafka message.");
           return CompletableFuture.completedFuture(Done.getInstance());
        }

        if (jobj == null) {
            log.error("Jtrack: Parsed JSON object is null. Cannot process message.");
            return CompletableFuture.completedFuture(Done.getInstance());
        }

        // Extract required fields with null checks
        String gatename = getStringSafely(jobj, "gateNum", null);
        String gateId = getStringSafely(jobj, "boom1Id", null);
        String l_value = getStringSafely(jobj, "l_value", null);
        String g_value = getStringSafely(jobj, "g_value", null);
        String batch = getStringSafely(jobj, "batch", "");
        
        // Validate required fields
        if (gatename == null || gateId == null || l_value == null || g_value == null) {
            log.error("Jtrack: Missing required fields in Kafka message. gateNum: {}, boom1Id: {}, l_value: {}, g_value: {}", 
                gatename, gateId, l_value, g_value);
            return CompletableFuture.completedFuture(Done.getInstance());
        }
        
        log.info("Kafka_Jtrack_consumer1 : Message received for gate : " + gatename);

        // Extract BS2 and LT values if present (optional for backward compatibility)
        String bs2_value = null;
        String lt_value = null;
        if (jobj.has("bs2_value") && !jobj.get("bs2_value").isJsonNull()) {
            bs2_value = getStringSafely(jobj, "bs2_value", null);
        }
        if (jobj.has("lt_value") && !jobj.get("lt_value").isJsonNull()) {
            lt_value = getStringSafely(jobj, "lt_value", null);
        }
        
        Gate m;
        if (bs2_value != null || lt_value != null) {
            m = new Gate(gatename, gateId, g_value, l_value, 
                bs2_value != null ? bs2_value : "", 
                lt_value != null ? lt_value : "");
        } else {
            m = new Gate(gatename, gateId, g_value, l_value);
        }
        m.setBatch(batch != null ? batch : "");

        String currentTime = getCurrentTime();

        log.info("Kafka_Jtrack_consumer : Message received for gate : " + gatename +" and boom1Id: " +gateId +" at Time: "+currentTime);

        ActorRef clusterEntity = akka.cluster.sharding.ClusterSharding$.MODULE$.get(context().system()).shardRegion("J_ClusterInit");
            clusterEntity.tell(m, getSelf());
        return CompletableFuture.completedFuture(Done.getInstance());
    }
    
    // Helper method to safely get string from JSON, handling JsonNull
    private String getStringSafely(JsonObject obj, String key, String defaultValue) {
        if (!obj.has(key)) {
            return defaultValue;
        }
        JsonElement element = obj.get(key);
        if (element == null || element.isJsonNull()) {
            return defaultValue;
        }
        try {
            return element.getAsString();
        } catch (Exception e) {
            log.warning("Error getting string value for key {}: {}", key, e.getMessage());
            return defaultValue;
        }
    }

    @Override
    public CompletionStage<Done> shutdown() {

        // Do all your cleanup
        // For safety, call super
        log.debug("Inside shutdown of Kafka_Jtrack_consumer");
        return super.shutdown();
    }

    public static String getCurrentTime(){
        DateFormat targetFormat = new SimpleDateFormat("yyyyMMddHHmmssZ");
        Date cur_time = new Date();
        String formattedTime = targetFormat.format(cur_time);
        return formattedTime;
    }
}

